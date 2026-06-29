
# StressBridge: Sensor Data Pipeline

StressBridge is a local-first sensor data collection pipeline. It provides a modular framework to capture real-time physiological and motion data from an Android device and securely store it in a local CSV-based database on a development machine.

## 1. Pipeline Architecture

* **Step 1: Data Acquisition** 
    - The Android application collects sensor data (PPG, GSR) from the HealthyPi watch but due to firmware limitations, it collects motion data from your mobile phone device.
* **Step 2: Transmission** 
    - Captured data is serialized into JSON and transmitted via `HTTP POST` requests using the Ktor client.
* **Step 3: Ingestion** 
    - A local Python-based backend (FastAPI/Flask) listens for incoming packets on a designated port.
* **Step 4: Local Storage** 
    - The server writes incoming records directly to a local, persistent `.csv` file.

## 2. Functionality Status

### Working Functionalities

* **Local Data Ingestion:** Real-time transmission of JSON payloads from the Android client to the local server.
* **Persistent CSV Logging:** Reliable, append-only file writing implemented on the local machine using absolute path targeting.
* **Asynchronous Processing:** Non-blocking I/O operations on the server ensure that high-frequency data ingestion does not interrupt local system processes.

### Silent Functionalities (Configurable)

* **Cloudflare Tunneling:** While the codebase contains the structure for tunnel connectivity, it is currently **disabled** to optimize for a local-only development cycle.
* **Remote Database Synchronization:** The pipeline is architected for a future transition to cloud-based storage (e.g., PostgreSQL or MongoDB) once the local data cleaning protocol is verified.

## 3. Extending the Pipeline (Modular Design)

The system is built to be easily modifiable. To scale the data collection to include new parameters (e.g., SpO2 or Skin Temperature), follow these steps:

### A. Android Client Updates

1. **Model Definition:** Update your data class (the Kotlin object) to include the new field.
2. **Sensor Callback:** Inside your sensor manager, map the new sensor reading to the key corresponding to your new field.

### B. Server Backend Updates

1. **Write Logic:** In your `ReceiverApp.py`, update the `writer.writerow()` call to include the new data point fetched from the request object.
2. **Schema Definition:** Update the file initialization section (where the header is written) to add the new column name to the CSV header.

### C. Pipeline Maintenance

* If the sensor frequency increases, adjust the `f.flush()` logic in the backend to ensure data is periodically saved to disk without creating an I/O bottleneck.

---

## 4. Quick Start

1. **Start the Server:** Ensure your Python environment is set up and run:
```bash
python SimpleReceiver.py

```


2. **Configure Client:** Update your `ServerUploader` base URL to point to your development laptop's local IPv4 address (e.g., `http://192.168.x.x:5000/data`).
3. **Verify:** Check `~/sensor_data.csv` on your server machine to confirm incoming records.

