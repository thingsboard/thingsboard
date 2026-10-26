package com.iot.thingmodel.admin;

import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.config.ThingModelProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class CredentialCipher {

    private final ThingModelProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(ThingModelProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[Constants.Auth.IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(Constants.Auth.AES_GCM_NO_PADDING);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key(),
                    new GCMParameterSpec(Constants.Auth.TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt tenant credential", ex);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            if (decoded.length < Constants.Auth.IV_LENGTH) {
                throw new IllegalArgumentException("Invalid cipher text");
            }
            byte[] iv = new byte[Constants.Auth.IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - Constants.Auth.IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, Constants.Auth.IV_LENGTH);
            System.arraycopy(decoded, Constants.Auth.IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(Constants.Auth.AES_GCM_NO_PADDING);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key(),
                    new GCMParameterSpec(Constants.Auth.TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt tenant credential", ex);
        }
    }

    private SecretKeySpec key() {
        String configured = properties.getSecurity().getSecretKey();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("THINGMODEL_SECRET_KEY is required for tenant credential encryption");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("THINGMODEL_SECRET_KEY must be Base64 encoded", ex);
        }
        if (decoded.length != Constants.Auth.AES_KEY_LENGTH_128
                && decoded.length != Constants.Auth.AES_KEY_LENGTH_192
                && decoded.length != Constants.Auth.AES_KEY_LENGTH_256) {
            throw new IllegalStateException("THINGMODEL_SECRET_KEY must decode to "
                    + Constants.Auth.AES_KEY_LENGTH_128 + ", "
                    + Constants.Auth.AES_KEY_LENGTH_192 + " or "
                    + Constants.Auth.AES_KEY_LENGTH_256 + " bytes");
        }
        return new SecretKeySpec(decoded, Constants.Auth.AES);
    }
}
