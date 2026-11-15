import re
import logging
from collections import Counter
from datetime import date
from typing import Optional

import cv2
import numpy as np

from .vision import extract_with_vision

logger = logging.getLogger(__name__)

DATE_PATTERNS = (
    re.compile(r"\b(?P<day>\d{2})[./-](?P<month>\d{2})[./-](?P<year>\d{4})\b"),
    re.compile(r"\b(?P<year>\d{4})[./-](?P<month>\d{2})[./-](?P<day>\d{2})\b"),
)
TEXT_DATE_PATTERN = re.compile(
    r"\b(?P<day>\d{1,2})[-./ ](?P<month>[A-Za-zĂÂÎȘȚăâîșț]{3,})[-./ ](?P<year>\d{4})\b"
)
MONTHS = {
    "JAN": 1, "IAN": 1, "FEB": 2, "MAR": 3, "APR": 4, "MAI": 5,
    "MAY": 5, "JUN": 6, "IUN": 6, "JUL": 7, "AUG": 8, "SEP": 9,
    "OCT": 10, "NOV": 11, "DEC": 12,
}
DECIMAL_AMOUNT_PATTERN = re.compile(
    r"(?<!\d)(?P<whole>\d{1,3})\s*[,.:]\s*(?P<fraction>\d{1,2})(?!\d)"
)
NUMBER_WITH_CURRENCY_PATTERN = re.compile(
    r"(?<!\d)(?P<amount>\d{1,3}(?:[.,]\d{1,2})?)\s*"
    r"(?:R[O0]N|H[O0]N|LE[I1]|RON|lei)\b",
    re.IGNORECASE,
)
STORE_PATTERN = re.compile(r"(?:magazin|store)\s*[:\-]\s*(?P<store>[A-Za-zĂÂÎȘȚăâîșț0-9 .&'-]{2,40})", re.IGNORECASE)


def preprocess(image):
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    gray = cv2.resize(gray, None, fx=3, fy=3, interpolation=cv2.INTER_CUBIC)
    gray = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)).apply(gray)
    return cv2.adaptiveThreshold(
        gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 11
    )


def _preprocessing_variants(image):
    height, width = image.shape[:2]
    largest_side = max(height, width)
    if largest_side > 1600:
        scale = 1600 / largest_side
        image = cv2.resize(
            image, (int(width * scale), int(height * scale)), interpolation=cv2.INTER_AREA
        )
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    gray = cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)).apply(gray)
    yield cv2.threshold(clahe, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
    yield cv2.threshold(clahe, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)[1]
    yield cv2.adaptiveThreshold(
        clahe, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 11
    )


def parse_date(text: str) -> Optional[date]:
    for pattern in DATE_PATTERNS:
        match = pattern.search(text)
        if not match:
            continue
        try:
            return date(
                int(match.group("year")),
                int(match.group("month")),
                int(match.group("day")),
            )
        except ValueError:
            return None
    text_match = TEXT_DATE_PATTERN.search(text)
    if text_match:
        month = MONTHS.get(text_match.group("month")[:3].upper())
        if month:
            try:
                return date(
                    int(text_match.group("year")),
                    month,
                    int(text_match.group("day")),
                )
            except ValueError:
                return None
    return None


def parse_amount(text: str) -> Optional[float]:
    sgr_amounts = []
    for line in text.splitlines():
        if not re.search(r"\b(?:SGR|PET|STICLA|STICLĂ)\b", line, re.IGNORECASE):
            continue
        currency_matches = list(NUMBER_WITH_CURRENCY_PATTERN.finditer(line))
        if currency_matches:
            for match in currency_matches:
                parsed = float(match.group("amount").replace(",", "."))
                if _valid_voucher_amount(parsed):
                    sgr_amounts.append(parsed)
        else:
            for match in DECIMAL_AMOUNT_PATTERN.finditer(line):
                parsed = float(f"{match.group('whole')}.{match.group('fraction')}")
                if _valid_voucher_amount(parsed):
                    sgr_amounts.append(parsed)
    unique_sgr_amounts = sorted(set(sgr_amounts))
    if unique_sgr_amounts:
        return sum(unique_sgr_amounts)

    contextual_amounts = []
    for line in text.splitlines():
        if re.search(r"R[O0]N|H[O0]N|LE[I1]", line, re.IGNORECASE):
            contextual_amounts.extend(
                float(match.group("amount").replace(",", "."))
                for match in NUMBER_WITH_CURRENCY_PATTERN.finditer(line)
            )
    valid_contextual = [amount for amount in contextual_amounts if _valid_voucher_amount(amount)]
    if valid_contextual:
        return max(valid_contextual)

    currency_amounts = []
    for match in NUMBER_WITH_CURRENCY_PATTERN.finditer(text):
        value = match.group("amount").replace(",", ".")
        parsed = float(value)
        if _valid_voucher_amount(parsed):
            currency_amounts.append(parsed)
    if currency_amounts:
        return max(currency_amounts)

    decimal_amounts = []
    for match in DECIMAL_AMOUNT_PATTERN.finditer(text):
        parsed = float(f"{match.group('whole')}.{match.group('fraction')}")
        if _valid_voucher_amount(parsed):
            decimal_amounts.append(parsed)
    return max(decimal_amounts) if decimal_amounts else None


def _valid_voucher_amount(amount: float) -> bool:
    return 0 < amount <= 100 and abs(amount * 2 - round(amount * 2)) < 1e-6


def parse_store(text: str) -> Optional[str]:
    match = STORE_PATTERN.search(text)
    if match:
        return match.group("store").strip()
    for known_store in ("Lidl", "Kaufland", "Carrefour", "Auchan", "Mega Image"):
        if known_store.lower() in text.lower():
            return known_store
    return None


def extract_voucher(image_path: str) -> dict:
    import pytesseract
    from pyzbar.pyzbar import decode
    from rapidocr_onnxruntime import RapidOCR

    image = cv2.imread(image_path)
    if image is None:
        raise ValueError("The uploaded file is not a readable image")
    logger.info("OCR started: file=%s size=%sx%s", image_path, image.shape[1], image.shape[0])

    rectified = _rectify_receipt(image)
    ocr_images = [rectified] if rectified is not None else [image]
    logger.info("Local OCR mode: rectified_receipt=%s image_variants=%s", rectified is not None, len(ocr_images))
    barcode_number = None
    barcode_images = [image] + ocr_images
    barcode_images.extend(cv2.cvtColor(item, cv2.COLOR_BGR2GRAY) for item in barcode_images.copy())
    for barcode_image in barcode_images:
        barcodes = decode(barcode_image)
        if barcodes:
            barcode_number = barcodes[0].data.decode("utf-8", errors="ignore")
            break
    if barcode_number is None:
        barcode_number = _decode_with_opencv(ocr_images)
    logger.info("Barcode detection result: found=%s", barcode_number is not None)
    if barcode_number is None:
        for angle in (90, 180, 270):
            rotated = cv2.rotate(image, {
                90: cv2.ROTATE_90_CLOCKWISE,
                180: cv2.ROTATE_180,
                270: cv2.ROTATE_90_COUNTERCLOCKWISE,
            }[angle])
            barcodes = decode(rotated)
            if barcodes:
                barcode_number = barcodes[0].data.decode("utf-8", errors="ignore")
                break

    texts = []
    rapid_ocr = RapidOCR()
    for ocr_image in ocr_images:
        logger.info("Starting RapidOCR")
        rapid_result, _ = rapid_ocr(ocr_image)
        if rapid_result:
            texts.append("\n".join(item[1] for item in rapid_result))
        logger.info("RapidOCR completed")
        for variant in _preprocessing_variants(ocr_image):
            logger.info("Starting Tesseract variant")
            text = pytesseract.image_to_string(variant, lang="ron+eng", config="--psm 6")
            if text.strip():
                texts.append(text)
            logger.info("Tesseract variant completed")
    text = max(texts, key=len, default="")
    parsed_amounts = [parse_amount(candidate) for candidate in texts]
    parsed_amounts = [amount for amount in parsed_amounts if amount is not None]
    selected_amount = _select_amount(texts, parsed_amounts)
    store_names = [store for store in (parse_store(candidate) for candidate in texts) if store]
    dates = [parsed for parsed in (parse_date(candidate) for candidate in texts) if parsed]
    logger.info(
        "Local OCR completed: text_variants=%s store=%s amount=%s date=%s",
        len(texts),
        store_names[0] if store_names else None,
        selected_amount,
        dates[0] if dates else None,
    )

    local_result = {
        "barcode_number": barcode_number,
        "store_name": store_names[0] if store_names else None,
        "voucher_value": selected_amount,
        "print_date": dates[0] if dates else None,
        "raw_text": text,
    }
    if _is_complete_result(local_result):
        logger.info("OCR completed with local engine: result=%s", local_result)
        return local_result

    logger.info("Local OCR incomplete; trying Gemini Vision")
    vision_result = extract_with_vision(image_path)
    if vision_result:
        logger.info("OCR completed with Gemini Vision: result=%s", vision_result)
        return {**vision_result, "raw_text": "Extracted with Gemini Vision"}

    logger.info("OCR completed with incomplete local result: result=%s", local_result)
    return local_result


def _is_complete_result(result: dict) -> bool:
    return all(
        result.get(field) not in (None, "")
        for field in ("barcode_number", "store_name", "voucher_value", "print_date")
    )


def _select_amount(texts, parsed_amounts) -> Optional[float]:
    if not parsed_amounts:
        return None
    counts = Counter(parsed_amounts)
    most_common = counts.most_common()
    highest_count = most_common[0][1]
    candidates = [amount for amount, count in most_common if count == highest_count]
    longest_text_amount = parse_amount(max(texts, key=len, default=""))
    if longest_text_amount in candidates:
        return longest_text_amount
    return candidates[0]


def _decode_with_opencv(images) -> Optional[str]:
    detector_factory = getattr(cv2, "barcode_BarcodeDetector", None)
    if detector_factory is None:
        logger.info("OpenCV barcode detector is not available")
        return None
    detector = detector_factory()
    for image in images:
        try:
            result = detector.detectAndDecode(image)
            if isinstance(result, tuple):
                for item in result:
                    if isinstance(item, str) and item:
                        return item
                    if isinstance(item, (list, tuple)) and item:
                        for value in item:
                            if isinstance(value, str) and value:
                                return value
            elif isinstance(result, str) and result:
                return result
        except cv2.error:
            continue
    return None


def _rectify_receipt(image):
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blurred, 50, 150)
    contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    image_area = image.shape[0] * image.shape[1]

    for contour in sorted(contours, key=cv2.contourArea, reverse=True)[:20]:
        perimeter = cv2.arcLength(contour, True)
        polygon = cv2.approxPolyDP(contour, 0.02 * perimeter, True)
        area = cv2.contourArea(polygon)
        if len(polygon) != 4 or area < image_area * 0.08:
            continue

        points = polygon.reshape(4, 2).astype("float32")
        ordered = _order_points(points)
        width = int(max(
            cv2.norm(ordered[1] - ordered[0]),
            cv2.norm(ordered[2] - ordered[3]),
        ))
        height = int(max(
            cv2.norm(ordered[3] - ordered[0]),
            cv2.norm(ordered[2] - ordered[1]),
        ))
        if width < 100 or height < 100:
            continue
        destination = np.array(
            [[0, 0], [width - 1, 0], [width - 1, height - 1], [0, height - 1]],
            dtype="float32",
        )
        matrix = cv2.getPerspectiveTransform(ordered, destination)
        return cv2.warpPerspective(image, matrix, (width, height))
    return None


def _order_points(points):
    ordered = points.copy()
    sums = points.sum(axis=1)
    differences = points[:, 1] - points[:, 0]
    ordered[0] = points[sums.argmin()]
    ordered[2] = points[sums.argmax()]
    ordered[1] = points[differences.argmin()]
    ordered[3] = points[differences.argmax()]
    return ordered
