package com.ooad.mhs.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Adapter pattern: BCryptPasswordHasher makes the jBCrypt library fit our PasswordHasher interface. */
class BCryptPasswordHasherTest {

    private final PasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    @DisplayName("Adapter: hash is not the plain password, and only the right password matches")
    void hashThenMatch() {
        String hash = hasher.hash("admin123");

        assertThat(hash).isNotEqualTo("admin123").startsWith("$2a$");
        assertThat(hasher.matches("admin123", hash)).isTrue();
        assertThat(hasher.matches("wrong", hash)).isFalse();
    }

    @Test
    @DisplayName("Adapter: same password gives a different hash each time (random salt)")
    void saltedHashes() {
        assertThat(hasher.hash("admin123")).isNotEqualTo(hasher.hash("admin123"));
    }
}
