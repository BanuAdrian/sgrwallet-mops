package ro.mops.sgrwallet.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.mops.sgrwallet.model.Role;
import ro.mops.sgrwallet.model.User;
import ro.mops.sgrwallet.repository.RoleRepository;
import ro.mops.sgrwallet.repository.UserRepository;

@Component
public class DevUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public DevUserInitializer(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            Role userRole = roleRepository.findByName("User")
                    .orElseGet(() -> roleRepository.save(new Role("User")));
            userRepository.save(new User("demo@sgrwallet.ro", "demo-hash", userRole));
        }
    }
}
