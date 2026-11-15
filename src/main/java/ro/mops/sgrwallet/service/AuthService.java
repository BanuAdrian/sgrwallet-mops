package ro.mops.sgrwallet.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ro.mops.sgrwallet.dto.AuthDtos.AuthResponse;
import ro.mops.sgrwallet.dto.AuthDtos.LoginRequest;
import ro.mops.sgrwallet.dto.AuthDtos.RegisterRequest;
import ro.mops.sgrwallet.model.Role;
import ro.mops.sgrwallet.model.User;
import ro.mops.sgrwallet.repository.RoleRepository;
import ro.mops.sgrwallet.repository.UserRepository;

@Service
public class AuthService {
    private static final String USER_ROLE = "User";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtKey;
    private final long jwtExpirationSeconds;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-seconds:86400}") long jwtExpirationSeconds) {
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must contain at least 32 characters");
        }
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationSeconds = jwtExpirationSeconds;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Acest email este deja folosit.");
        }

        Role role = roleRepository.findByNameIgnoreCase(USER_ROLE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Rolurile aplicației nu sunt configurate."));

        User user = new User(email, passwordEncoder.encode(request.password()), role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email sau parolă incorectă."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email sau parolă incorectă.");
        }
        if ("Blocked".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Contul tău a fost blocat. Contactează administratorul.");
        }
        if ("Pending".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Contul tău urmează să fie aprobat de un administrator. Te rugăm să aștepți.");
        }

        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtExpirationSeconds)))
                .signWith(jwtKey)
                .compact();
        return new AuthResponse(token, user.getRole().getName(), user.getEmail());
    }
}
