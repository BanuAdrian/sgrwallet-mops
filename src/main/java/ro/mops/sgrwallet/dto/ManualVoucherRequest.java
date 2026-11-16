package ro.mops.sgrwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ManualVoucherRequest(
        @NotBlank(message = "Codul bonului este obligatoriu.")
        String voucherCode,

        @NotBlank(message = "Numele magazinului este obligatoriu.")
        String storeName,

        @NotNull(message = "Suma bonului este obligatorie.")
        @Positive(message = "Suma bonului trebuie sa fie mai mare decat 0.")
        BigDecimal amount,

        String issuedAt,

        @NotBlank(message = "Data expirarii este obligatorie.")
        String expiresAt
) {
}
