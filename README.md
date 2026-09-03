# MineGov AI

An offline-first, AI-powered mining inspection and compliance governance platform.

MineGov AI enables field inspectors to capture mining violations with GPS, photos, videos, voice notes, and OCR-based document information. The system works offline using local storage and synchronizes data with the backend when connectivity is available.

The platform also includes AI-powered PPE/hazard detection using YOLO and a web-based governance dashboard for monitoring violations, risk, SLA, assignments, GIS data, and AI findings.

---

## Current Stable Version

**Checkpoint: 34D**

The current stable version includes:

- Android field inspection application
- Offline-first violation creation
- GPS/location capture
- Photo evidence
- Video evidence
- Voice notes
- Local Room database
- WorkManager synchronization
- FastAPI backend
- PostgreSQL database
- AI risk scoring
- YOLO PPE/hazard detection
- ML Kit OCR
- Governance dashboard
- GIS/map visualization
- Violation assignment
- SLA calculation
- Escalation logic
- AI risk and vision results

---

# Project Structure

```text
MineGovAI/
│
├── mobile-app/
│   └── Android field inspection application
│
├── backend/
│   └── FastAPI backend and REST APIs
│
├── ai-service/
│   └── YOLO-based computer vision service
│
├── governance-dashboard/
│   └── React + TypeScript governance dashboard
│
└── README.md





                    ┌─────────────────────┐
                    │   Android Mobile    │
                    │   Inspector App     │
                    └──────────┬──────────┘
                               │
                    Offline / Online
                               │
                               ▼
                    ┌─────────────────────┐
                    │    FastAPI Backend  │
                    └───────┬───────┬─────┘
                            │       │
                 ┌──────────┘       └──────────┐
                 ▼                             ▼
        ┌─────────────────┐          ┌─────────────────┐
        │   PostgreSQL    │          │    AI Service   │
        │    Database     │          │      YOLO       │
        └─────────────────┘          └─────────────────┘
                                             
                    ┌─────────────────────┐
                    │ Governance Dashboard│
                    │ React + TypeScript  │
                    └─────────────────────┘



Prerequisites

Install the following before running the project.

Required
Git
Android Studio
JDK 17
Android SDK
Python 3.13+
Node.js
npm
PostgreSQL
Recommended
Android physical device for testing camera/GPS
Stable local network/Wi-Fi hotspot for mobile-to-PC communication



1. Clone the Repository
git clone https://github.com/YOUR_USERNAME/MineGov-AI.git
cd MineGov-AI
2. Backend Setup

Go to the backend:

cd backend

Create a Python virtual environment:

Windows
python -m venv .venv

Activate it:

.\.venv\Scripts\activate

Install dependencies:

pip install fastapi uvicorn sqlalchemy psycopg2-binary httpx python-multipart
3. PostgreSQL Setup

Install and start PostgreSQL.

Create a database named:

minegov_db

Example using PostgreSQL:

CREATE DATABASE minegov_db;

The backend currently uses a PostgreSQL connection similar to:

postgresql://postgres:YOUR_PASSWORD@localhost:5432/minegov_db

Update the database configuration in:

backend/database/connection.py

Replace:

YOUR_PASSWORD

with your PostgreSQL password.

Important

Never commit real passwords or API keys to GitHub.

For a production deployment, use environment variables instead.

4. Run the Backend

From:

MineGovAI/backend

run:

python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000

Backend will run at:

http://127.0.0.1:8000

Health check:

http://127.0.0.1:8000/health

Swagger API documentation:

http://127.0.0.1:8000/docs
5. AI Service Setup

Open another terminal.

Go to:

cd "D:\BTECH(STUDY STUFF)\MineGovAI\ai-service"

Create the virtual environment:

python -m venv .venv

Activate:

.\.venv\Scripts\activate

Install dependencies:

pip install ultralytics fastapi uvicorn python-multipart pillow opencv-python huggingface_hub

The AI service uses the YOLO PPE model:

baskarmother/yolov8-ppe-construction

The trained model file should be available as:

ai-service/models/best.pt

The model file is intentionally excluded from GitHub because of its size.

If necessary, download/restore the model into:

ai-service/models/
6. Run the AI Service

From:

MineGovAI/ai-service

run:

python -m uvicorn app:app --reload --host 127.0.0.1 --port 8100

AI service:

http://127.0.0.1:8100

Health check:

http://127.0.0.1:8100/health

Swagger:

http://127.0.0.1:8100/docs
7. Backend + AI Service

The backend communicates with the AI service through:

http://127.0.0.1:8100

Therefore, both services must be running.

Terminal 1
Backend
Port 8000

Terminal 2
AI Service
Port 8100
8. Governance Dashboard Setup

Open another terminal:

cd governance-dashboard

Install dependencies:

npm install

Run the development server:

npm run dev

Vite will display the local URL, usually:

http://localhost:5173

Open that URL in your browser.

9. Android Mobile App Setup

Open:

mobile-app/

in Android Studio.

Recommended environment:

Android Studio
JDK 17
Android SDK

The project uses:

Kotlin
Jetpack Compose
Material 3
Room
WorkManager
CameraX
ML Kit OCR
Retrofit
OkHttp
Google Location Services
10. Android SDK

Make sure Android Studio has the required SDK installed.

Typical Windows SDK location:

C:\Users\<USERNAME>\AppData\Local\Android\Sdk

Android Studio can configure this automatically.

11. Configure Backend IP for Android

The Android application cannot use:

127.0.0.1

to access the backend running on your computer.

The phone must use the computer's local network IP.

For example:

http://10.235.217.43:8000/

Update the API base URL in:

mobile-app/app/src/main/java/com/minegov/ai/network/ApiClient.kt

Example:

baseUrl = "http://YOUR_PC_IP:8000/"

Replace:

YOUR_PC_IP

with your computer's IPv4 address.

Find your IP on Windows:

ipconfig

Look for:

IPv4 Address
12. Connect Android Phone

Enable:

Developer Options
USB Debugging

Connect the Android device through USB.

Check:

adb devices

Your device should appear in the list.

You can then run the application from Android Studio.

13. Required Android Permissions

The application requires permissions for features such as:

Camera
Location
Microphone

When Android requests permission, allow the required permissions.

These are required for:

Camera → Photo/Video evidence

Location → GPS tagging

Microphone → Voice notes
14. Test the Mobile Application

Launch the application.

Main workflow:

Login
  ↓
Home
  ↓
New Violation
  ↓
Select Violation Type
  ↓
Capture GPS
  ↓
Capture Photo
  ↓
Capture Video
  ↓
Record Voice Note
  ↓
OCR
  ↓
Enter/Edit Details
  ↓
Review
  ↓
Submit
15. Offline-First Workflow

The application is designed to work without continuous internet connectivity.

Test it by:

Turn off internet on the phone.
Open the application.
Create a violation.
Capture GPS.
Capture evidence.
Add OCR information.
Submit the violation.

The violation should be stored locally in the Room database.

The record initially has:

PENDING

When connectivity is restored, WorkManager attempts synchronization.

Expected flow:

Offline
   ↓
Room Database
   ↓
PENDING
   ↓
Network Restored
   ↓
WorkManager
   ↓
FastAPI
   ↓
PostgreSQL
   ↓
AI Vision Analysis
   ↓
SYNCED
16. YOLO AI Detection

The mobile application uploads the captured violation photo during synchronization.

Flow:

Violation Photo
       ↓
FastAPI
       ↓
AI Vision Service
       ↓
YOLO
       ↓
PPE Detection
       ↓
Risk Score
       ↓
Backend
       ↓
Dashboard

The AI service can detect PPE-related violations such as:

No Hardhat
No Mask
No Safety Vest
17. OCR Workflow

The current 34D implementation uses ML Kit OCR.

Flow:

Captured Image
      ↓
ML Kit OCR
      ↓
Extracted Text
      ↓
Inspector Review
      ↓
Use in Description
      ↓
Violation Record

The inspector can review/edit the extracted text before using it.

18. Governance Dashboard

The dashboard provides governance-level visibility into violations.

It includes:

Violation list
Severity
AI risk score
AI vision results
YOLO detections
GPS/map
Assignment
SLA
Status
Governance summary
Analytics
19. SLA

Current SLA logic is severity based:

CRITICAL → 4 hours
HIGH     → 8 hours
MEDIUM   → 24 hours
LOW      → 48 hours

The backend calculates the appropriate deadline when a violation is assigned.

20. Running Everything Together

For a complete local demonstration, run three services.

Terminal 1 — Backend
cd backend
.\.venv\Scripts\activate
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
Terminal 2 — AI Service
cd ai-service
.\.venv\Scripts\activate
python -m uvicorn app:app --reload --host 127.0.0.1 --port 8100
Terminal 3 — Dashboard
cd governance-dashboard
npm run dev

Then:

Android App
     ↓
FastAPI :8000
     ↓
PostgreSQL
     ↓
AI Service :8100

Dashboard
     ↓
FastAPI :8000
Troubleshooting
Backend doesn't start

Check PostgreSQL is running and verify:

DATABASE_URL

in:

backend/database/connection.py
Android cannot connect to backend

Check:

1. PC and phone are on the same network
2. Backend is running on 0.0.0.0
3. Android uses PC IPv4 address
4. Port 8000 is accessible
5. Windows Firewall isn't blocking port 8000

Test from the phone browser:

http://YOUR_PC_IP:8000/health
YOLO doesn't work

Check:

AI service is running
best.pt exists
backend can reach port 8100

Test:

http://127.0.0.1:8100/health
Dashboard doesn't load

Run:

npm install
npm run dev

and check the Vite URL shown in the terminal.

Development Workflow

After making changes:

git status
git add .
git commit -m "describe your change"
git push

The stable 34D version is tagged:

v34D

This can be used as the rollback checkpoint for the project.

Current Development Status
Mobile App                 ✅
Offline Storage            ✅
GPS                        ✅
Photo Evidence             ✅
Video Evidence             ✅
Voice Notes                ✅
Room Database              ✅
WorkManager Sync           ✅
FastAPI Backend            ✅
PostgreSQL                 ✅
AI Risk Engine             ✅
YOLO Vision                ✅
ML Kit OCR                 ✅
Governance Dashboard       ✅
GIS Map                    ✅
Assignment                 ✅
SLA                        ✅
34D Checkpoint             ✅
Future Development

Planned future phases include:

34E — Separate Document Evidence
34F — OCR Field Extraction
35  — Whisper Speech-to-Text
36  — Voice Agent Integration
37  — Notifications & Escalation
38  — Audit Trail
39  — MinIO Evidence Storage
40  — Authentication & RBAC
41  — Advanced Analytics
42  — Advanced GIS
43  — End-to-End Testing
44  — UI & Demo Polish
45  — Final Demo
License

Add your project license here.

Team

MineGov AI


### One important change I'd make before putting this publicly

Your current backend configuration contains the PostgreSQL connection string format, and your Android app contains a local IP. **Don't put real passwords/API keys in the README or source code.**

For GitHub, I'd eventually change the backend to:

```text
DATABASE_URL = os.getenv("DATABASE_URL")

and use a .env file locally.
