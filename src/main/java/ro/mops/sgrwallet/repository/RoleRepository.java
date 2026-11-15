package ro.mops.sgrwallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.mops.sgrwallet.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Optional<Role> findByNameIgnoreCase(String name);
}
