package ro.mops.sgrwallet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.mops.sgrwallet.dto.ManualVoucherRequest;
import ro.mops.sgrwallet.dto.VoucherResponse;
import ro.mops.sgrwallet.model.BusinessStore;
import ro.mops.sgrwallet.model.User;
import ro.mops.sgrwallet.model.Voucher;
import ro.mops.sgrwallet.model.VoucherSource;
import ro.mops.sgrwallet.repository.BusinessStoreRepository;
import ro.mops.sgrwallet.repository.UserRepository;
import ro.mops.sgrwallet.repository.VoucherRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.UUID;

@Service
public class VoucherService {

    private static final DateTimeFormatter STRICT_ISO_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final VoucherRepository voucherRepository;
    private final BusinessStoreRepository businessStoreRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public VoucherService(VoucherRepository voucherRepository,
                          BusinessStoreRepository businessStoreRepository,
                          UserRepository userRepository) {
        this(voucherRepository, businessStoreRepository, userRepository, Clock.systemUTC());
    }

    public VoucherService(VoucherRepository voucherRepository,
                          BusinessStoreRepository businessStoreRepository,
                          UserRepository userRepository,
                          Clock clock) {
        this.voucherRepository = voucherRepository;
        this.businessStoreRepository = businessStoreRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public VoucherResponse addManualVoucher(Long userId, ManualVoucherRequest request) {
        validateMandatoryFields(request);

        if (request.expiresAt() == null || request.expiresAt().isBlank()) {
            throw new IllegalArgumentException("Data expirarii este obligatorie.");
        }

        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(request.expiresAt().trim(), STRICT_ISO_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data expirarii nu este o data calendaristica valida.");
        }

        LocalDate today = LocalDate.now(clock);
        if (expiryDate.isBefore(today)) {
            throw new IllegalArgumentException("Data expirarii nu poate fi in trecut (bon expirat).");
        }

        if (request.issuedAt() != null && !request.issuedAt().isBlank()) {
            LocalDate issuedDate;
            try {
                issuedDate = LocalDate.parse(request.issuedAt().trim(), STRICT_ISO_DATE);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Data emiterii nu este o data calendaristica valida.");
            }
            if (issuedDate.isAfter(expiryDate)) {
                throw new IllegalArgumentException("Data emiterii nu poate fi dupa data expirarii.");
            }
            if (issuedDate.isAfter(today)) {
                throw new IllegalArgumentException("Data emiterii nu poate fi in viitor.");
            }
        }

        User user = findUserOrThrow(userId);
        BusinessStore store = findOrCreateStore(request.storeName().trim());
        Instant expiryInstant = expiryDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        Voucher voucher = new Voucher();
        voucher.setVoucherCode(request.voucherCode().trim());
        voucher.setValue(request.amount());
        voucher.setExpiryDate(expiryInstant);
        voucher.setUsed(false);
        voucher.setSource(VoucherSource.MANUAL);
        voucher.setUser(user);
        voucher.setBusinessStore(store);

        Voucher saved = voucherRepository.save(voucher);
        return toResponse(saved);
    }

    private void validateMandatoryFields(ManualVoucherRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Datele bonului sunt obligatorii.");
        }
        if (request.voucherCode() == null || request.voucherCode().isBlank()) {
            throw new IllegalArgumentException("Codul bonului este obligatoriu.");
        }
        if (request.storeName() == null || request.storeName().isBlank()) {
            throw new IllegalArgumentException("Numele magazinului este obligatoriu.");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Suma bonului trebuie sa fie mai mare decat 0.");
        }
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit."));
    }

    private BusinessStore findOrCreateStore(String storeName) {
        return businessStoreRepository.findByNameIgnoreCase(storeName)
                .orElseGet(() -> businessStoreRepository.save(
                        new BusinessStore(storeName, "REG-" + UUID.randomUUID().toString().substring(0, 8))
                ));
    }

    private VoucherResponse toResponse(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getVoucherCode(),
                voucher.getBusinessStore().getName(),
                voucher.getValue(),
                voucher.getExpiryDate(),
                voucher.getAddedDate(),
                voucher.isUsed(),
                voucher.getSource()
        );
    }
}
