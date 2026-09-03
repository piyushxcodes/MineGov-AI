import json
import httpx


AI_SERVICE_URL = "http://127.0.0.1:8100"


async def analyze_image(image_bytes: bytes, filename: str):

    files = {
        "file": (
            filename,
            image_bytes,
            "application/octet-stream",
        )
    }

    async with httpx.AsyncClient(timeout=120.0) as client:

        response = await client.post(
            f"{AI_SERVICE_URL}/analyze",
            files=files,
        )

        response.raise_for_status()

        return response.json()


def build_vision_finding(result: dict) -> str:

    violations = result.get("violations", [])

    if not violations:
        return "No PPE safety violation detected by the AI vision model."

    findings = []

    for violation in violations:

        violation_type = violation.get("type", "Unknown")
        confidence = violation.get("confidence", 0)

        findings.append(
            f"{violation_type} detected "
            f"with {round(confidence * 100)}% confidence"
        )

    return "; ".join(findings)


def serialize_detections(result: dict) -> str:

    return json.dumps(
        result.get("detections", []),
        separators=(",", ":"),
    )