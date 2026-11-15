package ro.mops.sgrwallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.mops.sgrwallet.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
