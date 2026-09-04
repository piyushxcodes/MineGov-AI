from pathlib import Path

from huggingface_hub import hf_hub_download
from ultralytics import YOLO


# ============================================================
# MODEL CONFIGURATION
# ============================================================

MODEL_REPO = "baskarmother/yolov8-ppe-construction"
MODEL_FILE = "best.pt"

MODEL_DIR = (
    Path(__file__).resolve().parent / "models"
)

MODEL_DIR.mkdir(exist_ok=True)

MODEL_PATH = MODEL_DIR / MODEL_FILE


# ============================================================
# MODEL LOADING
# ============================================================

def load_model():
    """
    Download the YOLO model from Hugging Face
    if it does not already exist locally.
    """

    if not MODEL_PATH.exists():

        print("Downloading MineGov PPE model...")

        downloaded_path = hf_hub_download(
            repo_id=MODEL_REPO,
            filename=MODEL_FILE,
        )

        downloaded_path = Path(downloaded_path)

        MODEL_PATH.write_bytes(
            downloaded_path.read_bytes()
        )

        print(
            f"Model downloaded to: {MODEL_PATH}"
        )

    print(
        f"Loading PPE model: {MODEL_PATH}"
    )

    return YOLO(str(MODEL_PATH))


model = load_model()


# ============================================================
# PPE / SAFETY VIOLATION CLASSES
# ============================================================

VIOLATION_CLASSES = {
    "no-hardhat",
    "no-hard hat",
    "no hardhat",
    "no hard hat",

    "no-mask",
    "no mask",

    "no-safety vest",
    "no safety vest",
    "no-safety-vest",
    "no safety-vest",
}


def normalize_class_name(name: str) -> str:
    """
    Normalize YOLO class names so minor naming
    differences do not break violation detection.
    """

    return (
        name
        .strip()
        .lower()
        .replace("_", " ")
    )


def is_violation_class(name: str) -> bool:
    """
    Check whether a detected YOLO class
    represents a PPE/safety violation.
    """

    normalized = normalize_class_name(name)

    normalized_variants = {
        normalize_class_name(item)
        for item in VIOLATION_CLASSES
    }

    return normalized in normalized_variants


# ============================================================
# RISK CALCULATION
# ============================================================

def calculate_risk(violations: list) -> tuple[int, str]:
    """
    Convert detected PPE violations into a
    MineGov AI risk score.
    """

    risk_score = 0

    for violation in violations:

        confidence = float(
            violation.get("confidence", 0)
        )

        if confidence >= 0.85:
            risk_score += 35

        elif confidence >= 0.65:
            risk_score += 25

        else:
            risk_score += 15

    risk_score = min(
        risk_score,
        100,
    )

    if risk_score >= 70:
        risk_level = "CRITICAL"

    elif risk_score >= 40:
        risk_level = "HIGH"

    elif risk_score > 0:
        risk_level = "MEDIUM"

    else:
        risk_level = "LOW"

    return risk_score, risk_level


# ============================================================
# HUMAN-READABLE AI FINDING
# ============================================================

def build_finding(violations: list) -> str:
    """
    Generate a readable explanation for the
    dashboard / governance layer.
    """

    if not violations:

        return (
            "No PPE safety violation detected "
            "by the AI vision model."
        )

    findings = []

    for violation in violations:

        violation_type = violation.get(
            "type",
            "Unknown violation",
        )

        confidence = float(
            violation.get(
                "confidence",
                0,
            )
        )

        confidence_percent = round(
            confidence * 100
        )

        findings.append(
            f"{violation_type} detected "
            f"with {confidence_percent}% confidence"
        )

    return "; ".join(findings)


# ============================================================
# IMAGE ANALYSIS
# ============================================================

def analyze_image(image_path: str) -> dict:

    results = model.predict(
        source=image_path,
        conf=0.35,
        imgsz=640,
        verbose=False,
    )

    if not results:

        return {
            "detections": [],
            "violations": [],
            "riskScore": 0,
            "riskLevel": "LOW",
            "finding": (
                "No detection result was returned "
                "by the AI vision model."
            ),
        }

    result = results[0]

    detections = []
    violations = []

    # --------------------------------------------------------
    # NO DETECTIONS
    # --------------------------------------------------------

    if result.boxes is None:

        return {
            "detections": [],
            "violations": [],
            "riskScore": 0,
            "riskLevel": "LOW",
            "finding": (
                "No PPE safety violation detected "
                "by the AI vision model."
            ),
        }

    # --------------------------------------------------------
    # PROCESS YOLO DETECTIONS
    # --------------------------------------------------------

    for box in result.boxes:

        class_id = int(
            box.cls[0]
        )

        confidence = float(
            box.conf[0]
        )

        class_name = result.names.get(
            class_id,
            str(class_id),
        )

        coordinates = (
            box.xyxy[0]
            .tolist()
        )

        detection = {
            "class": class_name,

            "confidence": round(
                confidence,
                3,
            ),

            "box": [
                round(
                    float(coordinates[0]),
                    2,
                ),
                round(
                    float(coordinates[1]),
                    2,
                ),
                round(
                    float(coordinates[2]),
                    2,
                ),
                round(
                    float(coordinates[3]),
                    2,
                ),
            ],
        }

        detections.append(
            detection
        )

        # ----------------------------------------------------
        # PPE VIOLATION
        # ----------------------------------------------------

        if is_violation_class(
            class_name
        ):

            violations.append(
                {
                    "type": class_name,

                    "confidence": round(
                        confidence,
                        3,
                    ),
                }
            )

    # --------------------------------------------------------
    # SORT BY CONFIDENCE
    # --------------------------------------------------------

    detections.sort(
        key=lambda item: item["confidence"],
        reverse=True,
    )

    violations.sort(
        key=lambda item: item["confidence"],
        reverse=True,
    )

    # --------------------------------------------------------
    # RISK
    # --------------------------------------------------------

    risk_score, risk_level = (
        calculate_risk(
            violations
        )
    )

    # --------------------------------------------------------
    # FINDING
    # --------------------------------------------------------

    finding = build_finding(
        violations
    )

    # --------------------------------------------------------
    # FINAL RESPONSE
    # --------------------------------------------------------

    return {
        "detections": detections,

        "violations": violations,

        "riskScore": risk_score,

        "riskLevel": risk_level,

        "finding": finding,
    }