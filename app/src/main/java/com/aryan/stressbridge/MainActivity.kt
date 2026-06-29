package com.aryan.stressbridge

import com.aryan.stressbridge.JetsonBluetoothManager
import com.aryan.stressbridge.ServerUploader

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Instantiate our Managers
    private lateinit var bleManager: BLEManager
    private lateinit var motionManager: MotionSensorManager
    private lateinit var jetsonManager: JetsonBluetoothManager
    private lateinit var serverUploader: ServerUploader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        motionManager = MotionSensorManager(this)
        bleManager = BLEManager(this, motionManager)
        jetsonManager = JetsonBluetoothManager()
        serverUploader = ServerUploader("22CS30055@10.5.18.100", "Stiti#180804")

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppUI(bleManager = bleManager, motionManager = motionManager, serverUploader=serverUploader)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        motionManager.startListening() // 3. Start
    }

    override fun onPause() {
        super.onPause()
        motionManager.stopListening() // 4. Stop
    }
}

@Composable
fun AppUI(bleManager: BLEManager, motionManager: MotionSensorManager, serverUploader: ServerUploader) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(requiredPermissions)
    }

    val connectionState by bleManager.connectionState.collectAsState()
    val gsrValue by bleManager.gsrValue.collectAsState()
    val ppgValue by bleManager.ppgValue.collectAsState()

    // --- NEW: Observe Calculated BPM ---
    val bpmValue by bleManager.bpmValue.collectAsState()

    // --- NEW: Motion Sensor ---
    val motionMagnitude by motionManager.motionMagnitude.collectAsState()

    // --- NEW: Scope For BT Adapter ---
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "HealthyPi Watch Status:")
        Text(text = connectionState, style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        // --- NEW: Display BPM ---
        Text(text = "Calculated Heart Rate:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = String.format("%.1f BPM", bpmValue),
            style = MaterialTheme.typography.displayLarge, // Make it big!
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Current GSR:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = String.format("%.2f uS", gsrValue),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Current PPG (Raw):", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "$ppgValue",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Current Motion:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = String.format("%.2f", motionMagnitude),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(32.dp))

        /*Button(onClick = {
            scope.launch{
                jetsonManager.connectToJetson("38:8D:3D:24:F6:D5", BluetoothAdapter.getDefaultAdapter())
            }
        }) {
            Text("Connect to Jetson (Laptop)")
        }

        Spacer(modifier = Modifier.height(32.dp))*/


        Button(onClick = { bleManager.startScan() }) {
            Text(text = "Connect to Watch")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { bleManager.disconnect() }) {
            Text(text = "Disconnect")
        }
    }

    LaunchedEffect(key1 = Unit) {
        bleManager.aggregator.recordFlow.collect { record ->
            val json = """{"timestamp":${record.timestamp},"raw_ppg":${record.rawPpg},"motion":${record.motion},"gsr":${record.gsr}}"""

            // This now sends the data to your college server
            serverUploader.uploadRecord(json)
        }
    }
}