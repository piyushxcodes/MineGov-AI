from pydantic import BaseModel, Field


class AssignmentRequest(BaseModel):
    assigned_to: str = Field(..., min_length=2, max_length=100)