package com.hssc.backend;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void pendingSchoolCannotSignInAndReceivesAdministratorReviewMessage() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        JwtEncoder encoder = mock(JwtEncoder.class);
        UserAccount school = new UserAccount("school@example.test", "encoded-password", Role.SCHOOL, AccountStatus.PENDING);
        when(users.findByEmailIgnoreCase("school@example.test")).thenReturn(Optional.of(school));
        when(passwords.matches("school-password", "encoded-password")).thenReturn(true);
        AuthService auth = new AuthService(users, passwords, encoder, "hssc-test", 60);

        ApiException error = assertThrows(ApiException.class,
                () -> auth.login(new Api.LoginRequest("school@example.test", "school-password")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        assertEquals("Your account is still under review. Please wait for administrator approval before signing in.",
                error.getMessage());
        verifyNoInteractions(encoder);
    }
}
