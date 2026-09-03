import time


def check_sla_breach(
    sla_deadline: int | None,
    status: str
) -> bool:
    if sla_deadline is None:
        return False

    if status in {"RESOLVED", "VERIFIED", "CLOSED"}:
        return False

    current_time = int(time.time() * 1000)

    return current_time >= sla_deadline