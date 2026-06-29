package com.aryan.stressbridge

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.UUID

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class JetsonBluetoothManager {

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    suspend fun connectToJetson(deviceAddress: String, adapter: BluetoothAdapter) = withContext(Dispatchers.IO) {
        Log.i("JETSON_LOG", "Attempting connection to: $deviceAddress")
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            if (bluetoothSocket?.isConnected == true) return@withContext

            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            Log.i("JETSON_LOG", "Socket created, connecting...")
            bluetoothSocket?.connect()
            Log.i("JETSON_LOG", "Connected to Jetson via RFCOMM")
        } catch (e: Exception) {
            Log.e("JETSON_LOG", "CRITICAL CONNECTION ERROR: ${e.stackTraceToString()}")
        }
    }

    fun sendData(data: String) {
        // if the socket is null then Mock Mode
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            Log.d("MOCK_JETSON_OUTPUT", "JSON Data: $data")
            return
        }

        try {
            bluetoothSocket?.outputStream?.write(data.toByteArray())
            bluetoothSocket?.outputStream?.flush()
        } catch (e: IOException) {
            Log.e("JETSON_LOG", "Error sending data: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothSocket?.close()
    }
}