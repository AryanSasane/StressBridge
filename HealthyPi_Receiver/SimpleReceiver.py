from flask import Flask, request, jsonify
import csv
import os

app = Flask(__name__)
CSV_FILE = "sensor_data.csv"

# Ensure header exists
if not os.path.exists(CSV_FILE):
    with open(CSV_FILE, "w", newline='') as f:
        writer = csv.writer(f)
        writer.writerow(["timestamp", "raw_ppg", "motion", "gsr"])

@app.route('/data', methods=['POST'])
def receive_data():
    data = request.json
    print(f"Received: {data}")
    
    with open(CSV_FILE, "a", newline='') as f:
        writer = csv.writer(f)
        writer.writerow([data['timestamp'], data['raw_ppg'], data['motion'], data['gsr']])
        
    return jsonify({"status": "success"}), 200

if __name__ == '__main__':
    # host='0.0.0.0' allows connections from other devices
    app.run(host='0.0.0.0', port=5000)