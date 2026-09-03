from sqlalchemy import Column, String, Text, Float, BigInteger, Integer
from database.connection import Base


class Violation(Base):
    __tablename__ = "violations"

    id = Column(String, primary_key=True, index=True)
    local_id = Column(String, unique=True, nullable=False, index=True)

    violation_type = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    observed_condition = Column(Text, nullable=False)
    severity = Column(String, nullable=False)

    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)

    photo_uri = Column(Text, nullable=True)
    video_uri = Column(Text, nullable=True)
    voice_uri = Column(Text, nullable=True)

    
    # Workflow
    status = Column(
        String,
        nullable=False,
        default="SUBMITTED"
    )

    # Assignment
    assigned_to = Column(String, nullable=True)
    assigned_at = Column(BigInteger, nullable=True)

    # SLA
    sla_deadline = Column(BigInteger, nullable=True)

    # AI Analysis
    ai_risk_score = Column(Integer, nullable=True)
    ai_risk_level = Column(String, nullable=True)
    ai_finding = Column(Text, nullable=True)
    ai_confidence = Column(Float, nullable=True)

    ai_detections = Column(Text, nullable=True)
    ai_vision_findings = Column(Text, nullable=True)
    ai_vision_score = Column(Integer, nullable=True)


    # Timestamps
    created_at = Column(BigInteger, nullable=False)
    updated_at = Column(BigInteger, nullable=False)