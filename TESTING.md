# Testing

This document describes the testing approach used for Check-in 2.

## Automated test cases

### Backend (Spring Boot / JUnit)

**[`AuthServiceTest`](src/test/java/ro/mops/sgrwallet/service/AuthServiceTest.java) (5 tests)**

- [Registration normalization, hashing and role assignment](src/test/java/ro/mops/sgrwallet/service/AuthServiceTest.java#L54)
- [Duplicate email rejection](src/test/java/ro/mops/sgrwallet/service/AuthServiceTest.java#L71)
- [JWT creation and claims](src/test/java/ro/mops/sgrwallet/service/AuthServiceTest.java#L84)
- [Invalid password rejection](src/test/java/ro/mops/sgrwallet/service/AuthServiceTest.java#L104)
- [Blocked account rejection](src/test/java/ro/mops/sgrwallet/service/AuthServiceTest.java#L119)

**[`AuthControllerTest`](src/test/java/ro/mops/sgrwallet/controller/AuthControllerTest.java) (2 tests)**

- [Registration returns HTTP 201](src/test/java/ro/mops/sgrwallet/controller/AuthControllerTest.java#L29)
- [Login returns the authentication response](src/test/java/ro/mops/sgrwallet/controller/AuthControllerTest.java#L40)

**[`VoucherServiceTest`](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java) (13 tests)**

- [Valid manual voucher persistence](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L55)
- [Required voucher code validation](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L85)
- [Required store validation](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L104)
- [Positive amount validation](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L123)
- [Missing and impossible calendar dates](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L155)
- [Expired voucher rejection](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L187)
- [Issue/expiry date ordering](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L206)
- [Listing saved user vouchers](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L238)
- [Marking an active voucher as used](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L264)
- [Rejecting repeated use](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L278)
- [Rejecting another user's voucher](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L293)
- [Rejecting a duplicate voucher code](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L308)
- [Rejecting a duplicate code with whitespace or different case](src/test/java/ro/mops/sgrwallet/service/VoucherServiceTest.java#L330)

**[`VoucherControllerTest`](src/test/java/ro/mops/sgrwallet/controller/VoucherControllerTest.java) (4 tests)**

- [Add voucher returns HTTP 201](src/test/java/ro/mops/sgrwallet/controller/VoucherControllerTest.java#L38)
- [List vouchers returns HTTP 200](src/test/java/ro/mops/sgrwallet/controller/VoucherControllerTest.java#L68)
- [Mark used returns HTTP 200](src/test/java/ro/mops/sgrwallet/controller/VoucherControllerTest.java#L90)
- [Duplicate voucher returns HTTP 409](src/test/java/ro/mops/sgrwallet/controller/VoucherControllerTest.java#L114)

### Frontend (React / Vitest)

**[`DashboardPage.test.jsx`](frontend/src/pages/DashboardPage.test.jsx) (4 tests)**

- [Barcode rendering and available balance](frontend/src/pages/DashboardPage.test.jsx#L45)
- [Used state and balance deduction](frontend/src/pages/DashboardPage.test.jsx#L56)
- [OCR confirmation and editable correction](frontend/src/pages/DashboardPage.test.jsx#L73)

### OCR service (Python / pytest)

**[`tests/test_extractor.py`](ocr-service/tests/test_extractor.py) (6 tests)**

- [Select SGR/deposit amount](ocr-service/tests/test_extractor.py#L4)
- [Accept OCR currency mistake `HON`](ocr-service/tests/test_extractor.py#L8)
- [Reject invalid decimal noise](ocr-service/tests/test_extractor.py#L12)
- [Parse Romanian numeric date](ocr-service/tests/test_extractor.py#L16)
- [Parse text month date](ocr-service/tests/test_extractor.py#L20)
- [Detect known store](ocr-service/tests/test_extractor.py#L24)

**[`tests/test_api.py`](ocr-service/tests/test_api.py) (2 tests)**

- [Reject non-image upload with HTTP 400](ocr-service/tests/test_api.py#L10)
- [Return extracted fields from scan API](ocr-service/tests/test_api.py#L18)

These tests cover the acceptance criteria for US-01A, US-01B, US-04, US-16,
US-05A, US-05B and US-08.

## Test tools

- **Spring Boot:** JUnit 5, Mockito and AssertJ through
  `spring-boot-starter-test`
- **Frontend:** Vitest, Testing Library and jsdom
- **OCR service:** pytest and FastAPI `TestClient`
- **Coverage:** JaCoCo, Vitest V8 coverage and pytest-cov

## Running tests

From VS Code, use **Run and Debug**:

- `Run all tests`
- `Run all tests and open report` runs all suites and opens a readable HTML
  summary with status, duration and expandable logs.
- `Run all coverage and open reports` runs all coverage commands and opens the
  three HTML reports automatically.
- `Open coverage reports` reopens the latest generated reports.
- `Run backend tests` / `Run backend coverage`
- `Run frontend tests` / `Run frontend coverage`
- `Run OCR tests` / `Run OCR coverage`

Equivalent commands:

```powershell
# Backend tests and JaCoCo report
mvn test
mvn verify
# target/site/jacoco/index.html

# Frontend tests, lint and coverage
cd frontend
npm test
npm run lint
npm run test:coverage
# frontend/coverage/index.html

# OCR tests and coverage
cd ..\ocr-service
python -m pytest -q
python -m pytest --cov=app --cov-report=term-missing --cov-report=html
# ocr-service/htmlcov/index.html
```