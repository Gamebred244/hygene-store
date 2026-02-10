package com.codeop.store.config;

import com.codeop.store.model.AppUser;
import com.codeop.store.model.Role;
import com.codeop.store.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        fillMissingEmails();
        if (!userRepository.existsByUsername("admin")) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setEmail("admin@hygiene-store.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }
        if (!userRepository.existsByUsername("user")) {
            AppUser user = new AppUser();
            user.setUsername("user");
            user.setEmail("user@hygiene-store.local");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole(Role.USER);
            userRepository.save(user);
        }
    }

    private void fillMissingEmails() {
        for (AppUser user : userRepository.findByEmailIsNull()) {
            String email = user.getUsername() + "@hygiene-store.local";
            if (userRepository.existsByEmail(email)) {
                email = user.getUsername() + "-" + user.getId() + "@hygiene-store.local";
            }
            user.setEmail(email);
            userRepository.save(user);
        }
    }
}
