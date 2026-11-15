package com.ooad.mhs.repository;

import com.ooad.mhs.domain.Admin;

import java.util.Optional;

/**
 * Loads and saves admin accounts (passwords are stored as BCrypt hashes).
 *
 * Repository (Spring architecture, not a design pattern) + Dependency Inversion (SOLID): services depend on this interface;
 * the SQLite implementation is {@link JdbcAdminRepository}.
 */
public interface AdminRepository {

    /** Finds an admin by username, for login. */
    Optional<Admin> findByUsername(String username);

    /** Number of admin accounts (DataSeeder creates the default admin when this is 0). */
    long count();

    /** Creates an admin account; does nothing if the username already exists. */
    void save(String username, String passwordHash);
}
