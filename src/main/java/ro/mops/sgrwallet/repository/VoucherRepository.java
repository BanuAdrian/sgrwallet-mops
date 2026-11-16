package ro.mops.sgrwallet.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.mops.sgrwallet.model.Voucher;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findByUserIdOrderByAddedDateDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select voucher from Voucher voucher where voucher.id = :voucherId and voucher.user.id = :userId")
    Optional<Voucher> findByIdAndUserIdForUpdate(
            @Param("voucherId") Long voucherId,
            @Param("userId") Long userId
    );
}
