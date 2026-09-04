import time


TERMINAL_STATUSES = {
    "RESOLVED",
    "VERIFIED",
    "CLOSED",
}


def check_sla_breach(
    sla_deadline: int | None,
    status: str,
) -> bool:
    """
    Returns True when an active violation has crossed its SLA deadline.
    """
    if sla_deadline is None:
        return False

    if status in TERMINAL_STATUSES:
        return False

    current_time = int(time.time() * 1000)

    return current_time >= sla_deadline


def get_escalation_status(
    sla_deadline: int | None,
    status: str,
) -> str:
    """
    Returns the current escalation state.
    """

    if sla_deadline is None:
        return "NOT_ASSIGNED"

    if status in TERMINAL_STATUSES:
        return "COMPLETED"

    current_time = int(time.time() * 1000)

    if current_time >= sla_deadline:
        return "BREACHED"

    remaining = sla_deadline - current_time

    # Warning when less than 6 hours remain
    if remaining <= 6 * 60 * 60 * 1000:
        return "WARNING"

    return "ON_TRACK"


def should_escalate(
    sla_deadline: int | None,
    status: str,
) -> bool:
    """
    Determines whether an active violation should be escalated.
    """

    return check_sla_breach(
        sla_deadline=sla_deadline,
        status=status,
    )