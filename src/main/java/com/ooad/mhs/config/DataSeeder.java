package com.ooad.mhs.config;

import com.ooad.mhs.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import org.mindrot.jbcrypt.BCrypt;
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

    private final AdminRepository adminRepository;

    public DataSeeder(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @PostConstruct
    public void createDefaultAdminIfNoneExists() {
        if (adminRepository.count() == 0) {
            String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
            adminRepository.save("admin", hash);
            System.out.println("[DataSeeder] Created default admin: admin / admin123");
        }
    }
}