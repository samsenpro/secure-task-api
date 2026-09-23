package com.example.secureapi.config;

import com.example.secureapi.common.Emails;
import com.example.secureapi.common.PasswordPolicy;
import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;
import com.example.secureapi.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el administrador inicial si se han definido ADMIN_EMAIL y ADMIN_PASSWORD y aún no existe.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(AdminProperties adminProperties,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.adminProperties = adminProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminProperties.isConfigured()) {
            log.info("Admin seed skipped: ADMIN_EMAIL / ADMIN_PASSWORD not set");
            return;
        }

        String email = Emails.normalize(adminProperties.email());
        if (userRepository.existsByEmail(email)) {
            log.info("Admin seed skipped: account already exists");
            return;
        }

        if (!PasswordPolicy.isSatisfiedBy(adminProperties.password())) {
            log.warn("Admin seed skipped: ADMIN_PASSWORD does not satisfy the password policy");
            return;
        }

        String name = adminProperties.name() == null || adminProperties.name().isBlank()
                ? "Administrator"
                : adminProperties.name().trim();
        User admin = userRepository.save(
                new User(name, email, passwordEncoder.encode(adminProperties.password()), Role.ADMIN));
        log.info("Initial admin account created with id={}", admin.getId());
    }
}
