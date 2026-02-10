# 🩸 Dia-Smart: IoT-Enabled Diabetes Compliance Ecosystem

![Status](https://img.shields.io/badge/Status-Prototype-green)
![Platform](https://img.shields.io/badge/Platform-ESP32%20%7C%20Flutter-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

> **"Prescription is Science. Administration is Human."**

**Dia-Smart** is a dual-node IoT ecosystem designed to bridge the gap between medical prescription and patient compliance. It addresses the "Last Mile" problem in diabetes care by automating the tracking of **insulin dosage**, **storage conditions**, and **blood glucose trends** for elderly and Type 1 diabetic patients.

Unlike standard logbooks, Dia-Smart creates a **closed-loop system** that correlates prescribed doses with actual injected units and physical inventory.

---

## 🧐 The Problem
Diabetes management requires precise coordination between blood sugar testing and insulin administration. However, current solutions have major "blind spots":
* [cite_start]**Dosage Errors:** Elderly patients often forget if they took their shot or dial the wrong dose due to poor vision[cite: 8, 10].
* [cite_start]**Manual Logging:** Patients often fail to accurately record their readings in logbooks[cite: 26, 28].
* [cite_start]**Unsafe Storage:** Insulin kept in domestic fridges is often subject to spoilage without the user knowing[cite: 20, 21].

## 💡 Our Solution
Dia-Smart separates the **Storage** from the **User Interface**, creating a distributed system that monitors the patient at every step:

### 1. 🧠 Smart Insulin Pen Cap (Dosage Tracker)
A retrofit smart cap that clips onto standard insulin pens to automatically log injection data.
* **Inertial Sensing:** Uses an **MPU6050 Gyroscope** to detect the exact angle of dosage dialing ($15^\circ \approx 1 \text{ Unit}$), distinguishing between dialing up and correcting down.
* **Injection Detection:** Accelerometer-based gesture recognition detects the specific motion of injection.

### 2. 🔌 Gluco-Fetcher (Universal Bridge)
A retrofit module that upgrades non-smart glucometers (specifically **Mega Check TD-4257**) into IoT devices.
* **Protocol Hacking:** Intercepts RS-232 serial data from the glucometer's stereo jack to read historical values.
* [cite_start]**Auto-Sync:** Automatically fetches glucose readings + timestamps and uploads them to the cloud via Wi-Fi[cite: 58].

### 3. ❄️ Smart Cold-Storage Unit (Inventory & Safety)
A passive sensing enclosure placed inside the refrigerator.
* [cite_start]**Inventory Logic:** High-precision **Load Cells** detect if the insulin pen is present and estimate remaining cartridge levels[cite: 62].
* [cite_start]**Safety Monitoring:** **DS18B20** sensor alerts guardians if the fridge temperature deviates from the safe range ($2^\circ\text{C} - 8^\circ\text{C}$)[cite: 60].

### 4. 📱 Companion App (Flutter)
* [cite_start]**Real-time Dashboard:** Visualizes blood sugar vs. insulin dosage graphs[cite: 64].
* **Guardian Alerts:** Push notifications for missed doses, hypoglycemia events, or critical inventory levels.

---

## 🛠️ Technology Stack

| Domain | Technology / Component |
| :--- | :--- |
| **Microcontrollers** | ESP32-C3 SuperMini (Wearable), ESP32 DevKit V1 (Base) |
| **Sensors** | MPU6050 (IMU), HX711 + Load Cell, DS18B20 (Temp), MAX3232 (RS232-TTL) |
| **Connectivity** | BLE 5.0, ESP-NOW, MQTT over Wi-Fi |
| **Mobile App** | Flutter (Dart) |
| **Backend** | Firebase (Firestore, Cloud Functions) |

---

## ⚡ System Architecture

The system operates on a Master-Slave topology:
1.  **The Edge (Smart Cap):** Sleeps 99% of the time. Wakes on movement to record dosage.
2.  **The Hub (Base Station):** Always powered. Connects to the Glucometer and acts as the Wi-Fi gateway for the Cap.
3.  **The Cloud:** Processes data and triggers alerts to the Guardian's phone.

---

## 👥 The Team (Group 07)

* **E/21/031** - Ananthasagaran N. ([Email](mailto:e21031@eng.pdn.ac.lk))
* **E/21/036** - Arnikan U. ([Email](mailto:e21036@eng.pdn.ac.lk))
* **E/21/356** - Sanjeevan U. ([Email](mailto:e21356@eng.pdn.ac.lk))
* **E/21/386** - Sivasuthan J. ([Email](mailto:e21386@eng.pdn.ac.lk))

**Supervisors:**
* Dr. [Supervisor Name]
* Dr. [Co-Supervisor Name]

---

## ⚖️ License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
