package com.codeop.store.service;

import com.codeop.store.model.AppUser;
import com.codeop.store.model.PasswordResetToken;
import com.codeop.store.repository.PasswordResetTokenRepository;
import com.codeop.store.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final long expiryMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                @Value("${app.frontend.base-url}") String frontendBaseUrl,
                                @Value("${app.reset.expiration-mins}") long expiryMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.expiryMinutes = expiryMinutes;
    }

    public void sendResetLink(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found"));
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        tokenRepository.save(token);

        String link = frontendBaseUrl + "/reset?token=" + token.getToken();
        emailService.sendPasswordReset(user.getEmail(), link);
    }

    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenAndUsedFalse(tokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token expired");
        }
        AppUser user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
