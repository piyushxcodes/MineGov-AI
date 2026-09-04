from sqlalchemy import Column, String, Text, Float, Integer, BigInteger

from database.connection import Base


class Violation(Base):
    __tablename__ = "violations"

    id = Column(String, primary_key=True)

    local_id = Column(String, unique=True, nullable=False)

    violation_type = Column(String, nullable=False)

    description = Column(Text, nullable=False)

    observed_condition = Column(Text, nullable=True)

    severity = Column(String, nullable=False)

    latitude = Column(Float, nullable=True)

    longitude = Column(Float, nullable=True)

    photo_uri = Column(Text, nullable=True)

    video_uri = Column(Text, nullable=True)

    voice_uri = Column(Text, nullable=True)

    voice_transcript = Column(Text, nullable=True)
   
    voice_language = Column(String(20), nullable=True)

    # =========================================================
    # DOCUMENT OCR
    # =========================================================

    document_uri = Column(Text, nullable=True)

    ocr_text = Column(Text, nullable=True)

    # =========================================================
    # WORKFLOW
    # =========================================================

    status = Column(String, default="SUBMITTED")

    assigned_to = Column(String, nullable=True)

    assigned_at = Column(BigInteger, nullable=True)

    sla_deadline = Column(BigInteger, nullable=True)

    # =========================================================
    # RULE-BASED AI
    # =========================================================

    ai_risk_score = Column(Integer, nullable=True)

    ai_risk_level = Column(String, nullable=True)

    ai_finding = Column(Text, nullable=True)

    ai_confidence = Column(Float, nullable=True)

    # =========================================================
    # AI VISION / YOLO
    # =========================================================

    ai_detections = Column(Text, nullable=True)

    ai_vision_findings = Column(Text, nullable=True)

    ai_vision_score = Column(Integer, nullable=True)

    # =========================================================
    # TIMESTAMPS
    # =========================================================

    created_at = Column(BigInteger, nullable=False)

    updated_at = Column(BigInteger, nullable=False)