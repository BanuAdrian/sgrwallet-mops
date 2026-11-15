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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class VoucherService {

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
        User user = findUserOrThrow(userId);
        BusinessStore store = findOrCreateStore(request.storeName().trim());
        Instant expiryInstant = LocalDate.parse(request.expiresAt().trim())
                .atTime(23, 59, 59)
                .toInstant(ZoneOffset.UTC);

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
