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

    @Test
    void failsWhenSecretKeyIsMissing() {
        CredentialCipher cipher = cipher("");

        assertThrows(IllegalStateException.class, () -> cipher.encrypt("secret-password"));
    }

    @Test
    void failsWhenSecretKeyIsNotBase64() {
        CredentialCipher cipher = cipher("not-base64!");

        assertThrows(IllegalStateException.class, () -> cipher.encrypt("secret-password"));
    }

    @Test
    void failsWhenSecretKeyLengthIsInvalid() {
        CredentialCipher cipher = cipher(Base64.getEncoder().encodeToString(new byte[8]));

        assertThrows(IllegalStateException.class, () -> cipher.encrypt("secret-password"));
    }

    private CredentialCipher cipher() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return cipher(Base64.getEncoder().encodeToString(key));
    }

    private CredentialCipher cipher(String secretKey) {
        ThingModelProperties properties = new ThingModelProperties();
        properties.getSecurity().setSecretKey(secretKey);
        return new CredentialCipher(properties);
    }
}
