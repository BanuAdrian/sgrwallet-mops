from app.extractor import parse_amount, parse_date, parse_store


def test_parse_amount_returns_smallest_deposit_value():
    assert parse_amount("Total 12,40 RON\nSGR 0,50 lei") == 0.5


def test_parse_amount_accepts_ocr_misread_currency():
    assert parse_amount("RON 3,50HON") == 3.5


def test_parse_amount_rejects_unrealistic_decimal_noise():
    assert parse_amount("random number 3.08") is None


def test_parse_date_supports_romanian_format():
    assert str(parse_date("Data: 15-11-2026")) == "2026-11-15"


def test_parse_date_supports_text_month():
    assert str(parse_date("13-JUN-2025")) == "2025-06-13"


def test_parse_store_detects_known_store():
    assert parse_store("Magazin: Lidl") == "Lidl"
