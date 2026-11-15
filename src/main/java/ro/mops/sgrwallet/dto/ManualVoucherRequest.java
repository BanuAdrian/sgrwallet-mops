package ro.mops.sgrwallet.dto;

import java.math.BigDecimal;

public record ManualVoucherRequest(
        String voucherCode,
        String storeName,
        BigDecimal amount,
        String issuedAt,
        String expiresAt
) {
}
