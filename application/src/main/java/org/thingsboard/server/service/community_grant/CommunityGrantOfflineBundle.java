// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.thingsboard.server.service.community_grant.CommunityGrantBundleFormatException.Reason;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The {@code TBICBDL1} offline enrollment bundle.
 * <pre>
 * offset  width  field         value / constraint
 * 0       8      MAGIC         "TBICBDL1"
 * 8       1      VERSION       0x01
 * 9       4      MANIFEST_LEN  uint32 big-endian, 1..8192
 * 13      4      CHECKER_LEN   uint32 big-endian, >= 1
 * 17      74     RELSIG        a complete TBICSIG1 file, verbatim
 * 91      M      MANIFEST      UTF-8 JSON
 * 91+M    C      CHECKER       the executable image
 * </pre>
 * {@code RELSIG} is not inspected here; {@link CommunityGrantReleaseSignatureVerifier} does that.
 */
public record CommunityGrantOfflineBundle(String os, String arch, UUID clusterId, String checkerFileName,
                                          long issuedAt, long challengeExpiresAt,
                                          CommunityGrantCheckerInput checkerInput, byte[] signature, byte[] checker) {

    public static final int HEADER_LEN = 91;
    public static final int MIN_MANIFEST_LEN = 1;
    public static final int MAX_MANIFEST_LEN = 8192;

    public static final int MAX_OVERHEAD_BYTES = HEADER_LEN + MAX_MANIFEST_LEN;

    private static final byte[] MAGIC = {'T', 'B', 'I', 'C', 'B', 'D', 'L', '1'};
    private static final byte VERSION_V1 = 0x01;
    private static final int RELSIG_OFFSET = 17;
    private static final int RELSIG_LEN = 74;
    private static final long MIN_CHECKER_LEN = 1;

    private static final int MANIFEST_LEN_OFFSET = 9;
    private static final int CHECKER_LEN_OFFSET = 13;

    private static final int MAX_CHECKER_FILE_NAME_LEN = 255;
    private static final int MAX_QUOTED_KEY_LEN = 64;

    private static final List<String> KNOWN_OS = List.of("linux", "darwin", "windows");
    private static final List<String> KNOWN_ARCH = List.of("amd64", "arm64");

    /** {@link UUID#fromString} alone is far too lenient. */
    private static final Pattern CANONICAL_UUID =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    /** Duplicate keys and trailing content are rejected: parsers disagree on which duplicate wins. */
    private static final ObjectMapper MANIFEST_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    /**
     * @throws CommunityGrantBundleFormatException with a message safe to show the uploader and to store in
     *                                             an audit row.
     */
    public static CommunityGrantOfflineBundle parse(byte[] data) {
        int total = data == null ? 0 : data.length;
        if (total < HEADER_LEN) {
            throw new CommunityGrantBundleFormatException(Reason.TOO_SHORT, "The uploaded file is " + total
                    + " bytes, shorter than the " + HEADER_LEN + "-byte enrollment bundle header, so it is not"
                    + " a ThingsBoard enrollment bundle.");
        }
        if (!Arrays.equals(data, 0, MAGIC.length, MAGIC, 0, MAGIC.length)) {
            throw new CommunityGrantBundleFormatException(Reason.BAD_MAGIC, "The uploaded file is not a"
                    + " ThingsBoard enrollment bundle: it does not carry the marker every bundle begins with.");
        }
        if (data[MAGIC.length] != VERSION_V1) {
            throw new CommunityGrantBundleFormatException(Reason.UNSUPPORTED_VERSION, "The enrollment bundle"
                    + " declares format version " + (data[MAGIC.length] & 0xFF) + ", and this deployment"
                    + " supports version " + VERSION_V1 + " only.");
        }
        // Unsigned: read into a long so 0xFFFFFFFF cannot wrap the length arithmetic.
        long manifestLen = readUnsignedInt(data, MANIFEST_LEN_OFFSET);
        long checkerLen = readUnsignedInt(data, CHECKER_LEN_OFFSET);
        if (manifestLen < MIN_MANIFEST_LEN || manifestLen > MAX_MANIFEST_LEN) {
            throw new CommunityGrantBundleFormatException(Reason.MANIFEST_LEN_RANGE, "The enrollment bundle"
                    + " declares a manifest of " + manifestLen + " bytes, outside the permitted range of "
                    + MIN_MANIFEST_LEN + " to " + MAX_MANIFEST_LEN + " bytes, so the file is damaged.");
        }
        if (checkerLen < MIN_CHECKER_LEN) {
            throw new CommunityGrantBundleFormatException(Reason.CHECKER_LEN_RANGE, "The enrollment bundle"
                    + " declares a checker of " + checkerLen + " bytes, so the file is damaged.");
        }
        long expectedTotal = HEADER_LEN + manifestLen + checkerLen;
        if (expectedTotal != total) {
            throw new CommunityGrantBundleFormatException(Reason.LENGTH_MISMATCH, "The enrollment bundle is"
                    + " damaged or truncated: its declared " + manifestLen + "-byte manifest and "
                    + checkerLen + "-byte checker need a file of " + expectedTotal + " bytes, but the"
                    + " uploaded file is " + total + " bytes. Download the bundle again.");
        }
        JsonNode manifest = readManifest(data, (int) manifestLen);
        return new CommunityGrantOfflineBundle(
                readEnum(manifest, "os", KNOWN_OS),
                readEnum(manifest, "arch", KNOWN_ARCH),
                readClusterId(manifest),
                readCheckerFileName(manifest),
                readIssuedAt(manifest),
                readTimestamp(manifest, "challengeExpiresAt"),
                readCheckerInput(manifest),
                Arrays.copyOfRange(data, RELSIG_OFFSET, RELSIG_OFFSET + RELSIG_LEN),
                Arrays.copyOfRange(data, HEADER_LEN + (int) manifestLen, total));
    }

    /** Strictly later: the portal still accepts the deadline instant itself. */
    public boolean isChallengeExpired(long now) {
        return now > challengeExpiresAt;
    }

    private static long readUnsignedInt(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    /** A strict decoder: a substituting one would report binary garbage as bad JSON rather than bad text. */
    private static JsonNode readManifest(byte[] data, int manifestLen) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer text;
        try {
            text = decoder.decode(ByteBuffer.wrap(data, HEADER_LEN, manifestLen));
        } catch (CharacterCodingException e) {
            throw new CommunityGrantBundleFormatException(Reason.MANIFEST_NOT_UTF8, "The enrollment bundle's"
                    + " manifest is not readable text, so the file is damaged. Download the bundle again.");
        }
        String manifestJson = text.toString();
        JsonNode manifest;
        try {
            manifest = MANIFEST_MAPPER.readTree(manifestJson);
        } catch (JsonParseException e) {
            throw notAJsonObject();
        } catch (IOException e) {
            // Not a syntax error, so the duplicate-key refusal; Jackson's message does not name the key reliably.
            String duplicate = findDuplicateKey(manifestJson);
            if (duplicate == null) {
                throw notAJsonObject();
            }
            throw fieldError(quoteKey(duplicate), "appears more than once");
        }
        if (manifest == null || !manifest.isObject()) {
            throw notAJsonObject();
        }
        int declaredVersion = readInt(manifest, "bundleVersion");
        if (declaredVersion != VERSION_V1) {
            throw fieldError("bundleVersion", "must be " + VERSION_V1);
        }
        return manifest;
    }

    /** Scans the root value only, so trailing junk is not blamed for a duplicate key. */
    private static String findDuplicateKey(String manifestJson) {
        Deque<Set<String>> seenPerObject = new ArrayDeque<>();
        try (JsonParser parser = MANIFEST_MAPPER.createParser(manifestJson)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token == JsonToken.START_OBJECT) {
                    seenPerObject.push(new HashSet<>());
                } else if (token == JsonToken.END_OBJECT) {
                    seenPerObject.pop();
                    if (seenPerObject.isEmpty()) {
                        break;
                    }
                } else if (token == JsonToken.FIELD_NAME && !seenPerObject.peek().add(parser.currentName())) {
                    return parser.currentName();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static CommunityGrantBundleFormatException notAJsonObject() {
        return new CommunityGrantBundleFormatException(Reason.MANIFEST_NOT_JSON, "The enrollment bundle's"
                + " manifest is not a JSON object, so the file is damaged. Download the bundle again.");
    }

    /** Never quotes the offending value: the message is shown to the uploader and audited. */
    private static CommunityGrantBundleFormatException fieldError(String key, String problem) {
        return new CommunityGrantBundleFormatException(Reason.MANIFEST_FIELD, "The enrollment bundle's"
                + " manifest is not usable: \"" + key + "\" " + problem + ". Download the bundle again.");
    }

    /** The one uploaded string a refusal quotes, so it is bounded and stripped of control characters. */
    private static String quoteKey(String key) {
        String quoted = key;
        if (quoted.length() > MAX_QUOTED_KEY_LEN) {
            // Never cut between the halves of a surrogate pair.
            int end = Character.isHighSurrogate(quoted.charAt(MAX_QUOTED_KEY_LEN - 1))
                    ? MAX_QUOTED_KEY_LEN - 1 : MAX_QUOTED_KEY_LEN;
            quoted = quoted.substring(0, end) + "...";
        }
        return quoted.replaceAll("\\p{Cntrl}", "?");
    }

    private static JsonNode required(JsonNode manifest, String key) {
        JsonNode value = manifest.get(key);
        if (value == null || value.isNull()) {
            throw fieldError(key, "is missing");
        }
        return value;
    }

    private static String readString(JsonNode manifest, String key) {
        JsonNode value = required(manifest, key);
        if (!value.isTextual()) {
            throw fieldError(key, "must be a string");
        }
        return value.textValue();
    }

    private static int readInt(JsonNode manifest, String key) {
        JsonNode value = required(manifest, key);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw fieldError(key, "must be a whole number");
        }
        return value.intValue();
    }

    private static long readTimestamp(JsonNode manifest, String key) {
        JsonNode value = required(manifest, key);
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw fieldError(key, "must be a whole number of milliseconds");
        }
        long millis = value.longValue();
        if (millis <= 0) {
            throw fieldError(key, "must be a positive number of milliseconds");
        }
        return millis;
    }

    private static String readEnum(JsonNode manifest, String key, List<String> permitted) {
        String value = readString(manifest, key);
        if (!permitted.contains(value)) {
            throw fieldError(key, "must be one of " + String.join(", ", permitted));
        }
        return value;
    }

    private static UUID readClusterId(JsonNode manifest) {
        String value = readString(manifest, "clusterId");
        if (!CANONICAL_UUID.matcher(value).matches()) {
            throw fieldError("clusterId", "must be a canonical lowercase UUID");
        }
        return UUID.fromString(value);
    }

    private static String readCheckerFileName(JsonNode manifest) {
        String value = readString(manifest, "checkerFileName");
        if (value.isEmpty() || value.length() > MAX_CHECKER_FILE_NAME_LEN) {
            throw fieldError("checkerFileName", "must be between 1 and " + MAX_CHECKER_FILE_NAME_LEN
                    + " characters");
        }
        // Logged verbatim, so control characters could forge log lines.
        if (value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || value.contains("..")
                || containsControlCharacter(value)) {
            throw fieldError("checkerFileName",
                    "must not contain a path separator, \"..\" or a control character");
        }
        return value;
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private static long readIssuedAt(JsonNode manifest) {
        long issuedAt = readTimestamp(manifest, "issuedAt");
        if (issuedAt > readTimestamp(manifest, "challengeExpiresAt")) {
            throw fieldError("issuedAt", "must not be later than the challenge's expiry");
        }
        return issuedAt;
    }

    private static CommunityGrantCheckerInput readCheckerInput(JsonNode manifest) {
        return CommunityGrantCheckerInput.from(manifest.get("checkerInput"))
                .orElseThrow(() -> fieldError("checkerInput", "must be a non-empty object"));
    }

}
