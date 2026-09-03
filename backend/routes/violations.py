import json
import time
import uuid

from fastapi import (
    APIRouter,
    Depends,
    File,
    HTTPException,
    UploadFile,
)
from pydantic import BaseModel
from sqlalchemy.orm import Session

from database.connection import get_db
from database.models import Violation

from services.ai_service import analyze_violation
from services.ai_vision_service import (
    analyze_image,
    build_vision_finding,
    serialize_detections,
)
from services.sla_service import (
    get_sla_status,
    get_remaining_sla_ms,
)
from services.escalation_service import (
    escalation_required,
)


router = APIRouter(
    prefix="/api/v1/violations",
    tags=["Violations"],
)


# ============================================================
# REQUEST MODELS
# ============================================================

class ViolationCreate(BaseModel):
    localId: str
    violationType: str
    description: str
    observedCondition: str
    severity: str

    latitude: float | None = None
    longitude: float | None = None

    photoUri: str | None = None
    videoUri: str | None = None
    voiceUri: str | None = None

    createdAt: int


class AssignmentRequest(BaseModel):
    assignedTo: str


class StatusUpdateRequest(BaseModel):
    status: str


# ============================================================
# HELPERS
# ============================================================

ALLOWED_STATUSES = {
    "SUBMITTED",
    "ASSIGNED",
    "IN_PROGRESS",
    "RESOLVED",
    "VERIFIED",
    "CLOSED",
}


def violation_to_dict(violation: Violation):
    """
    Convert SQLAlchemy violation into API response.
    """

    return {
        "id": violation.id,
        "localId": violation.local_id,

        "violationType": violation.violation_type,
        "description": violation.description,
        "observedCondition": violation.observed_condition,
        "severity": violation.severity,

        "latitude": violation.latitude,
        "longitude": violation.longitude,

        "photoUri": violation.photo_uri,
        "videoUri": violation.video_uri,
        "voiceUri": violation.voice_uri,

        "status": violation.status,

        "assignedTo": violation.assigned_to,
        "assignedAt": violation.assigned_at,
        "slaDeadline": violation.sla_deadline,

        "slaStatus": get_sla_status(
            violation.sla_deadline,
            violation.status,
        ),

        "remainingSlaMs": get_remaining_sla_ms(
            violation.sla_deadline,
            violation.status,
        ),

        "escalationRequired": escalation_required(
            violation.sla_deadline,
            violation.status,
        ),

        # Rule/NLP AI
        "aiRiskScore": violation.ai_risk_score,
        "aiRiskLevel": violation.ai_risk_level,
        "aiFinding": violation.ai_finding,
        "aiConfidence": violation.ai_confidence,

        # YOLO Vision AI
        "aiDetections": (
            json.loads(violation.ai_detections)
            if violation.ai_detections
            else []
        ),

        "aiVisionFindings": violation.ai_vision_findings,
        "aiVisionScore": violation.ai_vision_score,

        "createdAt": violation.created_at,
        "updatedAt": violation.updated_at,
    }


# ============================================================
# CREATE VIOLATION
# ============================================================

@router.post("")
def create_violation(
    payload: ViolationCreate,
    db: Session = Depends(get_db),
):

    # --------------------------------------------------------
    # Prevent duplicate offline sync
    # --------------------------------------------------------

    existing = (
        db.query(Violation)
        .filter(Violation.local_id == payload.localId)
        .first()
    )

    if existing:

        return {
            "id": existing.id,
            "message": "Violation already exists",
            "duplicate": True,

            "aiRiskScore": existing.ai_risk_score,
            "aiRiskLevel": existing.ai_risk_level,
            "aiFinding": existing.ai_finding,
            "aiConfidence": existing.ai_confidence,

            "aiDetections": (
                json.loads(existing.ai_detections)
                if existing.ai_detections
                else []
            ),

            "aiVisionFindings": existing.ai_vision_findings,
            "aiVisionScore": existing.ai_vision_score,
        }

    # --------------------------------------------------------
    # AI Risk Analysis
    # --------------------------------------------------------

    ai_result = analyze_violation(
        violation_type=payload.violationType,
        severity=payload.severity,
        description=payload.description,
        observed_condition=payload.observedCondition,

        has_photo=bool(payload.photoUri),
        has_video=bool(payload.videoUri),
        has_voice=bool(payload.voiceUri),

        has_gps=(
            payload.latitude is not None
            and payload.longitude is not None
        ),
    )

    # --------------------------------------------------------
    # Create database object
    # --------------------------------------------------------

    violation = Violation(

        id=f"VIO-{uuid.uuid4().hex[:8].upper()}",

        local_id=payload.localId,

        violation_type=payload.violationType,
        description=payload.description,
        observed_condition=payload.observedCondition,
        severity=payload.severity.upper(),

        latitude=payload.latitude,
        longitude=payload.longitude,

        photo_uri=payload.photoUri,
        video_uri=payload.videoUri,
        voice_uri=payload.voiceUri,

        status="SUBMITTED",

        assigned_to=None,
        assigned_at=None,
        sla_deadline=None,

        # AI Risk
        ai_risk_score=ai_result["riskScore"],
        ai_risk_level=ai_result["riskLevel"],
        ai_finding=ai_result["finding"],
        ai_confidence=ai_result["confidence"],

        # AI Vision
        ai_detections=None,
        ai_vision_findings=None,
        ai_vision_score=None,

        created_at=payload.createdAt,
        updated_at=int(time.time() * 1000),
    )

    db.add(violation)
    db.commit()
    db.refresh(violation)

    return {
        "id": violation.id,
        "message": "Violation created successfully",

        "aiRiskScore": violation.ai_risk_score,
        "aiRiskLevel": violation.ai_risk_level,
        "aiFinding": violation.ai_finding,
        "aiConfidence": violation.ai_confidence,

        "aiDetections": [],
        "aiVisionFindings": None,
        "aiVisionScore": None,
    }


# ============================================================
# GET ALL VIOLATIONS
# ============================================================

@router.get("")
def get_violations(
    db: Session = Depends(get_db),
):

    violations = (
        db.query(Violation)
        .order_by(Violation.created_at.desc())
        .all()
    )

    return {
        "count": len(violations),
        "violations": [
            violation_to_dict(v)
            for v in violations
        ],
    }


# ============================================================
# GET SINGLE VIOLATION
# ============================================================

@router.get("/{violation_id}")
def get_violation(
    violation_id: str,
    db: Session = Depends(get_db),
):

    violation = (
        db.query(Violation)
        .filter(Violation.id == violation_id)
        .first()
    )

    if not violation:
        raise HTTPException(
            status_code=404,
            detail="Violation not found",
        )

    return violation_to_dict(violation)


# ============================================================
# RULE-BASED AI ANALYSIS
# ============================================================

@router.post("/{violation_id}/analyze")
def analyze_existing_violation(
    violation_id: str,
    db: Session = Depends(get_db),
):

    violation = (
        db.query(Violation)
        .filter(Violation.id == violation_id)
        .first()
    )

    if not violation:
        raise HTTPException(
            status_code=404,
            detail="Violation not found",
        )

    ai_result = analyze_violation(
        violation_type=violation.violation_type,
        severity=violation.severity,
        description=violation.description,
        observed_condition=violation.observed_condition,

        has_photo=bool(violation.photo_uri),
        has_video=bool(violation.video_uri),
        has_voice=bool(violation.voice_uri),

        has_gps=(
            violation.latitude is not None
            and violation.longitude is not None
        ),
    )

    violation.ai_risk_score = ai_result["riskScore"]
    violation.ai_risk_level = ai_result["riskLevel"]
    violation.ai_finding = ai_result["finding"]
    violation.ai_confidence = ai_result["confidence"]

    violation.updated_at = int(time.time() * 1000)

    db.commit()
    db.refresh(violation)

    return {
        "success": True,
        "violationId": violation.id,

        "aiRiskScore": violation.ai_risk_score,
        "aiRiskLevel": violation.ai_risk_level,
        "aiFinding": violation.ai_finding,
        "aiConfidence": violation.ai_confidence,
    }


# ============================================================
# YOLO AI VISION ANALYSIS
# ============================================================

@router.post("/{violation_id}/vision-analyze")
async def vision_analyze_violation(
    violation_id: str,
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
):

    # --------------------------------------------------------
    # Find violation
    # --------------------------------------------------------

    violation = (
        db.query(Violation)
        .filter(Violation.id == violation_id)
        .first()
    )

    if not violation:
        raise HTTPException(
            status_code=404,
            detail="Violation not found",
        )

    # --------------------------------------------------------
    # Validate image
    # --------------------------------------------------------

    allowed_types = {
        "image/jpeg",
        "image/png",
        "image/webp",
    }

    if file.content_type not in allowed_types:

        raise HTTPException(
            status_code=400,
            detail="Only JPEG, PNG and WebP images are supported.",
        )

    image_bytes = await file.read()

    if not image_bytes:

        raise HTTPException(
            status_code=400,
            detail="Uploaded image is empty.",
        )

    # --------------------------------------------------------
    # Call YOLO AI Service
    # --------------------------------------------------------

    try:

        result = await analyze_image(
            image_bytes,
            file.filename or "evidence.jpg",
        )

    except Exception as exc:

        raise HTTPException(
            status_code=502,
            detail=f"AI Vision Service unavailable: {str(exc)}",
        )

    # --------------------------------------------------------
    # Extract YOLO result
    # --------------------------------------------------------

    vision_score = int(
        result.get("riskScore", 0)
    )

    vision_level = result.get(
        "riskLevel",
        "LOW",
    )

    detections = result.get(
        "detections",
        [],
    )

    vision_violations = result.get(
        "violations",
        [],
    )

    # --------------------------------------------------------
    # Combine existing Risk Engine + YOLO
    # --------------------------------------------------------

    existing_score = (
        violation.ai_risk_score
        if violation.ai_risk_score is not None
        else 0
    )

    combined_score = max(
        existing_score,
        vision_score,
    )

    if combined_score >= 80:

        combined_level = "CRITICAL"

    elif combined_score >= 60:

        combined_level = "HIGH"

    elif combined_score >= 35:

        combined_level = "MEDIUM"

    else:

        combined_level = "LOW"

    # --------------------------------------------------------
    # Generate vision finding
    # --------------------------------------------------------

    vision_finding = build_vision_finding(
        result
    )

    # --------------------------------------------------------
    # Store YOLO detections
    # --------------------------------------------------------

    violation.ai_detections = serialize_detections(
        result
    )

    violation.ai_vision_findings = (
        vision_finding
    )

    violation.ai_vision_score = (
        vision_score
    )

    # --------------------------------------------------------
    # Update combined AI risk
    # --------------------------------------------------------

    violation.ai_risk_score = (
        combined_score
    )

    violation.ai_risk_level = (
        combined_level
    )

    # --------------------------------------------------------
    # Combine findings
    # --------------------------------------------------------

    if vision_finding:

        existing_finding = (
            violation.ai_finding or ""
        )

        if existing_finding:

            violation.ai_finding = (
                existing_finding
                + " "
                + vision_finding
            )

        else:

            violation.ai_finding = (
                vision_finding
            )

    # --------------------------------------------------------
    # Update timestamp
    # --------------------------------------------------------

    violation.updated_at = int(
        time.time() * 1000
    )

    db.commit()
    db.refresh(violation)

    # --------------------------------------------------------
    # Response
    # --------------------------------------------------------

    return {

        "success": True,

        "violationId": violation.id,

        "vision": {

            "riskScore": vision_score,

            "riskLevel": vision_level,

            "violations": vision_violations,

            "detections": detections,
        },

        "combinedRisk": {

            "score": violation.ai_risk_score,

            "level": violation.ai_risk_level,
        },

        "finding": violation.ai_finding,

    }


# ============================================================
# ASSIGN VIOLATION
# ============================================================

@router.patch("/{violation_id}/assign")
def assign_violation(
    violation_id: str,
    payload: AssignmentRequest,
    db: Session = Depends(get_db),
):

    violation = (
        db.query(Violation)
        .filter(Violation.id == violation_id)
        .first()
    )

    if not violation:
        raise HTTPException(
            status_code=404,
            detail="Violation not found",
        )

    if not payload.assignedTo.strip():

        raise HTTPException(
            status_code=400,
            detail="assignedTo cannot be empty",
        )

    now = int(time.time() * 1000)

    violation.assigned_to = (
        payload.assignedTo.strip()
    )

    violation.assigned_at = now

    # --------------------------------------------------------
    # SLA
    # --------------------------------------------------------
    # Severity-based SLA window
    # CRITICAL = 4 hours
    # HIGH     = 8 hours
    # MEDIUM   = 24 hours
    # LOW      = 48 hours
    # --------------------------------------------------------

    sla_hours = {

        "CRITICAL": 4,
        "HIGH": 8,
        "MEDIUM": 24,
        "LOW": 48,

    }

    hours = sla_hours.get(
        violation.severity.upper(),
        24,
    )

    violation.sla_deadline = (
        now + hours * 60 * 60 * 1000
    )

    if violation.status == "SUBMITTED":

        violation.status = "ASSIGNED"

    violation.updated_at = now

    db.commit()
    db.refresh(violation)

    return violation_to_dict(
        violation
    )


# ============================================================
# UPDATE STATUS
# ============================================================

@router.patch("/{violation_id}/status")
def update_violation_status(
    violation_id: str,
    payload: StatusUpdateRequest,
    db: Session = Depends(get_db),
):

    violation = (
        db.query(Violation)
        .filter(Violation.id == violation_id)
        .first()
    )

    if not violation:
        raise HTTPException(
            status_code=404,
            detail="Violation not found",
        )

    new_status = payload.status.upper()

    if new_status not in ALLOWED_STATUSES:

        raise HTTPException(
            status_code=400,
            detail=(
                "Invalid status. Allowed values: "
                + ", ".join(sorted(ALLOWED_STATUSES))
            ),
        )

    violation.status = new_status

    violation.updated_at = int(
        time.time() * 1000
    )

    db.commit()
    db.refresh(violation)

    return violation_to_dict(
        violation
    )