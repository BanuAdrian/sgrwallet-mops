import os
import logging
import shutil
import tempfile
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware

from .extractor import extract_voucher
from .schemas import VoucherScanResponse

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
app = FastAPI(title="SGRWallet OCR Service", version="0.1.0")
logger = logging.getLogger(__name__)
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("OCR_ALLOWED_ORIGINS", "http://localhost:5173").split(","),
    allow_credentials=False,
    allow_methods=["POST"],
    allow_headers=["*"],
)


@app.post("/api/scan-voucher", response_model=VoucherScanResponse)
async def scan_voucher(file: UploadFile = File(...)):
    logger.info("Scan request received: filename=%s content_type=%s", file.filename, file.content_type)
    if not file.content_type or not file.content_type.startswith("image/"):
        logger.warning("Rejected scan: unsupported content type=%s", file.content_type)
        raise HTTPException(status_code=400, detail="File must be an image")

    suffix = Path(file.filename or "upload.jpg").suffix or ".jpg"
    temporary_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temporary_file:
            shutil.copyfileobj(file.file, temporary_file)
            temporary_path = temporary_file.name
        result = extract_voucher(temporary_path)
        logger.info("Scan request completed: filename=%s", file.filename)
        return result
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
    except Exception as error:
        logger.exception("Scan request failed: filename=%s", file.filename)
        raise HTTPException(status_code=500, detail="OCR processing failed") from error
    finally:
        await file.close()
        if temporary_path:
            Path(temporary_path).unlink(missing_ok=True)
