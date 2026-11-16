package ro.mops.sgrwallet.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.mops.sgrwallet.dto.AuthDtos.AuthResponse;
import ro.mops.sgrwallet.dto.AuthDtos.LoginRequest;
import ro.mops.sgrwallet.dto.AuthDtos.MessageResponse;
import ro.mops.sgrwallet.dto.AuthDtos.RegisterRequest;
import ro.mops.sgrwallet.service.AuthService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerReturnsCreatedResponse() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123");

        ResponseEntity<MessageResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Cont creat cu succes!", response.getBody().message());
        verify(authService).register(request);
    }

    @Test
    void loginReturnsAuthenticationResponse() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        AuthResponse expected = new AuthResponse("jwt-token", "User", "user@example.com");
        when(authService.login(request)).thenReturn(expected);

        AuthResponse response = authController.login(request);

        assertSame(expected, response);
    }
}
