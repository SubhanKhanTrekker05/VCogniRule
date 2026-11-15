package com.ooad.mhs.config;

import com.ooad.mhs.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import com.ooad.mhs.security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Creates a default admin account on first startup if none exists.
 * 
 * Default credentials: admin / admin123
 * Password is BCrypt-hashed — never stored as plain text.
 * 
 * Runs after Spring initializes all beans and the database schema/data
 * have been loaded (schema.sql + data.sql).
 */
@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AdminRepository adminRepository;
    private final PasswordHasher passwordHasher;

    public DataSeeder(AdminRepository adminRepository, PasswordHasher passwordHasher) {
        this.adminRepository = adminRepository;
        this.passwordHasher = passwordHasher;
    }

    @PostConstruct
    public void createDefaultAdminIfNoneExists() {
        if (adminRepository.count() == 0) {
            String hash = passwordHasher.hash("admin123");
            adminRepository.save("admin", hash);
            log.info("Created default admin account: admin / admin123");
        }
    }
}