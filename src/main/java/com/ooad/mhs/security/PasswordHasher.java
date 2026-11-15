package com.ooad.mhs.security;

/**
 * What our code needs for passwords: make a hash, and check a password against a hash.
 * (The Target interface of the Adapter pattern; {@link BCryptPasswordHasher} adapts the
 * BCrypt library to it.)
 */
public interface PasswordHasher {

    /** @return a one-way hash of the password, safe to store in the database */
    String hash(String plainPassword);

    /** @return true if the password matches the stored hash */
    boolean matches(String plainPassword, String storedHash);
}
