from pathlib import Path

from huggingface_hub import hf_hub_download
from ultralytics import YOLO


MODEL_REPO = "baskarmother/yolov8-ppe-construction"
MODEL_FILE = "best.pt"

MODEL_DIR = Path(__file__).resolve().parent / "models"
MODEL_DIR.mkdir(exist_ok=True)

MODEL_PATH = MODEL_DIR / MODEL_FILE


def load_model():
    if not MODEL_PATH.exists():
        downloaded_path = hf_hub_download(
            repo_id=MODEL_REPO,
            filename=MODEL_FILE
        )

        downloaded_path = Path(downloaded_path)
        MODEL_PATH.write_bytes(downloaded_path.read_bytes())

    print(f"Loading PPE model: {MODEL_PATH}")

    return YOLO(str(MODEL_PATH))


model = load_model()


VIOLATION_CLASSES = {
    "no-hardhat",
    "no-mask",
    "no-safety vest",
}


def analyze_image(image_path: str):

    results = model.predict(
        source=image_path,
        conf=0.35,
        imgsz=640,
        verbose=False
    )

    result = results[0]

    detections = []
    violations = []

    if result.boxes is None:
        return {
            "detections": [],
            "violations": [],
            "riskScore": 0,
            "riskLevel": "LOW"
        }

    for box in result.boxes:

        class_id = int(box.cls[0])
        confidence = float(box.conf[0])

        class_name = result.names[class_id]

        coordinates = box.xyxy[0].tolist()

        detection = {
            "class": class_name,
            "confidence": round(confidence, 3),
            "box": [
                round(float(coordinates[0]), 2),
                round(float(coordinates[1]), 2),
                round(float(coordinates[2]), 2),
                round(float(coordinates[3]), 2),
            ]
        }

        detections.append(detection)

        normalized_name = class_name.lower()

        if normalized_name in VIOLATION_CLASSES:
            violations.append({
                "type": class_name,
                "confidence": round(confidence, 3)
            })

    # Risk calculation
    risk_score = 0

    for violation in violations:

        confidence = violation["confidence"]

        if confidence >= 0.85:
            risk_score += 35
        elif confidence >= 0.65:
            risk_score += 25
        else:
            risk_score += 15

    risk_score = min(risk_score, 100)

    if risk_score >= 70:
        risk_level = "CRITICAL"
    elif risk_score >= 40:
        risk_level = "HIGH"
    elif risk_score > 0:
        risk_level = "MEDIUM"
    else:
        risk_level = "LOW"

    return {
        "detections": detections,
        "violations": violations,
        "riskScore": risk_score,
        "riskLevel": risk_level
    }