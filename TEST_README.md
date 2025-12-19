# ThingsBoard Technical Assignment - Backend Extension

**Participant:** Faisal Faris Al-Oufi

## 1. Code Comprehension

A summary of the core modules reviewed, detailing their purpose and key classes:

| Module | Purpose | Key Classes & Role |
| :--- | :--- | :--- |
| **`application`** | The central service containing the core business logic, REST API controllers, and security enforcement. | `BaseController`, `PingController` |
| **`dao`** | The Data Access Object layer. Responsible for abstracting and managing database interactions (e.g., fetching device records). | `DeviceService` |
| **`transport`** | Handles device communication protocols (MQTT, HTTP, CoAP). This is the initial entry point for device telemetry data. | `MqttTransportService` |
| **`ui-ngx`** | The Angular web frontend. Provides the user interface for viewing data and managing entities. | `DeviceTabsComponent`, `DeviceService` (Angular HTTP service) |

---

## 2. Overview of Implemented Changes

| Task | Affected Files | Implementation Details |
| :--- | :--- | :--- |
| **Backend API** | `PingController.java` | Added the secure endpoint `GET /api/device/ping/{deviceId}`. Logic determines **`reachable`** based on the device's creation time being within the last 24 hours (for simplified demonstration). |
| **Unit Testing** | `PingControllerTest.java` | Implemented Mockito unit tests to verify the controller's logic, confirming correct `true` (reachable) and `false` (unreachable) responses. |
| **Frontend Service** | `device.service.ts` | Added the `pingDevice(deviceId: string)` method to correctly map the API call to the new backend endpoint. |
| **Frontend UI** | `device-tabs.component.ts/.html` | Implemented the `pingDevice` function in the component and integrated a "Ping Device" button on the UI, displaying the result in an **alert popup**. |

---

## 3. Build & Test Instructions

### Prerequisites
* Java Development Kit (JDK) **17**
* Apache Maven 3.6+

### Full Project Build
To clean and install the entire project:
```bash
mvn clean install -DskipTests