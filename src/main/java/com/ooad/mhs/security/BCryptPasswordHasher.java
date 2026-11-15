package com.ooad.mhs.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern: adapts the jBCrypt library (static methods hashpw / gensalt / checkpw)
 * to our {@link PasswordHasher} interface. The rest of the app never calls BCrypt directly,
 * so the library could be replaced by changing only this class.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    @Override
    public boolean matches(String plainPassword, String storedHash) {
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}
