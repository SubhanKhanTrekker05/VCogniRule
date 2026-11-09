package com.ooad.mhs.domain;

/**
 * Represents an admin account for authentication.
 * 
 * Only admins can access the rule management UI (add/edit/delete/toggle rules).
 * Regular users are anonymous — no account, no login, no PII.
 * 
 * Password is stored as a BCrypt hash (never plain text).
 * The AdminService verifies passwords using BCrypt.checkpw().
 * 
 * A default admin (username="admin", password="admin123") is created
 * by DataSeeder on first startup if no admin exists.
 * 
 * Stored in: admin table (0 rows until DataSeeder runs)
 */
public record Admin(
    Long id,
    String username,        // unique, e.g., "admin"
    String passwordHash     // BCrypt hash, e.g., "$2a$10$N9qo8uLOi..."
) {}