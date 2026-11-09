package com.ooad.mhs.repository;

import com.ooad.mhs.domain.Admin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Loads and saves admin accounts for authentication.
 * 
 * Only 1-2 admins expected (default: admin/admin123 created by DataSeeder).
 * Passwords are stored as BCrypt hashes, never plain text.
 */
@Repository
public class AdminRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Admin> MAPPER = (rs, rowNum) -> new Admin(
        rs.getLong("id"),
        rs.getString("username"),
        rs.getString("password_hash")
    );

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Find admin by username for login authentication. */
    public Optional<Admin> findByUsername(String username) {
        List<Admin> results = jdbcTemplate.query(
            "SELECT * FROM admin WHERE username = ?", MAPPER, username);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /** Count admins. Used by DataSeeder to check if default admin needs creating. */
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin", Long.class);
        return count != null ? count : 0;
    }

    /** Create a new admin account. Used by DataSeeder on first startup. */
    public void save(String username, String passwordHash) {
        jdbcTemplate.update(
            "INSERT OR IGNORE INTO admin (username, password_hash) VALUES (?, ?)",
            username, passwordHash
        );
    }
}