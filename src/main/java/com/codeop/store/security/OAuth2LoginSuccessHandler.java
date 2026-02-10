package com.codeop.store.security;

import com.codeop.store.model.AppUser;
import com.codeop.store.model.Role;
import com.codeop.store.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String cookieName;
    private final long expirationMs;
    private final String frontendBaseUrl;

    public OAuth2LoginSuccessHandler(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     JwtService jwtService,
                                     @Value("${app.jwt.cookie-name}") String cookieName,
                                     @Value("${app.jwt.expiration-ms}") long expirationMs,
                                     @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
        this.expirationMs = expirationMs;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User)) {
            response.sendRedirect(frontendBaseUrl + "/login");
            return;
        }
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = value(attributes, "email");
        if (email == null) {
            response.sendRedirect(frontendBaseUrl + "/login?error=missing_email");
            return;
        }
        AppUser user = userRepository.findByEmail(email).orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setEmail(email);
            newUser.setUsername(email);
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setRole(Role.USER);
            return userRepository.save(newUser);
        });
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.sendRedirect(frontendBaseUrl + "/");
    }

    private String value(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
