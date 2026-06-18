7bee035 | Arnikan18 | 2026-06-16 | test: add backend and frontend automated test suites
d64e8a5 | Arnikan18 | 2026-06-16 | Merge remote-tracking branch 'origin/develop' into Arni-new
010c0a8 | Arnikan18 | 2026-06-16 | test: add backend and frontend automated tests
d50e84f | ananthu65 | 2026-06-16 | Merge branch 'ananthu-dev' into develop
3074e27 | ananthu65 | 2026-06-16 | feat(firmware): calibrate inner inventory and xiao pen dose tracking
74a2128 | Sivasuthan Jeganathan | 2026-06-15 | DS-1 Fix frontend build for AWS deployment
fc5f7a8 | Arnikan18 | 2026-06-14 | Merge pull request #45 from cepdnaclk/Arni-new
42d2ab6 | Arnikan18 | 2026-06-14 | Add authentication and landing page features
e44b5e2 | Sivasuthan Jeganathan | 2026-06-14 | Fix frontend TypeScript build errors
99df737 | ananthu65 | 2026-06-13 | fix(firmware/outer-unit): reduce display redraw flicker
56443a7 | ananthu65 | 2026-06-13 | feat(firmware/outer-unit): add status display pages
5f2a96b | ananthu65 | 2026-06-13 | feat(firmware/outer-unit): expose mqtt queue status to display
9f315b9 | ananthu65 | 2026-06-13 | feat(firmware/outer-unit): retain confirmed dose display state
05d4f24 | ananthu65 | 2026-06-11 | feat(firmware/outer-unit): queue telemetry json while offline
b8a25b5 | ananthu65 | 2026-06-11 | fix(firmware/outer-unit): make mqtt publish report failures
2614621 | ananthu65 | 2026-06-10 | chore(firmware): restore local wifi and upload ports
21bab8c | ananthu65 | 2026-06-10 | Merge branch 'ananthu-dev' into develop
a469af4 | ananthu65 | 2026-06-10 | feat(firmware/outer-unit): add keypad dose confirmation
8f98d51 | Arnikan18 | 2026-06-10 | Merge pull request #44 from cepdnaclk/Arni-new
7924f07 | Arnikan18 | 2026-06-09 | feat(frontend): The hardcoded endpoints changed to actual logic
da3a872 | Arnikan18 | 2026-06-09 | Merge pull request #43 from cepdnaclk/Arni-new
f51f6e9 | Arnikan18 | 2026-06-09 | feat(frontend): Implement frontend-backend integration for profile, alerts, dashboard, prescriptions and analytics
d11a0b5 | Arnikan18 | 2026-06-09 | Merge pull request #42 from cepdnaclk/Arni-new
afbc3a0 | Arnikan18 | 2026-06-09 | feat(frontend): Integrate authentication, profile and alerts APIs
f4dfbaa | ananthu65 | 2026-06-08 | fix(firmware): stabilize inner espnow and outer ble flow
7082d33 | ananthu65 | 2026-06-08 | feat(firmware): add fast inner door trigger
2427e9f | ananthu65 | 2026-06-08 | fix(firmware/outer-unit): publish inner sensor changes
a9cd69c | ananthu65 | 2026-06-08 | feat(firmware/inner-unit): add battery adc telemetry
a8293c2 | ananthu65 | 2026-06-08 | feat(firmware/outer-unit): add raw tft dashboard
eabbd3d | ananthu65 | 2026-06-07 | feat(firmware/outer-unit): add tft dashboard
ae61261 | ananthu65 | 2026-06-07 | docs(firmware/inner-unit): document calibration triggers
4785dc6 | ananthu65 | 2026-06-07 | feat(firmware/outer-unit): schedule ble scan windows
72f71a6 | ananthu65 | 2026-06-07 | feat(firmware/pen-unit): add pending-aware advertising
2eef58c | ananthu65 | 2026-06-07 | Merge branch 'ananthu-dev' into develop
e0c8f7c | ananthu65 | 2026-06-07 | feat(firmware/outer-unit): dedupe glucometer sequences
79295e4 | Arnikan18 | 2026-06-07 | Merge pull request #41 from cepdnaclk/Arni-new
c2bd536 | Arnikan18 | 2026-06-07 | feat(frontend): implement service-based data architecture
22e4c9d | ananthu65 | 2026-06-07 | feat(firmware): ack pen doses and balance ble scans
62bfb0c | ananthu65 | 2026-06-07 | Merge branch 'ananthu-dev' into develop
3d2d3d9 | ananthu65 | 2026-06-07 | feat(firmware): sync pen dose timestamps over ble
ed6ae31 | Arnikan18 | 2026-06-06 | Merge pull request #40 from cepdnaclk/Arni-new
fbf3017 | Arnikan18 | 2026-06-06 | feat(frontend): add authentication and role-based navigation infrastructure
734469d | Arnikan18 | 2026-06-06 | Merge pull request #39 from cepdnaclk/Arni-new
b1d8bfb | Arnikan18 | 2026-06-06 | feat(frontend): implement dashboard, analytics, alerts and prescriptions pages
92078c8 | Arnikan18 | 2026-06-06 | Initialize React Vite frontend
d7d86b7 | Arnikan18 | 2026-06-06 | Initialize React Vite TypeScript frontend
285786b | ananthu65 | 2026-06-05 | feat(firmware/pen-unit): save confirmed doses before queueing
b1a1735 | ananthu65 | 2026-06-05 | Revert "feat(firmware/pen-unit): save doses before queueing"
3739b37 | ananthu65 | 2026-06-05 | feat(firmware/pen-unit): save doses before queueing
51f94b6 | ananthu65 | 2026-06-05 | feat(firmware/pen-unit): persist dose records in nvs
e577f62 | ananthu65 | 2026-06-05 | feat(firmware/pen-unit): add dose storage service skeleton
d97d589 | ananthu65 | 2026-06-05 | feat(firmware/pen-unit): add persistent dose record model
0701d96 | ananthu65 | 2026-06-05 | feat(firmware/common): define source freshness state model
61955d8 | ananthu65 | 2026-06-05 | feat(firmware/common): define shared ack protocol types
a90bea5 | ananthu65 | 2026-06-05 | feat(firmware/common): add internal event identity metadata
ffcd4d4 | ananthu65 | 2026-06-05 | refactor(firmware/common): centralize shared packet structs
ec9d01c | ananthu65 | 2026-06-04 | docs(workflow): add firmware branch sync loop
6b1455c | ananthu65 | 2026-06-04 | docs(firmware): record phase 0 baseline
63e737c | ananthu65 | 2026-06-04 | fix(firmware): stabilize MQTT delivery and align telemetry topic
1338d7a | ananthu65 | 2026-06-01 | fix(firmware/outer-unit): restore Accu-Chek glucose sync with fresh GATT rediscovery
c181b33 | ananthu65 | 2026-06-01 | fix(firmware/inner-unit): call initEspNow() in task, use peer.channel=0 for auto WiFi channel
0675de3 | ananthu65 | 2026-05-29 | merge(firmware): Phase 3 outer-unit BLE+ESP-NOW+MQTT from ananthu-dev
fb0dee4 | ananthu65 | 2026-05-29 | fix(firmware/outer-unit): fix ESP_BLE_SEC_NONE compile error + update mock to use doseUnits field
3ce418c | ananthu65 | 2026-05-29 | feat(firmware/outer-unit): implement ble_manager — pen GATT notify + glucometer RACP state machine
2d2a017 | ananthu65 | 2026-05-29 | feat(firmware/outer-unit): rewrite event_aggregator_task — real queue drain, NTP timestamp, WiFi RSSI, heap size
e4f6c57 | ananthu65 | 2026-05-29 | feat(firmware/outer-unit): main.cpp — ESP-NOW rx callback, 4 queues, NTP sync, bleManagerTask spawn
41cc6c0 | ananthu65 | 2026-05-29 | fix(firmware/outer-unit): json_serializer — PATIENT_ID Long, device UIDs, compact JSON, full payload with battery section
42fb4fe | ananthu65 | 2026-05-29 | feat(firmware/outer-unit): update TelemetryEvent — add doseUnits(float), estimatedPercent, RSSI, heap, sequenceNumber fields
5de09b6 | ananthu65 | 2026-05-29 | feat(firmware/outer-unit): update system_queues.h — add InnerPacket, GlucoseReading, DoseReading structs + queue externs
d2e6e91 | ananthu65 | 2026-05-29 | feat(firmware/outer-unit): update app_config.h — fix PATIENT_ID, add device UIDs and BLE/ESP-NOW constants
52930d5 | ananthu65 | 2026-05-29 | merge(firmware): Phase 1+2 pen-unit and inner-unit firmware from ananthu-dev
060665e | ananthu65 | 2026-05-29 | feat(firmware/inner-unit): add main.cpp — WiFi channel lock + sensorSamplingTask spawn
a99c092 | ananthu65 | 2026-05-29 | feat(firmware/inner-unit): add sensor_sampling_task (DS18B20+HX711+reed+ESP-NOW)
dd62ddf | ananthu65 | 2026-05-29 | feat(firmware/inner-unit): add InnerPacket model (ESP-NOW struct)
7ca8551 | ananthu65 | 2026-05-29 | feat(firmware/inner-unit): add app_config.h with all hardware constants
2d1b18e | ananthu65 | 2026-05-29 | feat(firmware/inner-unit): add platformio.ini build config
c97ec94 | ananthu65 | 2026-05-29 | feat(firmware/pen-unit): add main.cpp FreeRTOS setup and task creation
032b54a | ananthu65 | 2026-05-29 | feat(firmware/pen-unit): add ble_transfer_task BLE GATT server with notify
83ff8c9 | ananthu65 | 2026-05-29 | feat(firmware/pen-unit): add dose_detection_task with AS5600 + button debounce
cc162fe | ananthu65 | 2026-05-29 | feat(firmware/pen-unit): add DoseEvent model struct
2aa0367 | ananthu65 | 2026-05-29 | feat(firmware/pen-unit): add app_config.h with all hardware constants
120e62c | ananthu65 | 2026-05-29 | feat(firmware/pen-unit): add platformio.ini build config
08eb762 | ananthu65 | 2026-05-25 | Merge branch 'ananthu-dev' into develop
f7d924b | ananthu65 | 2026-05-25 | feat(admin): add devices endpoint GET /api/v1/admin/devices
160c8cf | ananthu65 | 2026-05-25 | Merge branch 'ananthu-dev' into develop
c0f6639 | ananthu65 | 2026-05-25 | feat(admin): add audit-logs endpoint GET /api/v1/admin/audit-logs
8e0945a | ananthu65 | 2026-05-25 | Merge branch 'ananthu-dev' into develop
0c9e89c | ananthu65 | 2026-05-25 | feat(analytics): complete analytics module + fix dashboard RBAC and alert scoping
0a9f4a4 | ananthu65 | 2026-05-25 | Merge branch 'ananthu-dev' into develop
6212c85 | ananthu65 | 2026-05-25 | AdherenceAnalyticsService
32b5b72 | ananthu65 | 2026-05-25 | Merge branch 'ananthu-dev' into develop
43b0b61 | ananthu65 | 2026-05-25 | AdherenceAnalyticsResponse
eedb2d1 | ananthu65 | 2026-05-25 | Merge branch 'ananthu-dev' into develop
db754ab | ananthu65 | 2026-05-25 | DailyAdherenceBreakdown
aecf3c4 | ananthu65 | 2026-05-25 | Merge branch 'ananthu-dev' into develop
f90beb3 | ananthu65 | 2026-05-25 | feat(analytics): add analytics module skeleton - AdherenceEntry DTO, DoseScheduleRepository list method
46ac383 | Arnikan18 | 2026-05-24 | Merge pull request #38 from cepdnaclk/Arni-new
e941c7e | Arnikan18 | 2026-05-24 | feat(outer-unit): implement FreeRTOS queues, JSON serialization, and AWS IoT mTLS connection
0713ea8 | Arnikan18 | 2026-05-24 | Merge pull request #37 from cepdnaclk/Arni-new
1beac4d | Arnikan18 | 2026-05-24 | Implemented stable FreeRTOS telemetry event pipeline for outer unit
2e0c6af | Arnikan18 | 2026-05-22 | Merge pull request #36 from cepdnaclk/Arni-new
12cf2dd | Arnikan18 | 2026-05-22 | Implement initial FreeRTOS event pipeline architecture
3f8e9eb | ananthu65 | 2026-05-22 | Merge pull request #35 from cepdnaclk/ananthu-dev
1b932e3 | ananthu65 | 2026-05-21 | Ananthu: alert fixes, permission expansion, exception handlers, config fixes on develop base
670296d | Arnikan18 | 2026-05-19 | Merge pull request #33 from cepdnaclk/Arni-new
bad8f4a | Arnikan18 | 2026-05-19 | Add schedule adherence tracking and improve exception handling
95aed89 | Arnikan18 | 2026-05-19 | Merge pull request #32 from cepdnaclk/Arni-new
20c9854 | Arnikan18 | 2026-05-19 | Implement dose schedule adherence and schedule CRUD APIs
3005b03 | Arnikan18 | 2026-05-18 | Merge pull request #31 from cepdnaclk/Arni-new
fa73f18 | Arnikan18 | 2026-05-18 | Implement prescription management APIs with RBAC support
071fd51 | Arnikan18 | 2026-05-18 | Merge pull request #30 from cepdnaclk/Arni-new
4936930 | Arnikan18 | 2026-05-18 | Complete patient telemetry APIs and fix RBAC onboarding integration
2350503 | Arnikan18 | 2026-05-18 | Implement dose telemetry APIs and fix patient RBAC provisioning
eedb27f | Arnikan18 | 2026-05-18 | Merge pull request #29 from cepdnaclk/Arni-new
f9e12f2 | Arnikan18 | 2026-05-18 | Fix patient ownership provisioning and stabilize patient APIs
0118526 | Arnikan18 | 2026-05-18 | Merge remote-tracking branch 'origin/develop' into Arni-new
8c398ca | Arnikan18 | 2026-05-18 | Add patient profile API with RBAC integration
2a18758 | Sivasuthan Jeganathan | 2026-05-17 | Merge pull request #28 from cepdnaclk/siva-admin-user-managemnet-api
300e0e2 | Sivasuthan Jeganathan | 2026-05-17 | Add admin user managemnet APIs.
4a257fd | Sivasuthan Jeganathan | 2026-05-16 | Add patient access management APIs
331c06e | Sivasuthan Jeganathan | 2026-05-16 | Merge pull request #27 from cepdnaclk/siva-patient-access-management-api
50e1554 | Sivasuthan Jeganathan | 2026-05-16 | Add patient access management APIs
7922408 | Sivasuthan Jeganathan | 2026-05-16 | Merge pull request #26 from cepdnaclk/siva-user-patient-access-layer
3f220d7 | Sivasuthan Jeganathan | 2026-05-16 | Add user-patient access authorization layer
635103b | Sivasuthan Jeganathan | 2026-05-16 | Merge pull request #25 from cepdnaclk/siva-changepassword-api
6ebbfbe | Sivasuthan Jeganathan | 2026-05-16 | Add current user password change API
51b88c3 | Sivasuthan Jeganathan | 2026-05-16 | Merge pull request #24 from cepdnaclk/siva-fix-userid-references
bdb18ad | Sivasuthan Jeganathan | 2026-05-16 | Fix user id references after schema alignment
35fae19 | Sivasuthan Jeganathan | 2026-05-16 | Merge pull request #23 from cepdnaclk/siva-change-password-api
f9d3998 | Sivasuthan Jeganathan | 2026-05-16 | Align auth and user APIs with final database schema
c1bb9bc | Arnikan18 | 2026-05-15 | Merge pull request #22 from cepdnaclk/Arni-new
1eb3d57 | Arnikan18 | 2026-05-15 | Integrate MQTT-driven automatic alert generation
168df26 | Arnikan18 | 2026-05-15 | Implement inventory alert evaluation infrastructure
7196688 | Arnikan18 | 2026-05-15 | Implement alert generation infrastructure and storage alert evaluation
c53000c | Arnikan18 | 2026-05-15 | Merge pull request #21 from cepdnaclk/Arni-new
d393aab | Arnikan18 | 2026-05-15 | Implement alerts module and dashboard summary APIs
b0821d5 | Arnikan18 | 2026-05-15 | Merge pull request #20 from cepdnaclk/Arni-new
09969a9 | Arnikan18 | 2026-05-15 | Implement glucose, dose, and storage telemetry GET APIs
1fbdaef | Sivasuthan Jeganathan | 2026-05-14 | Merge pull request #19 from cepdnaclk/siva-profile-update-api
ff9c40f | Sivasuthan Jeganathan | 2026-05-14 | Add current user profile update API
4e81be6 | U.Sanjeevan | 2026-05-14 | Merge pull request #18 from cepdnaclk/sanjeevan-dev
a94e5e7 | SanjeevanUthayachandran | 2026-05-14 | Implement MQTT device processing and local development working setup
a735c3a | U.Sanjeevan | 2026-05-14 | Merge pull request #17 from cepdnaclk/sanjeevan-dev
53bf5b4 | SanjeevanUthayachandran | 2026-05-14 | Resolved merge conflicts using current branch
21d2460 | SanjeevanUthayachandran | 2026-05-14 | add devices and notification apis
40866a6 | Sivasuthan Jeganathan | 2026-05-14 | Merge pull request #15 from cepdnaclk/siva-clean-runtime-files
e2ead14 | Sivasuthan Jeganathan | 2026-05-14 | Remove MQTT runtime lock file from tracking
a5ba44b | Sivasuthan Jeganathan | 2026-05-14 | Merge pull request #14 from cepdnaclk/siva-users-api
87b941c | Sivasuthan Jeganathan | 2026-05-14 | Add shared current user service for JWT modules
16771da | SanjeevanUthayachandran | 2026-05-13 | Implemented MQTT telemetry ingestion pipeline, IOT device to backend then RDS all are successly integrate and testing completed
20d56e1 | SanjeevanUthayachandran | 2026-05-13 | Implemented MQTT telemetry ingestion pipeline, IOT device to backend then RDS all are successly integrate and testing completed
6563eac | Sivasuthan Jeganathan | 2026-05-12 | Add current user profile API
d8bbb8c | Sivasuthan Jeganathan | 2026-05-12 | Merge pull request #12 from cepdnaclk/siva-login-jwt
bbde3b9 | Sivasuthan Jeganathan | 2026-05-12 | Add login API and JWT authentication
e5c91bc | Sivasuthan Jeganathan | 2026-05-12 | Normalize dev YAML configuration keys
2fd7c60 | Sivasuthan Jeganathan | 2026-05-11 | Merge pull request #11 from cepdnaclk/siva-register-api
a44164a | Sivasuthan Jeganathan | 2026-05-10 | Add user registration API
34a16bc | Sivasuthan Jeganathan | 2026-05-09 | Add user registration API
37d908c | Sivasuthan Jeganathan | 2026-05-09 | Merge pull request #9 from cepdnaclk/siva-auth-security
8519b2a | Sivasuthan Jeganathan | 2026-05-09 | Add AppUser entity and repository
ae902cf | Sivasuthan Jeganathan | 2026-05-09 | Add shared enums and API response DTOs
faa5355 | SanjeevanUthayachandran | 2026-05-08 | databse , iot coreand backend integration are successfully done
dad3173 | Arnikan18 | 2026-05-08 | Initialize Spring Boot backend structure
ae2edd7 | SanjeevanUthayachandran | 2026-05-06 | updated final rds schema and rbac database improvment
c3f82f6 | SanjeevanUthayachandran | 2026-05-05 | Added final sql file
7996a92 | Arnikan18 | 2026-05-01 | chore: add .gitkeep files to track empty folders
1600238 | Arnikan18 | 2026-05-01 | Merge pull request #7 from cepdnaclk/repo-restructure
c10901a | Arnikan18 | 2026-05-01 | refactor: move glucometer code to legacy outer-unit
c411f1c | Arnikan18 | 2026-05-01 | refactor: restructure repo and remove node_modules from tracking
6a33996 | Arnikan18 | 2026-05-01 | refactor: restructure repo for spring backend and milestone 3 architecture
5870a59 | Arnikan18 | 2026-05-01 | Merge branch 'develop' of https://github.com/cepdnaclk/e21-3yp-dia-smart into develop