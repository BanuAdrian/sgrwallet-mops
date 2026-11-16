package ro.mops.sgrwallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ro.mops.sgrwallet.dto.ManualVoucherRequest;
import ro.mops.sgrwallet.dto.VoucherResponse;
import ro.mops.sgrwallet.exception.DuplicateVoucherException;
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

    @Test
    @DisplayName("US-04: Respinge bonul cand codul bonului este gol sau null")
    void shouldRejectVoucherWhenCodeIsBlank() {
        ManualVoucherRequest request = new ManualVoucherRequest(
                "   ",
                "Kaufland",
                new BigDecimal("10.00"),
                "2026-03-10",
                "2026-12-31"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, request)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("codul bonului"));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-04: Respinge bonul cand numele magazinului este gol sau null")
    void shouldRejectVoucherWhenStoreNameIsBlank() {
        ManualVoucherRequest request = new ManualVoucherRequest(
                "SGR-111222",
                "",
                new BigDecimal("10.00"),
                "2026-03-10",
                "2026-12-31"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, request)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("magazin"));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-04: Respinge bonul cand suma este zero, negativa sau null")
    void shouldRejectVoucherWhenAmountIsNullOrNonPositive() {
        ManualVoucherRequest zeroAmountRequest = new ManualVoucherRequest(
                "SGR-111222",
                "Lidl",
                BigDecimal.ZERO,
                "2026-03-10",
                "2026-12-31"
        );
        ManualVoucherRequest negativeAmountRequest = new ManualVoucherRequest(
                "SGR-111222",
                "Lidl",
                new BigDecimal("-5.00"),
                "2026-03-10",
                "2026-12-31"
        );

        IllegalArgumentException zeroEx = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, zeroAmountRequest)
        );
        assertTrue(zeroEx.getMessage().toLowerCase().contains("suma"));

        IllegalArgumentException negativeEx = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, negativeAmountRequest)
        );
        assertTrue(negativeEx.getMessage().toLowerCase().contains("suma"));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-04: Respinge bonul cand data expirarii lipseste sau este o data calendaristica inexistenta (ex. 2026-02-30)")
    void shouldRejectVoucherWhenExpiryDateIsMissingOrInvalidCalendarDate() {
        ManualVoucherRequest blankDateRequest = new ManualVoucherRequest(
                "SGR-111222",
                "Lidl",
                new BigDecimal("10.00"),
                "2026-03-10",
                "   "
        );
        ManualVoucherRequest impossibleDateRequest = new ManualVoucherRequest(
                "SGR-111222",
                "Lidl",
                new BigDecimal("10.00"),
                "2026-03-10",
                "2026-02-30"
        );

        IllegalArgumentException blankEx = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, blankDateRequest)
        );
        assertTrue(blankEx.getMessage().toLowerCase().contains("expir"));

        IllegalArgumentException invalidCalEx = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, impossibleDateRequest)
        );
        assertTrue(invalidCalEx.getMessage().toLowerCase().contains("calendaristic"));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-04: Respinge bonul cand data expirarii este in trecut")
    void shouldRejectVoucherWhenExpiryDateIsInThePast() {
        ManualVoucherRequest expiredRequest = new ManualVoucherRequest(
                "SGR-111222",
                "Mega Image",
                new BigDecimal("12.00"),
                "2026-02-01",
                "2026-03-01"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, expiredRequest)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("trecut") || ex.getMessage().toLowerCase().contains("expirat"));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-04: Respinge bonul cand data emiterii este invalida calendaristic (2026-02-29) sau dupa data expirarii")
    void shouldRejectVoucherWhenIssuedDateIsInvalidOrAfterExpiryDate() {
        ManualVoucherRequest nonLeapYearRequest = new ManualVoucherRequest(
                "SGR-111222",
                "Carrefour",
                new BigDecimal("8.50"),
                "2026-02-29",
                "2026-12-31"
        );
        ManualVoucherRequest issuedAfterExpiryRequest = new ManualVoucherRequest(
                "SGR-111222",
                "Carrefour",
                new BigDecimal("8.50"),
                "2026-06-15",
                "2026-05-01"
        );

        IllegalArgumentException leapEx = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, nonLeapYearRequest)
        );
        assertTrue(leapEx.getMessage().toLowerCase().contains("calendaristic"));

        IllegalArgumentException orderEx = assertThrows(
                IllegalArgumentException.class,
                () -> voucherService.addManualVoucher(1L, issuedAfterExpiryRequest)
        );
        assertTrue(orderEx.getMessage().toLowerCase().contains("emiterii"));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-04: Returneaza lista bonurilor utilizatorului astfel incat bonul salvat sa apara in lista")
    void shouldReturnUserVouchersAfterSaving() {
        Long userId = 1L;
        User user = new User("user@sgr.ro", "hashed", new Role("User"));
        BusinessStore store = new BusinessStore("Kaufland", "RO123456");

        Voucher voucher = new Voucher();
        voucher.setVoucherCode("SGR-555666");
        voucher.setValue(new BigDecimal("20.00"));
        voucher.setExpiryDate(Instant.parse("2026-12-31T23:59:59Z"));
        voucher.setUsed(false);
        voucher.setSource(VoucherSource.MANUAL);
        voucher.setUser(user);
        voucher.setBusinessStore(store);

        when(voucherRepository.findByUserIdOrderByAddedDateDesc(userId)).thenReturn(java.util.List.of(voucher));

        java.util.List<VoucherResponse> vouchers = voucherService.getUserVouchers(userId);

        assertEquals(1, vouchers.size());
        assertEquals("SGR-555666", vouchers.get(0).voucherCode());
        assertEquals("Kaufland", vouchers.get(0).storeName());
        assertEquals(new BigDecimal("20.00"), vouchers.get(0).amount());
    }

    @Test
    @DisplayName("US-08: Marcheaza bonul ca folosit si inregistreaza momentul folosirii")
    void shouldMarkVoucherUsedAndSetUsedDate() {
        Voucher voucher = createVoucher(false);
        when(voucherRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(voucher)).thenReturn(voucher);

        VoucherResponse response = voucherService.markVoucherUsed(1L, 10L);

        assertTrue(response.used());
        assertEquals(fixedClock.instant(), voucher.getUsedDate());
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("US-08: Nu permite folosirea repetata a aceluiasi bon")
    void shouldRejectVoucherThatIsAlreadyUsed() {
        Voucher voucher = createVoucher(true);
        when(voucherRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(voucher));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> voucherService.markVoucherUsed(1L, 10L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-08: Nu expune bonul altui utilizator")
    void shouldRejectVoucherNotOwnedByUser() {
        when(voucherRepository.findByIdAndUserIdForUpdate(10L, 2L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> voucherService.markVoucherUsed(2L, 10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-16: Respinge bonul inainte de salvare daca exista deja acelasi cod in baza de date")
    void shouldRejectDuplicateVoucherCodeBeforeSaving() {
        ManualVoucherRequest request = new ManualVoucherRequest(
                "SGR-987654321",
                "Kaufland",
                new BigDecimal("15.50"),
                "2026-03-10",
                "2026-12-31"
        );

        when(voucherRepository.existsByVoucherCodeIgnoreCase("SGR-987654321")).thenReturn(true);

        DuplicateVoucherException ex = assertThrows(
                DuplicateVoucherException.class,
                () -> voucherService.addManualVoucher(1L, request)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("exista deja"));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-16: Detecteaza bonul dublat chiar daca are spatii la capete sau litere cu alta capitalizare")
    void shouldRejectDuplicateVoucherCodeWithWhitespaceOrDifferentCase() {
        ManualVoucherRequest request = new ManualVoucherRequest(
                "  sgr-987654321  ",
                "Lidl",
                new BigDecimal("10.00"),
                "2026-03-10",
                "2026-12-31"
        );

        when(voucherRepository.existsByVoucherCodeIgnoreCase("sgr-987654321")).thenReturn(true);

        DuplicateVoucherException ex = assertThrows(
                DuplicateVoucherException.class,
                () -> voucherService.addManualVoucher(1L, request)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("cod"));
        verify(voucherRepository, never()).save(any());
        verifyNoInteractions(businessStoreRepository);
    }

    private Voucher createVoucher(boolean used) {
        Voucher voucher = new Voucher();
        voucher.setVoucherCode("SGR-123456");
        voucher.setValue(new BigDecimal("12.50"));
        voucher.setExpiryDate(Instant.parse("2026-12-31T23:59:59Z"));
        voucher.setUsed(used);
        voucher.setUsedDate(used ? fixedClock.instant() : null);
        voucher.setSource(VoucherSource.MANUAL);
        voucher.setUser(new User("user@sgr.ro", "hashed", new Role("User")));
        voucher.setBusinessStore(new BusinessStore("Lidl", "RO123456"));
        return voucher;
    }
}
