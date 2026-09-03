from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from database.connection import engine
from database.models import Base
from routes.violations import router as violations_router


# Create database tables if they don't already exist
Base.metadata.create_all(bind=engine)


app = FastAPI(
    title="MineGov AI Backend",
    description="Backend API for MineGov AI mining inspection and compliance platform",
    version="1.0.0"
)


# --------------------------------------------------
# CORS
# --------------------------------------------------

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# --------------------------------------------------
# API ROUTES
# --------------------------------------------------

app.include_router(violations_router)


# --------------------------------------------------
# ROOT
# --------------------------------------------------

@app.get("/")
def root():
    return {
        "message": "MineGov AI Backend is running",
        "status": "online"
    }


# --------------------------------------------------
# HEALTH
# --------------------------------------------------

@app.get("/health")
def health_check():
    return {
        "status": "healthy"
    }