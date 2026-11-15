import json
import os
from datetime import date
from pathlib import Path
from typing import Optional

from pydantic import BaseModel


class VisionVoucher(BaseModel):
    barcode_number: Optional[str] = None
    store_name: Optional[str] = None
    voucher_value: Optional[float] = None
    print_date: Optional[date] = None


def extract_with_vision(image_path: str) -> Optional[dict]:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return None

    from google import genai
    from google.genai import types

    client = genai.Client(api_key=api_key)
    image_bytes = Path(image_path).read_bytes()
    prompt = (
        "Read this Romanian SGR return voucher. Extract only the voucher's own "
        "barcode, store name, total voucher value in RON, and issue date. "
        "Do not use receipt totals from unrelated purchases. Return JSON only."
    )
    response = client.models.generate_content(
        model=os.getenv("OCR_VISION_MODEL", "gemini-2.5-flash"),
        contents=[
            types.Part.from_bytes(data=image_bytes, mime_type="image/jpeg"),
            prompt,
        ],
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=VisionVoucher,
        ),
    )
    return VisionVoucher.model_validate(json.loads(response.text)).model_dump()
