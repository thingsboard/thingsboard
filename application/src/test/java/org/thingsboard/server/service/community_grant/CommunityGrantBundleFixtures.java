// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Golden {@code TBICBDL1} vectors; {@code vectors.json} records each file's size and SHA-256.
 */
public final class CommunityGrantBundleFixtures {

    public static final String CLUSTER_ID = "3f2a1c44-9f3b-11ee-8c90-0242ac120002";
    public static final String CHALLENGE = "Y2hhbGxlbmdlLTAwMS1ieXRlcw==";
    public static final String PORTAL_PUBLIC_KEY = "KTA3PkVMU1phaG92fYSLkpmgp661vMPK0djf5u30+wI=";
    public static final int KEY_ID = 1;
    public static final long REFERENCE_MILLIS = 1787788800000L;
    public static final long ISSUED_AT = 4102444800000L;
    public static final long CHALLENGE_EXPIRES_AT = 4102531200000L;
    public static final String CHECKER_FILE_NAME = "tb-instance-check_linux_amd64";

    /** Verifies the golden bundle's {@code RELSIG} under {@link #KEY_ID}. */
    public static final String RELEASE_PUBLIC_KEY = "Nqbrd0SVV4w9yZWl0cgu9I7G/URhdL3PN8mWRwnWDzE=";
    public static final String TRUSTS_RELEASE_KEY = KEY_ID + ":" + RELEASE_PUBLIC_KEY;

    private static final String RELSIG_HEX = "544249435349473101013ab2afc43727f37b9388510b6c0e0b7d920a738ee"
            + "df45aae5169aa7d97abe3f73796b907168188e0685566bbf792ae0977b6c38072e684ee149d6bcf161fc004";

    private static final String VECTOR_RESOURCE_DIR = "/community-grant/";

    private CommunityGrantBundleFixtures() {
    }

    public static byte[] vector(String fileName) {
        return resource(VECTOR_RESOURCE_DIR + fileName);
    }

    public static JsonNode vectorsManifest() {
        return JacksonUtil.fromBytes(resource(VECTOR_RESOURCE_DIR + "vectors.json"));
    }

    public static byte[] relsig() {
        return HexFormat.of().parseHex(RELSIG_HEX);
    }

    public static byte[] standInChecker() {
        byte[] checker = new byte[64];
        for (int i = 0; i < checker.length; i++) {
            checker[i] = (byte) (i * 7 + 3);
        }
        return checker;
    }

    public static ObjectNode validManifest() {
        ObjectNode manifest = JacksonUtil.newObjectNode();
        manifest.put("bundleVersion", 1);
        manifest.put("os", "linux");
        manifest.put("arch", "amd64");
        manifest.put("clusterId", CLUSTER_ID);
        manifest.set("checkerInput", validCheckerInput());
        manifest.put("challengeExpiresAt", CHALLENGE_EXPIRES_AT);
        manifest.put("issuedAt", ISSUED_AT);
        manifest.put("checkerFileName", CHECKER_FILE_NAME);
        return manifest;
    }

    public static ObjectNode validCheckerInput() {
        ObjectNode checkerInput = JacksonUtil.newObjectNode();
        checkerInput.put("clusterId", CLUSTER_ID);
        checkerInput.put("keyId", KEY_ID);
        checkerInput.put("publicKey", PORTAL_PUBLIC_KEY);
        checkerInput.put("challenge", CHALLENGE);
        checkerInput.put("referenceMillis", REFERENCE_MILLIS);
        return checkerInput;
    }

    public static byte[] bundle(JsonNode manifest) {
        return bundle(JacksonUtil.toString(manifest));
    }

    public static byte[] bundle(String manifestJson) {
        return bundle(manifestJson.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] bundle(byte[] manifest) {
        return bundle(relsig(), manifest, standInChecker());
    }

    public static byte[] bundle(byte[] relsig, byte[] manifest, byte[] checker) {
        return frame((byte) 1, manifest.length, checker.length, relsig, manifest, checker);
    }

    /** Declared lengths are written as given, so the header can disagree with the body. */
    public static byte[] frame(byte version, long declaredManifestLen, long declaredCheckerLen,
                               byte[] relsig, byte[] manifest, byte[] checker) {
        return ByteBuffer.allocate(CommunityGrantOfflineBundle.HEADER_LEN + manifest.length + checker.length)
                .put("TBICBDL1".getBytes(StandardCharsets.US_ASCII))
                .put(version)
                .putInt((int) declaredManifestLen)
                .putInt((int) declaredCheckerLen)
                .put(relsig)
                .put(manifest)
                .put(checker)
                .array();
    }

    private static byte[] resource(String path) {
        try (InputStream in = CommunityGrantBundleFixtures.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException(path + " is not on the test classpath");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
