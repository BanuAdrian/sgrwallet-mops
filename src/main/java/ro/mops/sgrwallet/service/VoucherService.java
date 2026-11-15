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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BusinessStore store = businessStoreRepository.findByNameIgnoreCase(request.storeName())
                .orElseGet(() -> businessStoreRepository.save(
                        new BusinessStore(request.storeName(), "REG-" + UUID.randomUUID().toString().substring(0, 8))
                ));

        Instant expiryInstant = LocalDate.parse(request.expiresAt())
                .atTime(23, 59, 59)
                .toInstant(ZoneOffset.UTC);

        Voucher voucher = new Voucher();
        voucher.setVoucherCode(request.voucherCode());
        voucher.setValue(request.amount());
        voucher.setExpiryDate(expiryInstant);
        voucher.setUsed(false);
        voucher.setSource(VoucherSource.MANUAL);
        voucher.setUser(user);
        voucher.setBusinessStore(store);

        Voucher saved = voucherRepository.save(voucher);

        return new VoucherResponse(
                saved.getId(),
                saved.getVoucherCode(),
                saved.getBusinessStore().getName(),
                saved.getValue(),
                saved.getExpiryDate(),
                saved.getAddedDate(),
                saved.isUsed(),
                saved.getSource()
        );
    }
}
