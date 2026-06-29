package com.aryan.stressbridge

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

class BLEManager(private val context: Context, private val motionManager: MotionSensorManager) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    private val targetMacAddress = "D7:BA:F4:1E:EE:9E"
    private var bluetoothGatt: BluetoothGatt? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // UUIDs
    private val UUID_GSR = UUID.fromString("babe4a4c-7789-11ed-a1eb-0242ac120002")
    private val UUID_PPG_WRIST = UUID.fromString("cd5c1525-4448-7db8-ae4c-d1da8cba36d0")
    private val UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // BLE operation Q to prevent Android from dropping requests
    private val descriptorWriteQueue: Queue<BluetoothGattDescriptor> = LinkedList()

    // UI States
    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _gsrValue = MutableStateFlow(0.0f)
    val gsrValue: StateFlow<Float> = _gsrValue.asStateFlow()

    // Hold the latest PPG value
    private val _ppgValue = MutableStateFlow(0)
    val ppgValue: StateFlow<Int> = _ppgValue.asStateFlow()

    // Heart Rate Logic
    private val heartRateProcessor = HeartRateProcessor()
    private val _bpmValue = MutableStateFlow(0.0f)
    val bpmValue: StateFlow<Float> = _bpmValue.asStateFlow()

    // Data Aggregator Logic
    val aggregator = DataAggregator()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (device.address == targetMacAddress) {
                    stopScan()
                    connectToDevice(device)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bleScanner == null) return
        Log.i("APP_LOG", "Starting BLE scan...")
        _connectionState.value = "Scanning..."
        bleScanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = "Connecting..."
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    // process next item in operation queue
    @SuppressLint("MissingPermission")
    private fun processNextDescriptor() {
        if (descriptorWriteQueue.isNotEmpty()) {
            val descriptor = descriptorWriteQueue.element() // ceheck top item
            bluetoothGatt?.writeDescriptor(descriptor)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = "Connected. Discovering..."
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = "Disconnected"
                _gsrValue.value = 0.0f
                _ppgValue.value = 0
                descriptorWriteQueue.clear() // clear q on disconnect
                gatt.close()
                bluetoothGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = "Ready (Services Found)"

                for (service in gatt.services) {
                    // queue GSR subscription
                    val gsrChar = service.getCharacteristic(UUID_GSR)
                    if (gsrChar != null) {
                        gatt.setCharacteristicNotification(gsrChar, true)
                        val descriptor = gsrChar.getDescriptor(UUID_CCCD)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            descriptorWriteQueue.add(descriptor)
                        }
                    }

                    // queue PPG subscription
                    val ppgChar = service.getCharacteristic(UUID_PPG_WRIST)
                    if (ppgChar != null) {
                        gatt.setCharacteristicNotification(ppgChar, true)
                        val descriptor = ppgChar.getDescriptor(UUID_CCCD)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            descriptorWriteQueue.add(descriptor)
                        }
                    }
                }

                processNextDescriptor()
            }
        }

        // wait for descriptor write success
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("APP_LOG", "Successfully subscribed to characteristic.")
                descriptorWriteQueue.poll() // remove the successful item
                processNextDescriptor() // fire the next one
            } else {
                Log.e("APP_LOG", "Failed to write descriptor, status: $status")
            }
        }


        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value ?: return
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            if (characteristic.uuid == UUID_GSR) {
                val samples = mutableListOf<Float>()
                while (buffer.remaining() >= 4) {
                    samples.add(buffer.int / 100.0f)
                }
                if (samples.isNotEmpty()) {
                    _gsrValue.value = samples.last()
                    Log.d("APP_LOG", "GSR Packet: $samples")

                    // send to aggregator
                    serviceScope.launch {
                        aggregator.addData(
                            _gsrValue.value,
                            motionManager.motionMagnitude.value,
                            _bpmValue.value
                        )
                    }
                }
            }
            else if (characteristic.uuid == UUID_PPG_WRIST) {
                while (buffer.remaining() >= 4) {
                    val sample = buffer.int

                    // send to aggregator
                    serviceScope.launch {
                        aggregator.addData(
                            _gsrValue.value,
                            motionManager.motionMagnitude.value,
                            sample.toFloat() // Passing raw PPG
                        )
                    }

                    _ppgValue.value = sample
                }
            }
        }
    }
}
