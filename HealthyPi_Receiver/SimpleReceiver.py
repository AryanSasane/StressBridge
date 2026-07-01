from flask import Flask, request, jsonify
import csv
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CSV_FILE = os.path.join(BASE_DIR, "sensor_data.csv")

app = Flask(__name__)

# Ensure header exists
if not os.path.exists(CSV_FILE):
    with open(CSV_FILE, "w", newline='') as f:
        writer = csv.writer(f)
        writer.writerow([data['timestamp'], data['rawPpg'], data['motion'], data['gsr']])
        f.flush()            # Clear the internal buffer
        os.fsync(f.fileno()) # Force write to physical disk

@app.route('/data', methods=['POST'])
def receive_data():
    data = request.get_json()
    
    try:
        # Extract values using the EXACT keys found in your PARSED JSON output
        ts = data['timestamp']
        ppg = data['raw_ppg']  # This key exists in your log
        motion = data['motion'] # This key exists in your log
        gsr = data['gsr']       # This key exists in your log
        
        # Write to file
        with open(CSV_FILE, "a", newline='') as f:
            writer = csv.writer(f)
            writer.writerow([ts, ppg, motion, gsr])
            
        return jsonify({"status": "success"}), 200
        
    except Exception as e:
        # If this prints an error, it will tell us exactly what field is missing
        print(f"CRITICAL ERROR: {e}")
        return jsonify({"error": str(e)}), 400

if __name__ == '__main__':
    # host='0.0.0.0' allows connections from other devices
    app.run(host='0.0.0.0', port=5000)