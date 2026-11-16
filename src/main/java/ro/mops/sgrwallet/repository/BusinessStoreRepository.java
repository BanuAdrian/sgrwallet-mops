package ro.mops.sgrwallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.mops.sgrwallet.model.BusinessStore;

import java.util.Optional;

public interface BusinessStoreRepository extends JpaRepository<BusinessStore, Long> {
    Optional<BusinessStore> findByNameIgnoreCase(String name);
}
