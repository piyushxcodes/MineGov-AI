import asyncio
import time

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from database.connection import engine, SessionLocal
from database.models import Base, Violation
from database.audit import AuditLog
from routes.violations import router as violations_router
from services.escalation_service import check_sla_breach


# Create database tables if they don't already exist
Base.metadata.create_all(bind=engine)


app = FastAPI(
    title="MineGov AI Backend",
    description="Backend API for MineGov AI mining inspection and compliance platform",
    version="1.0.0",
)


# --------------------------------------------------
# CORS
# --------------------------------------------------

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "https://governance-dashboard-wfh9.onrender.com",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# --------------------------------------------------
# SLA MONITOR
# --------------------------------------------------

async def sla_monitor():
    """
    Checks active assigned violations every 60 seconds.
    Automatically changes breached violations to ESCALATED.
    """

    while True:
        db = SessionLocal()

        try:
            violations = (
                db.query(Violation)
                .filter(
                    Violation.sla_deadline.isnot(None),
                    Violation.status.notin_(
                        ["RESOLVED", "VERIFIED", "CLOSED", "ESCALATED"]
                    ),
                )
                .all()
            )

            now = int(time.time() * 1000)

            for violation in violations:

                if check_sla_breach(
                    violation.sla_deadline,
                    violation.status,
                ):
                    violation.status = "ESCALATED"
                    violation.updated_at = now

                    print(
                        f"[SLA] ESCALATED violation "
                        f"{violation.id} | "
                        f"assigned_to={violation.assigned_to}"
                    )

            db.commit()

        except Exception as error:
            db.rollback()
            print(f"[SLA MONITOR ERROR] {error}")

        finally:
            db.close()

        await asyncio.sleep(60)


@app.on_event("startup")
async def startup_event():
    asyncio.create_task(sla_monitor())
    print("[SLA] Background monitor started")


# --------------------------------------------------
# API ROUTES
# --------------------------------------------------

app.include_router(violations_router)


# --------------------------------------------------
# ROOT
# --------------------------------------------------

@app.get("/")
def root():
    return {
        "message": "MineGov AI Backend is running",
        "status": "online",
    }


# --------------------------------------------------
# HEALTH
# --------------------------------------------------

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
    }