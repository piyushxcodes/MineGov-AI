from pydantic import BaseModel

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