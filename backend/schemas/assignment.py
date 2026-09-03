from pydantic import BaseModel


class AssignmentRequest(BaseModel):
    assignedTo: str