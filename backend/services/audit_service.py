import hashlib
import json
import time
import uuid

from sqlalchemy.orm import Session

from database.audit import AuditLog


def create_audit_log(
    db: Session,
    action: str,
    violation_id: str | None = None,
    actor: str | None = None,
    old_status: str | None = None,
    new_status: str | None = None,
    details: dict | None = None,
):
    last_log = (
        db.query(AuditLog)
        .order_by(AuditLog.timestamp.desc())
        .first()
    )

    previous_hash = last_log.current_hash if last_log else ""

    timestamp = int(time.time() * 1000)

    payload = {
        "violation_id": violation_id,
        "action": action,
        "actor": actor,
        "old_status": old_status,
        "new_status": new_status,
        "details": details or {},
        "timestamp": timestamp,
        "previous_hash": previous_hash,
    }

    current_hash = hashlib.sha256(
        json.dumps(
            payload,
            sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")
    ).hexdigest()

    audit_log = AuditLog(
        id=str(uuid.uuid4()),
        violation_id=violation_id,
        action=action,
        actor=actor,
        old_status=old_status,
        new_status=new_status,
        details=json.dumps(details or {}),
        timestamp=timestamp,
        previous_hash=previous_hash,
        current_hash=current_hash,
    )

    db.add(audit_log)
    return audit_log