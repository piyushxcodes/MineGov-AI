def analyze_violation(
    violation_type: str,
    severity: str,
    description: str,
    observed_condition: str,
    has_photo: bool,
    has_video: bool,
    has_voice: bool,
    has_gps: bool,
):
    """
    Rule-based AI risk engine for the MVP.

    This is the governance risk layer.
    Computer vision models such as YOLO will be
    connected separately to provide visual findings.
    """

    score = 0

    severity_scores = {
        "LOW": 20,
        "MEDIUM": 45,
        "HIGH": 70,
        "CRITICAL": 90,
    }

    normalized_severity = severity.upper()

    score += severity_scores.get(
        normalized_severity,
        30
    )

    text = (
        f"{violation_type} "
        f"{description} "
        f"{observed_condition}"
    ).lower()

    high_risk_keywords = [
        "fire",
        "explosion",
        "collapse",
        "gas leak",
        "toxic",
        "fatal",
        "death",
        "unsafe blasting",
        "unauthorized extraction",
        "illegal mining",
        "landslide",
        "electrical hazard",
        "electrocution",
        "injury",
    ]

    detected_keywords = [
        keyword
        for keyword in high_risk_keywords
        if keyword in text
    ]

    if detected_keywords:
        score += min(
            len(detected_keywords) * 5,
            15
        )

    # Evidence strengthens confidence,
    # not necessarily the underlying risk.
    evidence_count = sum([
        has_photo,
        has_video,
        has_voice,
    ])

    if evidence_count >= 2:
        score += 3
    elif evidence_count == 1:
        score += 1

    # GPS improves governance traceability.
    if has_gps:
        score += 2

    score = min(score, 100)

    if score >= 80:
        risk_level = "CRITICAL"
    elif score >= 60:
        risk_level = "HIGH"
    elif score >= 35:
        risk_level = "MEDIUM"
    else:
        risk_level = "LOW"

    if detected_keywords:
        finding = (
            "Potential high-risk condition detected: "
            + ", ".join(detected_keywords)
            + "."
        )
    else:
        finding = (
            "Risk assessment generated from "
            "violation severity, description, "
            "observed condition and available evidence."
        )

    # This represents confidence in the risk
    # assessment logic, not model certainty.
    confidence = 0.75

    if evidence_count >= 2:
        confidence += 0.08

    if has_gps:
        confidence += 0.04

    if detected_keywords:
        confidence += 0.05

    confidence = min(confidence, 0.95)

    return {
        "riskScore": score,
        "riskLevel": risk_level,
        "finding": finding,
        "confidence": round(confidence, 2),
    }