from pathlib import Path
import shutil
import uuid

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from detector import analyze_image


BASE_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = BASE_DIR / "outputs"

OUTPUT_DIR.mkdir(exist_ok=True)


app = FastAPI(
    title="MineGov AI Vision Service",
    description="YOLO-based PPE and mining safety detection service",
    version="1.0.0"
)


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def root():
    return {
        "service": "MineGov AI Vision Service",
        "status": "online"
    }


@app.get("/health")
def health():
    return {
        "status": "healthy"
    }


@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):

    allowed_types = {
        "image/jpeg",
        "image/png",
        "image/webp"
    }

    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail="Only JPEG, PNG and WebP images are supported."
        )

    extension = Path(file.filename or "").suffix.lower()

    if extension not in {".jpg", ".jpeg", ".png", ".webp"}:
        extension = ".jpg"

    filename = f"{uuid.uuid4()}{extension}"

    image_path = OUTPUT_DIR / filename

    with image_path.open("wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    try:

        result = analyze_image(str(image_path))

        return {
            "success": True,
            "filename": filename,
            **result
        }

    except Exception as exc:

        raise HTTPException(
            status_code=500,
            detail=f"AI inference failed: {str(exc)}"
        )

    finally:

        if image_path.exists():
            image_path.unlink()