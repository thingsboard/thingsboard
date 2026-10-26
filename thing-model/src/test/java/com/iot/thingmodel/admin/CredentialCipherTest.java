package com.iot.thingmodel.admin;

import com.iot.thingmodel.config.ThingModelProperties;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialCipherTest {

    @Test
    void encryptsAndDecryptsCredential() {
        CredentialCipher cipher = cipher();

        String encrypted = cipher.encrypt("secret-password");

        assertNotEquals("secret-password", encrypted);
        assertEquals("secret-password", cipher.decrypt(encrypted));
    }

    @Test
    void failsWhenCipherTextIsTampered() {
        CredentialCipher cipher = cipher();
        String encrypted = cipher.encrypt("secret-password");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
    }

    private CredentialCipher cipher() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        ThingModelProperties properties = new ThingModelProperties();
        properties.getSecurity().setSecretKey(Base64.getEncoder().encodeToString(key));
        return new CredentialCipher(properties);
    }
}
