from sqlalchemy import Column, String, Text, BigInteger

from database.connection import Base


class AuditLog(Base):
    __tablename__ = "audit_logs"

    id = Column(String, primary_key=True)

    violation_id = Column(String, nullable=True)

    action = Column(String, nullable=False)

    actor = Column(String, nullable=True)

    old_status = Column(String, nullable=True)
    new_status = Column(String, nullable=True)

    details = Column(Text, nullable=True)

    timestamp = Column(BigInteger, nullable=False)

    previous_hash = Column(String, nullable=True)
    current_hash = Column(String, nullable=False)