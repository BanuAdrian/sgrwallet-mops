package ro.mops.sgrwallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.mops.sgrwallet.dto.ManualVoucherRequest;
import ro.mops.sgrwallet.dto.VoucherResponse;
import ro.mops.sgrwallet.model.BusinessStore;
import ro.mops.sgrwallet.model.Role;
import ro.mops.sgrwallet.model.User;
import ro.mops.sgrwallet.model.Voucher;
import ro.mops.sgrwallet.model.VoucherSource;
import ro.mops.sgrwallet.repository.BusinessStoreRepository;
import ro.mops.sgrwallet.repository.UserRepository;
import ro.mops.sgrwallet.repository.VoucherRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private BusinessStoreRepository businessStoreRepository;

    @Mock
    private UserRepository userRepository;

    private Clock fixedClock;
    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);
        voucherService = new VoucherService(voucherRepository, businessStoreRepository, userRepository, fixedClock);
    }

    @Test
    @DisplayName("US-04: Adauga cu succes un bon de mana cand toate datele sunt valide")
    void shouldAddManualVoucherWhenDataIsValid() {
        Long userId = 1L;
        User user = new User("user@sgr.ro", "hashed", new Role("User"));
        BusinessStore store = new BusinessStore("Kaufland", "RO123456");

        ManualVoucherRequest request = new ManualVoucherRequest(
                "SGR-987654321",
                "Kaufland",
                new BigDecimal("15.50"),
                "2026-03-10",
                "2026-12-31"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(businessStoreRepository.findByNameIgnoreCase("Kaufland")).thenReturn(Optional.of(store));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoucherResponse response = voucherService.addManualVoucher(userId, request);

        assertNotNull(response);
        assertEquals("SGR-987654321", response.voucherCode());
        assertEquals("Kaufland", response.storeName());
        assertEquals(new BigDecimal("15.50"), response.amount());
        assertEquals(VoucherSource.MANUAL, response.source());
        assertFalse(response.used());
        verify(voucherRepository, times(1)).save(any(Voucher.class));
    }
}
