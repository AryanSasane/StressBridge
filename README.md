
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
4. **Receiver Code:** Stored in the HealthyPi Receiver file.


---

## 5. Local Data Collection Guide

Follow these steps to establish the pipeline between your Android device and your local machine.

Here is the updated **Local Data Collection Guide** for your `README.md`, now tailored for your simpler **Flask** implementation.

---

## 5. Local Data Collection Guide

Follow these steps to establish the pipeline between your Android device and your local machine using the Flask receiver.

### A. Server Setup (The Receiver)

1. **Navigate to the Receiver Folder:** Open your terminal in the directory where your `ReceiverApp.py` is located.
2. **Install Dependencies:** Ensure you have the `flask` library installed:
```bash
pip install flask

```


3. **Launch the Server:** Run the Python script directly. This will start the Flask development server, which handles incoming HTTP requests on port 5000:
```bash
python ReceiverApp.py

```


*The console will indicate the server is running on `http://0.0.0.0:5000`. Your laptop's local IP address (e.g., `192.168.1.5`) is the address your Android device will use to communicate.*


### B. Client Configuration (The Android App)

1. **Upload the Code:** Upload the entire code onto your phone using Android Studio, and use the controls given there to connect with the smartwatch(enter the security code that appears on the smartwatch during bluetooth pairing) and your laptop.
1. **Find your Laptop IP:** On your laptop, open your command prompt/terminal and run `ipconfig` (Windows) or `ifconfig` (Linux/macOS) to find your Wi-Fi IPv4 address, it will change everytime you change WiFi routers.
2. **Update the Client:** In your Android project, locate `ServerUploader.kt`. Update the `client.post` URL to point to your Host IP:
```kotlin
// Replace '192.168.1.5' with your actual machine's local IP
client.post("http://192.168.1.5:5000/data") { 
    // ...
}

```


3. **Network Requirements:** Ensure both your Android device and your laptop are connected to the **same Wi-Fi network**. If you are on a restricted network (like a public college Wi-Fi), use a mobile hotspot from your phone and connect your laptop to it to bypass network isolation.

### C. Troubleshooting

* **Connection Timeout:** If the app cannot connect, check your **Windows Firewall** settings. Ensure `python.exe` is granted permission for **Private** and **Public** networks.
* **Verify Data:** Once the app runs, you should see logs appearing in your server terminal. Verify that the file `~/sensor_data.csv` is being updated by running:
```bash
tail -f ~/sensor_data.csv

```
