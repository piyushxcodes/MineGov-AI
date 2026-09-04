from typing import Optional, Any
from pydantic import BaseModel, ConfigDict, Field, AliasChoices


class ViolationCreate(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True
    )

    local_id: str = Field(
        validation_alias=AliasChoices("localId", "local_id")
    )

    violation_type: str = Field(
        validation_alias=AliasChoices("violationType", "violation_type")
    )

    description: str

    observed_condition: Optional[str] = Field(
        default=None,
        validation_alias=AliasChoices(
            "observedCondition",
            "observed_condition"
        )
    )

    severity: str

    latitude: Optional[float] = None
    longitude: Optional[float] = None

    photo_uri: Optional[str] = Field(
        default=None,
        validation_alias=AliasChoices("photoUri", "photo_uri")
    )

    video_uri: Optional[str] = Field(
        default=None,
        validation_alias=AliasChoices("videoUri", "video_uri")
    )

    voice_uri: Optional[str] = Field(
        default=None,
        validation_alias=AliasChoices("voiceUri", "voice_uri")
    )

    document_uri: Optional[str] = Field(
        default=None,
        validation_alias=AliasChoices(
            "documentUri",
            "document_uri"
        )
    )

    ocr_text: Optional[str] = Field(
        default=None,
        validation_alias=AliasChoices(
            "ocrText",
            "ocr_text"
        )
    )

    created_at: int = Field(
        validation_alias=AliasChoices(
            "createdAt",
            "created_at"
        )
    )


class ViolationResponse(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True
    )

    id: str
    local_id: str
    violation_type: str
    description: str
    observed_condition: Optional[str] = None
    severity: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    photo_uri: Optional[str] = None
    video_uri: Optional[str] = None
    voice_uri: Optional[str] = None
    document_uri: Optional[str] = None
    ocr_text: Optional[str] = None
    status: Optional[str] = None
    assigned_to: Optional[str] = None
    assigned_at: Optional[int] = None
    sla_deadline: Optional[int] = None
    ai_risk_score: Optional[int] = None
    ai_risk_level: Optional[str] = None
    ai_finding: Optional[str] = None
    ai_confidence: Optional[float] = None
    ai_detections: Optional[Any] = None
    ai_vision_findings: Optional[str] = None
    ai_vision_score: Optional[int] = None
    created_at: int
    updated_at: int