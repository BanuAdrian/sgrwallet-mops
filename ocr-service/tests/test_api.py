from fastapi.testclient import TestClient

from app import main
from app.main import app


client = TestClient(app)


def test_scan_rejects_non_image():
    response = client.post(
        "/api/scan-voucher",
        files={"file": ("receipt.txt", b"not an image", "text/plain")},
    )
    assert response.status_code == 400


def test_scan_returns_extracted_voucher_fields(monkeypatch):
    monkeypatch.setattr(
        main,
        "extract_voucher",
        lambda _: {
            "barcode_number": "98000020015000551623",
            "store_name": "Carrefour",
            "voucher_value": 1.5,
            "print_date": "2025-05-10",
            "raw_text": "receipt text",
        },
    )

    response = client.post(
        "/api/scan-voucher",
        files={"file": ("receipt.jpg", b"image bytes", "image/jpeg")},
    )

    assert response.status_code == 200
    assert response.json()["barcode_number"] == "98000020015000551623"
    assert response.json()["store_name"] == "Carrefour"
    assert response.json()["voucher_value"] == 1.5
    assert response.json()["print_date"] == "2025-05-10"
