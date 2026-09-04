MINEGOV AI - FINAL CORE GOVERNANCE UPDATE

Replace:
1. governance-dashboard/src/api.ts -> api.ts
2. governance-dashboard/src/App.tsx -> App.tsx
3. governance-dashboard/src/App.css -> App.css
4. backend/routes/violations.py -> violations.py

Implemented together:
- Inspector Resolution Verification button using backend /verify
- Audit Trail GET endpoint aligned to /audit
- Audit trail refresh after assignment/status/verification
- Automatic SLA escalation remains active in backend main.py
- Escalation + SLA alerts in dashboard notification popover
- Role selector: Inspector / Supervisor / Admin (UI-level MVP)
- CSV export of filtered violations
- Governance analytics strip: escalated, high/critical AI risk, SLA compliance, open
- Existing mobile/offline sync, OCR, Whisper, YOLO, AI risk, GIS, assignment and SLA features preserved

Commands:
BACKEND:
cd "D:\BTECH(STUDY STUFF)\MineGovAI\backend"
.\.venv\Scripts\python.exe -m uvicorn main:app --reload --port 8000

DASHBOARD:
cd "D:\BTECH(STUDY STUFF)\MineGovAI\governance-dashboard"
npm run dev

Test flow:
1. Create violation from Android.
2. Sync it.
3. Open dashboard.
4. Assign officer.
5. Set IN_PROGRESS.
6. Set RESOLVED.
7. Click Verify Resolution.
8. Audit Trail should show CREATED -> ASSIGNED -> STATUS_CHANGED -> VERIFIED.
9. Export CSV.
10. Check Bell for SLA alerts.
