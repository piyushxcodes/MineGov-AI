import json
import time
import uuid
from pathlib import Path
import shutil

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.orm import Session

from database.connection import get_db
from database.models import Violation
from database.audit import AuditLog
from schemas.violation import ViolationCreate

from services.whisper_service import transcribe_audio
from services.audit_service import create_audit_log

from services.sla_service import get_sla_status, get_remaining_sla_ms

from services.ai_service import analyze_violation
from services.ai_vision_service import (
    analyze_image,
    build_vision_finding,
    serialize_detections,
)


router = APIRouter(
    prefix="/api/v1/violations",
    tags=["Violations"],
)


# ============================================================
# HELPERS
# ============================================================

def violation_to_dict(violation: Violation):

    detections = []

    if violation.ai_detections:
        try:
            detections = json.loads(violation.ai_detections)
        except Exception:
            detections = []

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

        "voiceTranscript": violation.voice_transcript,
        "voiceLanguage": violation.voice_language,

        # DOCUMENT / OCR
        "documentUri": violation.document_uri,
        "ocrText": violation.ocr_text,



        # WORKFLOW
        "status": violation.status,
        "assignedTo": violation.assigned_to,
        "assignedAt": violation.assigned_at,
        "slaDeadline": violation.sla_deadline,
        "slaStatus": get_sla_status(
        violation.sla_deadline
        ),
        "remainingSlaMs": get_remaining_sla_ms(
            violation.sla_deadline
        ),

        # RULE AI
        "aiRiskScore": violation.ai_risk_score,
        "aiRiskLevel": violation.ai_risk_level,
        "aiFinding": violation.ai_finding,
        "aiConfidence": violation.ai_confidence,

        # VISION AI
        "aiDetections": detections,
        "aiVisionFindings": violation.ai_vision_findings,
        "aiVisionScore": violation.ai_vision_score,

        # TIME
        "createdAt": violation.created_at,
        "updatedAt": violation.updated_at,
    }


# ============================================================
# CREATE VIOLATION
# ============================================================

@router.post("")
async def create_violation(
    request: ViolationCreate,
    db: Session = Depends(get_db),
):

    # --------------------------------------------------------
    # DUPLICATE PROTECTION
    # --------------------------------------------------------

    existing = (
        db.query(Violation)
        .filter(Violation.local_id == request.local_id)
        .first()
    )

    if existing:
        return {
            "id": existing.id,
            "message": "Violation already exists",
        }

    now = int(time.time() * 1000)
    violation_id = str(uuid.uuid4())

    # --------------------------------------------------------
    # RULE-BASED AI
    # --------------------------------------------------------

    ai_result = analyze_violation(
        violation_type=request.violation_type,
        severity=request.severity,
        description=request.description,
        observed_condition=request.observed_condition or "",
        has_photo=request.photo_uri is not None,
        has_video=request.video_uri is not None,
        has_voice=request.voice_uri is not None,
        has_gps=(
            request.latitude is not None
            and request.longitude is not None
        ),
    )

    violation = Violation(
        id=violation_id,
        local_id=request.local_id,

        violation_type=request.violation_type,
        description=request.description,
        observed_condition=request.observed_condition,
        severity=request.severity,

        latitude=request.latitude,
        longitude=request.longitude,

        photo_uri=request.photo_uri,
        video_uri=request.video_uri,
        voice_uri=request.voice_uri,

        # DOCUMENT / OCR
        document_uri=request.document_uri,
        ocr_text=request.ocr_text,

        # WORKFLOW
        status="SUBMITTED",

        # RULE AI
        ai_risk_score=ai_result.get("riskScore"),
        ai_risk_level=ai_result.get("riskLevel"),
        ai_finding=ai_result.get("finding"),
        ai_confidence=ai_result.get("confidence"),

        created_at=request.created_at,
        updated_at=now,
    )

    db.add(violation)
    db.commit()
    db.refresh(violation)

    create_audit_log(
        db=db,
        action="VIOLATION_CREATED",
        violation_id=violation.id,
        actor="INSPECTOR",
        new_status=violation.status,
        details={
            "violation_type": violation.violation_type,
            "severity": violation.severity,
        },
    )

    db.commit()

    return {
        "id": violation.id,
        "message": "Violation created successfully",
        "riskScore": violation.ai_risk_score,
        "riskLevel": violation.ai_risk_level,
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

    return [
        violation_to_dict(v)
        for v in violations
    ]


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
# RUN RULE AI AGAIN
# ============================================================

@router.post("/{violation_id}/analyze")
async def analyze_existing_violation(
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
        observed_condition=violation.observed_condition or "",
        has_photo=violation.photo_uri is not None,
        has_video=violation.video_uri is not None,
        has_voice=violation.voice_uri is not None,
        has_gps=(
            violation.latitude is not None
            and violation.longitude is not None
        ),
    )

    violation.ai_risk_score = ai_result.get("riskScore")
    violation.ai_risk_level = ai_result.get("riskLevel")
    violation.ai_finding = ai_result.get("finding")
    violation.ai_confidence = ai_result.get("confidence")

    violation.updated_at = int(time.time() * 1000)

    db.commit()
    db.refresh(violation)

    return violation_to_dict(violation)


# ============================================================
# YOLO VISION ANALYSIS
# ============================================================

@router.post("/{violation_id}/vision-analyze")
async def analyze_violation_vision(
    violation_id: str,
    file: UploadFile = File(...),
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

    image_bytes = await file.read()

    if not image_bytes:
        raise HTTPException(
            status_code=400,
            detail="Empty image file",
        )

    try:
        result = await analyze_image(
            image_bytes=image_bytes,
            filename=file.filename or "image.jpg",
        )

    except Exception as error:
        raise HTTPException(
            status_code=502,
            detail=f"Vision AI service failed: {error}",
        )

    # --------------------------------------------------------
    # SAVE YOLO RESULTS
    # --------------------------------------------------------

    violation.ai_detections = serialize_detections(result)

    violation.ai_vision_findings = build_vision_finding(result)

    vision_score = result.get("riskScore")

    violation.ai_vision_score = vision_score

    # --------------------------------------------------------
    # COMBINED RISK
    # --------------------------------------------------------

    rule_score = (
        violation.ai_risk_score
        if violation.ai_risk_score is not None
        else 0
    )

    if vision_score is not None:

        combined_score = max(
            rule_score,
            int(vision_score),
        )

        if combined_score >= 80:
            combined_level = "CRITICAL"
        elif combined_score >= 60:
            combined_level = "HIGH"
        elif combined_score >= 30:
            combined_level = "MEDIUM"
        else:
            combined_level = "LOW"

        violation.ai_risk_score = combined_score
        violation.ai_risk_level = combined_level

    violation.updated_at = int(time.time() * 1000)

    db.commit()
    db.refresh(violation)

    return {
        "success": True,
        "violationId": violation.id,
        "vision": result,
        "combinedRisk": {
            "score": violation.ai_risk_score,
            "level": violation.ai_risk_level,
        },
        "finding": violation.ai_vision_findings,
    }


# ============================================================
# DOCUMENT UPLOAD
# ============================================================

@router.post("/{violation_id}/document-upload")
async def upload_document(
    violation_id: str,
    file: UploadFile = File(...),
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

    document_bytes = await file.read()

    if not document_bytes:
        raise HTTPException(
            status_code=400,
            detail="Empty document file",
        )

    storage_dir = Path("storage/evidence")
    storage_dir.mkdir(
        parents=True,
        exist_ok=True,
    )

    extension = Path(
        file.filename or ""
    ).suffix.lower()

    if not extension:
        extension = ".jpg"

    filename = f"{uuid.uuid4()}{extension}"

    file_path = storage_dir / filename

    try:

        with file_path.open("wb") as buffer:
            buffer.write(document_bytes)

        document_uri = f"/evidence/{filename}"

        violation.document_uri = document_uri
        violation.updated_at = int(
            time.time() * 1000
        )

        db.commit()
        db.refresh(violation)

        return {
            "success": True,
            "violationId": violation.id,
            "documentUri": document_uri,
            "filename": filename,
        }

    except Exception as error:

        if file_path.exists():
            file_path.unlink()

        db.rollback()

        raise HTTPException(
            status_code=500,
            detail=f"Document upload failed: {error}",
        )


# ============================================================
# ASSIGN VIOLATION
# ============================================================

@router.patch("/{violation_id}/assign")
def assign_violation(
    violation_id: str,
    assigned_to: str,
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

    now = int(time.time() * 1000)

    sla_hours = {
        "CRITICAL": 4,
        "HIGH": 8,
        "MEDIUM": 24,
        "LOW": 48,
    }

    hours = sla_hours.get(
        violation.severity.upper(),
        48,
    )

    violation.assigned_to = assigned_to
    violation.assigned_at = now

    violation.sla_deadline = (
        now + hours * 60 * 60 * 1000
    )

    violation.status = "ASSIGNED"
    violation.updated_at = now

    db.commit()
    db.refresh(violation)

    create_audit_log(
        db=db,
        action="ASSIGNED",
        violation_id=violation.id,
        actor="SUPERVISOR",
        new_status=violation.status,
        details={
            "assigned_to": violation.assigned_to,
            "sla_deadline": violation.sla_deadline,
        },
    )

    db.commit()

    return violation_to_dict(violation)


# ============================================================
# UPDATE STATUS
# ============================================================

@router.get("/{violation_id}/audit")
def get_violation_audit(
    violation_id: str,
    db: Session = Depends(get_db),
):
    violation = db.query(Violation).filter(Violation.id == violation_id).first()
    if not violation:
        raise HTTPException(status_code=404, detail="Violation not found")
    logs = (
        db.query(AuditLog)
        .filter(AuditLog.violation_id == violation_id)
        .order_by(AuditLog.timestamp.asc())
        .all()
    )
    return [
        {
            "id": log.id,
            "violationId": log.violation_id,
            "action": log.action,
            "actor": log.actor,
            "oldStatus": log.old_status,
            "newStatus": log.new_status,
            "details": log.details,
            "timestamp": log.timestamp,
            "previousHash": log.previous_hash,
            "currentHash": log.current_hash,
        }
        for log in logs
    ]


@router.patch("/{violation_id}/status")
def update_status(
    violation_id: str,
    status: str,
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

    allowed_statuses = {
        "SUBMITTED",
        "ASSIGNED",
        "IN_PROGRESS",
        "RESOLVED",
        "VERIFIED",
        "CLOSED",
        "ESCALATED",
    }

    status = status.upper()

    if status not in allowed_statuses:
        raise HTTPException(
            status_code=400,
            detail="Invalid status",
        )

    old_status = violation.status

    violation.status = status
    violation.updated_at = int(
        time.time() * 1000
    )

    db.commit()
    db.refresh(violation)

    create_audit_log(
        db=db,
        action="STATUS_CHANGED",
        violation_id=violation.id,
        actor="SUPERVISOR",
        old_status=old_status,
        new_status=violation.status,
        details={
            "status_change": f"{old_status} -> {violation.status}",
        },
    )

    db.commit()

    return violation_to_dict(violation)


@router.patch("/{violation_id}/verify")
def verify_violation(
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

    if violation.status != "RESOLVED":
        raise HTTPException(
            status_code=400,
            detail="Only RESOLVED violations can be verified",
        )

    now = int(time.time() * 1000)
    old_status = violation.status

    violation.status = "VERIFIED"
    violation.updated_at = now

    db.commit()
    db.refresh(violation)

    create_audit_log(
        db=db,
        action="VERIFIED",
        violation_id=violation.id,
        actor="INSPECTOR",
        old_status=old_status,
        new_status="VERIFIED",
        details={
            "verification": "Inspector verification completed",
        },
    )

    db.commit()

    return violation_to_dict(violation)

@router.post("/{violation_id}/voice-transcribe")
async def transcribe_violation_voice(
    violation_id: str,
    file: UploadFile = File(...),
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
            detail="Violation not found"
        )

    audio_bytes = await file.read()

    if not audio_bytes:
        raise HTTPException(
            status_code=400,
            detail="Empty audio file"
        )

    storage_dir = Path("storage/evidence")
    storage_dir.mkdir(parents=True, exist_ok=True)

    extension = Path(file.filename or "").suffix.lower()

    if not extension:
        extension = ".m4a"

    filename = f"{uuid.uuid4()}{extension}"
    file_path = storage_dir / filename

    try:
        with file_path.open("wb") as buffer:
            buffer.write(audio_bytes)

        result = transcribe_audio(str(file_path))

        violation.voice_uri = f"/evidence/{filename}"
        violation.voice_transcript = result["text"]
        violation.voice_language = result["language"]
        violation.updated_at = int(time.time() * 1000)

        db.commit()
        db.refresh(violation)

        return {
            "success": True,
            "violationId": violation.id,
            "voiceUri": violation.voice_uri,
            "transcript": violation.voice_transcript,
            "language": violation.voice_language,
            "languageProbability": result["languageProbability"],
        }

    except Exception as error:
        if file_path.exists():
            file_path.unlink()

        db.rollback()

        raise HTTPException(
            status_code=500,
            detail=f"Voice transcription failed: {error}"
        )