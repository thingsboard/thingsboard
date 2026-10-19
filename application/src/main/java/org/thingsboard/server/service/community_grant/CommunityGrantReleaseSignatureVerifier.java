// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies the instance checker's detached release signature against the configured release-signing keys.
 * Fails closed: with no trusted key, every checker is refused.
 */
@Component
@TbCoreComponent
@Slf4j
public class CommunityGrantReleaseSignatureVerifier {

    static final String PUBLIC_KEYS_PROPERTY = "community-grant.checker-release-public-keys";

    /** The whole signature file: {@code MAGIC(8) ‖ VERSION(1) ‖ KEYID(1) ‖ SIG(64)}, and nothing after it. */
    static final int SIGNATURE_FILE_LENGTH = 74;

    private static final byte[] MAGIC = {'T', 'B', 'I', 'C', 'S', 'I', 'G', '1'};
    private static final int VERSION_OFFSET = 8;
    private static final byte SUPPORTED_VERSION = 0x01;
    private static final int KEY_ID_OFFSET = 9;
    private static final int SIGNATURE_OFFSET = 10;

    private static final int ED25519_PUBLIC_KEY_LENGTH = 32;

    /** RFC 8410 SubjectPublicKeyInfo prefix for a raw 32-byte Ed25519 key; KeyFactory takes no raw spec. */
    private static final byte[] ED25519_SPKI_PREFIX = {
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
    };

    @Value("${" + PUBLIC_KEYS_PROPERTY + ":}")
    private String checkerReleasePublicKeys;

    /** Empty when nothing is configured and when the configured value could not be parsed. */
    private Map<Integer, PublicKey> trustStore = Map.of();

    private String trustStoreError;

    @PostConstruct
    void init() {
        try {
            trustStore = parsePublicKeys(checkerReleasePublicKeys);
            trustStoreError = null;
        } catch (IllegalArgumentException e) {
            trustStore = Map.of();
            trustStoreError = e.getMessage();
            log.error("{} could not be parsed, so no release-signing key is trusted and every "
                    + "instance checker will be refused: {}", PUBLIC_KEYS_PROPERTY, e.getMessage());
        }
    }

    public boolean hasTrustedKeys() {
        return !trustStore.isEmpty();
    }

    /**
     * Throws {@link IllegalArgumentException} unless {@code signatureData} is a valid release signature over
     * exactly {@code checkerData} under a trusted key. Messages are safe to show the uploader and to audit.
     */
    public void verify(byte[] checkerData, byte[] signatureData) {
        if (checkerData == null || checkerData.length == 0) {
            throw new IllegalArgumentException("The uploaded instance checker is empty, so there is nothing "
                    + "to verify. It was not run.");
        }
        if (signatureData == null || signatureData.length == 0) {
            throw new IllegalArgumentException("The enrollment bundle carries no release signature for the "
                    + "instance checker, so it could not be verified and was not run. Download the enrollment "
                    + "bundle for this platform again from the License Portal.");
        }
        if (signatureData.length != SIGNATURE_FILE_LENGTH) {
            throw new IllegalArgumentException("The release signature is " + signatureData.length
                    + " bytes; a ThingsBoard release signature is exactly " + SIGNATURE_FILE_LENGTH
                    + " bytes. The uploaded instance checker was not run. Download the enrollment bundle again "
                    + "from the License Portal and upload it unmodified.");
        }
        if (!Arrays.equals(signatureData, 0, MAGIC.length, MAGIC, 0, MAGIC.length)) {
            throw new IllegalArgumentException("The release signature is not a ThingsBoard release "
                    + "signature. The uploaded instance checker was not run. Download the enrollment bundle again "
                    + "from the License Portal and upload it unmodified.");
        }
        byte version = signatureData[VERSION_OFFSET];
        if (version != SUPPORTED_VERSION) {
            throw new IllegalArgumentException("The release signature declares signature format "
                    + "version " + (version & 0xFF) + "; this deployment only understands version "
                    + SUPPORTED_VERSION + ". The uploaded instance checker was not run. Upgrade ThingsBoard, or "
                    + "use a checker release this version can verify.");
        }
        int keyId = signatureData[KEY_ID_OFFSET] & 0xFF;
        if (keyId == 0) {
            throw new IllegalArgumentException("The release signature names key id 0, which is "
                    + "reserved and never valid. The uploaded instance checker was not run.");
        }
        if (trustStore.isEmpty()) {
            if (trustStoreError != null) {
                throw new IllegalArgumentException("The configured " + PUBLIC_KEYS_PROPERTY + " could not be "
                        + "parsed (" + trustStoreError + "), so no uploaded instance checker can be verified. "
                        + "It was not run. Fix the configured value.");
            }
            throw new IllegalArgumentException("This deployment is not configured to verify an uploaded "
                    + "instance checker, so it refuses to run one. Set " + PUBLIC_KEYS_PROPERTY + " to the "
                    + "release-signing public key ThingsBoard publishes, as a comma-separated list of "
                    + "keyId:base64PublicKey entries.");
        }
        PublicKey publicKey = trustStore.get(keyId);
        if (publicKey == null) {
            throw new IllegalArgumentException("The uploaded instance checker is signed under release-signing "
                    + "key id " + keyId + ", which this deployment does not trust. It was not run. Add that "
                    + "key id and its public key to " + PUBLIC_KEYS_PROPERTY + " if ThingsBoard published "
                    + "this release under it.");
        }
        byte[] signature = Arrays.copyOfRange(signatureData, SIGNATURE_OFFSET, SIGNATURE_FILE_LENGTH);
        boolean verified;
        try {
            Signature ed25519 = Signature.getInstance("Ed25519");
            ed25519.initVerify(publicKey);
            ed25519.update(checkerData);
            verified = ed25519.verify(signature);
        } catch (GeneralSecurityException e) {
            // The JDK throws, rather than returning false, for some invalid encodings.
            log.debug("An uploaded instance checker's release signature could not be evaluated", e);
            verified = false;
        }
        if (!verified) {
            throw new IllegalArgumentException("The uploaded instance checker does not match its release "
                    + "signature, so it was not run. Download the enrollment bundle again from the License Portal "
                    + "and upload it unmodified.");
        }
    }

    /**
     * Parses {@code keyId:base64PublicKey[,keyId:base64PublicKey...]}. Every malformed entry throws rather
     * than being skipped.
     */
    static Map<Integer, PublicKey> parsePublicKeys(String configured) {
        Map<Integer, PublicKey> keys = new LinkedHashMap<>();
        if (configured == null || configured.isBlank()) {
            return Map.copyOf(keys);
        }
        for (String entry : configured.split(",", -1)) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("it has an empty entry; every entry must be "
                        + "keyId:base64PublicKey");
            }
            int separator = trimmed.indexOf(':');
            if (separator < 0) {
                throw new IllegalArgumentException("an entry is not in the keyId:base64PublicKey form");
            }
            int keyId;
            try {
                keyId = Integer.parseInt(trimmed.substring(0, separator).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("an entry's key id is not a number");
            }
            if (keyId < 1 || keyId > 255) {
                throw new IllegalArgumentException("an entry's key id is " + keyId + "; it must be 1-255 "
                        + "(0 is reserved and never valid)");
            }
            byte[] raw;
            try {
                raw = Base64.getDecoder().decode(trimmed.substring(separator + 1).trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("the public key for key id " + keyId
                        + " is not valid standard base64");
            }
            if (raw.length != ED25519_PUBLIC_KEY_LENGTH) {
                throw new IllegalArgumentException("the public key for key id " + keyId + " decodes to "
                        + raw.length + " bytes; a raw Ed25519 public key is " + ED25519_PUBLIC_KEY_LENGTH
                        + " bytes (it must not be X.509 SubjectPublicKeyInfo-wrapped)");
            }
            if (keys.containsKey(keyId)) {
                throw new IllegalArgumentException("key id " + keyId + " is configured more than once");
            }
            keys.put(keyId, ed25519PublicKey(keyId, raw));
        }
        return Map.copyOf(keys);
    }

    /** Probes initVerify here: KeyFactory accepts some off-curve values and only initVerify rejects them. */
    private static PublicKey ed25519PublicKey(int keyId, byte[] raw) {
        byte[] encoded = new byte[ED25519_SPKI_PREFIX.length + raw.length];
        System.arraycopy(ED25519_SPKI_PREFIX, 0, encoded, 0, ED25519_SPKI_PREFIX.length);
        System.arraycopy(raw, 0, encoded, ED25519_SPKI_PREFIX.length, raw.length);
        try {
            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(encoded));
            Signature.getInstance("Ed25519").initVerify(publicKey);
            return publicKey;
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("the public key for key id " + keyId
                    + " is not a usable Ed25519 public key");
        }
    }

}
