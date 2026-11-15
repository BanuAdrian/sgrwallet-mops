from datetime import date
from typing import Optional

from pydantic import BaseModel, Field


class VoucherScanResponse(BaseModel):
    barcode_number: Optional[str] = None
    store_name: Optional[str] = None
    voucher_value: Optional[float] = Field(default=None, ge=0)
    print_date: Optional[date] = None
    raw_text: str
