package ro.mops.sgrwallet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ro.mops.sgrwallet.dto.ManualVoucherRequest;
import ro.mops.sgrwallet.dto.VoucherResponse;
import ro.mops.sgrwallet.service.VoucherService;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@CrossOrigin(origins = "*")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    public ResponseEntity<VoucherResponse> addManualVoucher(
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long fallbackUserId,
            @RequestBody ManualVoucherRequest request) {
        Long userId = resolveUserId(authentication, fallbackUserId);
        return addManualVoucher(userId, request);
    }

    public ResponseEntity<VoucherResponse> addManualVoucher(Long userId, ManualVoucherRequest request) {
        VoucherResponse created = voucherService.addManualVoucher(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<VoucherResponse>> getUserVouchers(
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long fallbackUserId) {
        Long userId = resolveUserId(authentication, fallbackUserId);
        return getUserVouchers(userId);
    }

    public ResponseEntity<List<VoucherResponse>> getUserVouchers(Long userId) {
        return ResponseEntity.ok(voucherService.getUserVouchers(userId));
    }

    private Long resolveUserId(Authentication authentication, Long fallbackUserId) {
        if (authentication != null && authentication.getName() != null) {
            try {
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallbackUserId != null ? fallbackUserId : 1L;
    }
}
