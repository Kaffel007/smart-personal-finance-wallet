package com.smartfinance.wallet.bootstrap;

import com.smartfinance.wallet.common.validation.ValidPassword;
import com.smartfinance.wallet.config.AdminBootstrapProperties;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String INVALID_CONFIGURATION_MESSAGE =
            "Configuration du compte administrateur initial invalide.";

    private final AdminBootstrapProperties properties;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            Validator validator
    ) {
        this.properties = properties;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!properties.isEnabled()) {
            LOGGER.info("Bootstrap administrateur désactivé.");
            return;
        }

        AdminBootstrapData data = normalizedAndValidatedData();
        AppUser existingUser = appUserRepository.findByEmail(data.email()).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() == Role.ADMIN) {
                LOGGER.info("Compte administrateur initial déjà présent.");
                return;
            }
            throw invalidConfiguration();
        }

        AppUser admin = new AppUser(
                data.firstName(),
                data.lastName(),
                data.email(),
                passwordEncoder.encode(data.password()),
                Role.ADMIN
        );
        admin.setEnabled(true);
        admin.setBlocked(false);
        appUserRepository.saveAndFlush(admin);
        LOGGER.info("Compte administrateur initial créé.");
    }

    private AdminBootstrapData normalizedAndValidatedData() {
        AdminBootstrapData data = new AdminBootstrapData(
                normalizeName(properties.getFirstName()),
                normalizeName(properties.getLastName()),
                normalizeEmail(properties.getEmail()),
                properties.getPassword()
        );
        Set<ConstraintViolation<AdminBootstrapData>> violations = validator.validate(data);
        if (!violations.isEmpty()) {
            throw invalidConfiguration();
        }
        return data;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private IllegalStateException invalidConfiguration() {
        return new IllegalStateException(INVALID_CONFIGURATION_MESSAGE);
    }

    private record AdminBootstrapData(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @ValidPassword String password
    ) {
    }
}
