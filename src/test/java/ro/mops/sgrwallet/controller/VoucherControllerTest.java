package ro.mops.sgrwallet.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.mops.sgrwallet.dto.ManualVoucherRequest;
import ro.mops.sgrwallet.dto.VoucherResponse;
import ro.mops.sgrwallet.model.VoucherSource;
import ro.mops.sgrwallet.service.VoucherService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherControllerTest {

    @Mock
    private VoucherService voucherService;

    private VoucherController voucherController;

    @BeforeEach
    void setUp() {
        voucherController = new VoucherController(voucherService);
    }

    @Test
    @DisplayName("US-04: POST /api/vouchers returneaza 201 Created cu bonul adaugat")
    void shouldReturnCreatedWhenAddingValidManualVoucher() {
        ManualVoucherRequest request = new ManualVoucherRequest(
                "SGR-777888",
                "Lidl",
                new BigDecimal("14.00"),
                "2026-03-10",
                "2026-12-31"
        );
        VoucherResponse expected = new VoucherResponse(
                10L,
                "SGR-777888",
                "Lidl",
                new BigDecimal("14.00"),
                Instant.parse("2026-12-31T23:59:59Z"),
                Instant.parse("2026-03-15T10:00:00Z"),
                false,
                VoucherSource.MANUAL
        );

        when(voucherService.addManualVoucher(1L, request)).thenReturn(expected);

        ResponseEntity<VoucherResponse> response = voucherController.addManualVoucher(1L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SGR-777888", response.getBody().voucherCode());
    }

    @Test
    @DisplayName("US-04: GET /api/vouchers returneaza 200 OK cu lista bonurilor salvate")
    void shouldReturnUserVouchersList() {
        VoucherResponse item = new VoucherResponse(
                10L,
                "SGR-777888",
                "Lidl",
                new BigDecimal("14.00"),
                Instant.parse("2026-12-31T23:59:59Z"),
                Instant.parse("2026-03-15T10:00:00Z"),
                false,
                VoucherSource.MANUAL
        );
        when(voucherService.getUserVouchers(1L)).thenReturn(List.of(item));

        ResponseEntity<List<VoucherResponse>> response = voucherController.getUserVouchers(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
}
