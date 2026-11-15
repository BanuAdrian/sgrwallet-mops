package ro.mops.sgrwallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.mops.sgrwallet.model.Voucher;

import java.util.List;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findByUserIdOrderByAddedDateDesc(Long userId);
}
