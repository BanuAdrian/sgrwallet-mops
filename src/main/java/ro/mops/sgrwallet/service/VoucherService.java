package ro.mops.sgrwallet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.mops.sgrwallet.dto.ManualVoucherRequest;
import ro.mops.sgrwallet.dto.VoucherResponse;
import ro.mops.sgrwallet.repository.BusinessStoreRepository;
import ro.mops.sgrwallet.repository.UserRepository;
import ro.mops.sgrwallet.repository.VoucherRepository;

import java.time.Clock;

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
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
