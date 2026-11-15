from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_scan_rejects_non_image():
    response = client.post(
        "/api/scan-voucher",
        files={"file": ("receipt.txt", b"not an image", "text/plain")},
    )
    assert response.status_code == 400
