package ro.mops.sgrwallet.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import ro.mops.sgrwallet.dto.AuthDtos.AuthResponse;
import ro.mops.sgrwallet.dto.AuthDtos.LoginRequest;
import ro.mops.sgrwallet.dto.AuthDtos.RegisterRequest;
import ro.mops.sgrwallet.model.Role;
import ro.mops.sgrwallet.model.User;
import ro.mops.sgrwallet.repository.RoleRepository;
import ro.mops.sgrwallet.repository.UserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String JWT_SECRET = "a-test-secret-with-at-least-32-characters";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, JWT_SECRET, 3600);
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsUserRole() {
        Role role = new Role("User");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase("User")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        authService.register(new RegisterRequest("  USER@example.com ", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("hashed-password", saved.getPasswordHash());
        assertSame(role, saved.getRole());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(new RegisterRequest("USER@example.com", "password123"))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsJwtForValidCredentials() {
        Role role = new Role("User");
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getPasswordHash()).thenReturn("hashed-password");
        when(user.getStatus()).thenReturn("Active");
        when(user.getRole()).thenReturn(role);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest(" USER@example.com ", "password123"));

        assertEquals("user@example.com", response.email());
        assertEquals("User", response.role());
        assertNotNull(response.token());
        assertJwtContainsExpectedClaims(response.token());
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("hashed-password");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new LoginRequest("user@example.com", "wrong"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void loginRejectsBlockedAccount() {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("hashed-password");
        when(user.getStatus()).thenReturn("Blocked");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new LoginRequest("user@example.com", "password123"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private void assertJwtContainsExpectedClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertEquals("7", claims.getSubject());
        assertEquals("user@example.com", claims.get("email", String.class));
        assertEquals("User", claims.get("role", String.class));
        assertTrue(claims.getExpiration().after(new Date()));
    }
}
