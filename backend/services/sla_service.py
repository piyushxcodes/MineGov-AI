import time


def get_sla_status(sla_deadline: int | None) -> str:
    if sla_deadline is None:
        return "NOT_ASSIGNED"

    current_time = int(time.time() * 1000)
    remaining_time = sla_deadline - current_time

    if remaining_time <= 0:
        return "BREACHED"

    if remaining_time <= 6 * 60 * 60 * 1000:
        return "WARNING"

    return "ON_TRACK"


def get_remaining_sla_ms(sla_deadline: int | None) -> int | None:
    if sla_deadline is None:
        return None

    current_time = int(time.time() * 1000)

    return max(
        0,
        sla_deadline - current_time
    )