package ro.mops.sgrwallet.dto;

import ro.mops.sgrwallet.model.VoucherSource;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherResponse(
        Long id,
        String voucherCode,
        String storeName,
        BigDecimal amount,
        Instant expiryDate,
        Instant addedDate,
        boolean used,
        VoucherSource source
) {
}
