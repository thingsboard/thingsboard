// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.community_grant.CommunityGrantBundleFormatException.Reason;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.CHALLENGE;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.CHALLENGE_EXPIRES_AT;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.CHECKER_FILE_NAME;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.CLUSTER_ID;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.ISSUED_AT;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.KEY_ID;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.PORTAL_PUBLIC_KEY;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.REFERENCE_MILLIS;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.bundle;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.frame;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.relsig;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.standInChecker;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.validCheckerInput;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.validManifest;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.vector;
import static org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures.vectorsManifest;

/**
 * Vectors 05-07 are malformed in two ways at once, so they pin the order of the checks as well as the reasons.
 */
class CommunityGrantOfflineBundleTest {

    private static final String CHECKER_SHA256 =
            "39e3d7b6b5d075d37d053ad89b24b41bef4f3c29760c84447cab3f3be1882241";

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenVectors")
    void testEveryGoldenVectorHasItsRecordedSizeDigestAndOutcome(String fileName, int sizeBytes, String sha256,
                                                                    String outcome, String reason) {
        byte[] data = vector(fileName);

        assertThat(data).hasSize(sizeBytes);
        assertThat(HexFormat.of().formatHex(sha256(data))).isEqualTo(sha256);
        if ("REJECT".equals(outcome)) {
            assertThat(reasonOf(data)).isEqualTo(Reason.valueOf(reason.substring("E_".length())));
        } else {
            assertThatCode(() -> CommunityGrantOfflineBundle.parse(data)).doesNotThrowAnyException();
        }
    }

    @Test
    void testAFileShorterThanTheHeaderIsRefusedBeforeAnythingIsRead() {
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(vector("02-too-short.tbicb")))
                .isInstanceOf(CommunityGrantBundleFormatException.class)
                .hasMessageContaining("The uploaded file is 90 bytes")
                .hasMessageContaining("shorter than the 91-byte enrollment bundle header");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 90})
    void testNoInputAndAnEmptyInputAreRefusedOnTheLengthFloor(int size) {
        assertThat(reasonOf(new byte[size])).isEqualTo(Reason.TOO_SHORT);
        assertThat(reasonOf(null)).isEqualTo(Reason.TOO_SHORT);
    }

    @Test
    void testTheVersionRefusalNamesBothTheDeclaredAndTheSupportedVersion() {
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(vector("04-unknown-version.tbicb")))
                .hasMessageContaining("declares format version 2")
                .hasMessageContaining("supports version 1 only");
    }

    @ParameterizedTest
    @CsvSource({"05-manifest-len-zero.tbicb, 0", "06-manifest-len-too-large.tbicb, 8193"})
    void testTheManifestLengthRefusalNamesTheDeclaredValueAndThePermittedRange(String fileName, String declared) {
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(vector(fileName)))
                .hasMessageContaining("declares a manifest of " + declared + " bytes")
                .hasMessageContaining("permitted range of 1 to 8192 bytes");
    }

    @Test
    void testTheCheckerLengthRefusalNamesTheDeclaredValue() {
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(vector("07-checker-len-zero.tbicb")))
                .hasMessageContaining("declares a checker of 0 bytes");
    }

    // Exact, not at-least: otherwise anything could be appended to a genuine bundle.
    @ParameterizedTest
    @CsvSource({"08-truncated-mid-manifest.tbicb, 301", "09-truncated-mid-checker.tbicb, 543",
            "10-trailing-byte.tbicb, 576"})
    void testTheExactnessRefusalNamesBothTheExpectedAndTheActualTotal(String fileName, String actualTotal) {
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(vector(fileName)))
                .hasMessageContaining("need a file of 575 bytes")
                .hasMessageContaining("uploaded file is " + actualTotal + " bytes");
    }

    // Decoding with new String(bytes, UTF_8) would substitute replacement characters and report bad JSON instead.
    @Test
    void testAManifestThatIsNotUtf8IsRefusedAsNotUtf8RatherThanAsNotJson() {
        assertThat(reasonOf(vector("11-manifest-not-utf8.tbicb"))).isEqualTo(Reason.MANIFEST_NOT_UTF8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1,2,3]", "\"x\"", "7", "null", "true"})
    void testAManifestThatIsNotAJsonObjectIsRefused(String manifestJson) {
        assertThat(reasonOf(bundle(manifestJson))).isEqualTo(Reason.MANIFEST_NOT_JSON);
    }

    @Test
    void testAMissingManifestFieldIsRefusedByName() {
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(vector("13-manifest-missing-field.tbicb")))
                .hasMessageContaining("clusterId");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("manifestsBrokenOneRuleAtATime")
    void testEveryManifestRuleIsEnforcedAndNamesItsOwnKey(String description, JsonNode manifest, String key) {
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(bundle(manifest)))
                .isInstanceOf(CommunityGrantBundleFormatException.class)
                .hasMessageContaining(key);
        assertThat(reasonOf(bundle(manifest))).isEqualTo(Reason.MANIFEST_FIELD);
    }

    // Parsers disagree about which duplicate wins, so the two ends could read different values.
    @Test
    void testADuplicateManifestKeyIsRefusedByName() {
        String manifestJson = JacksonUtil.toString(validManifest())
                .replace("\"checkerInput\":", "\"clusterId\":\"" + CLUSTER_ID + "\",\"checkerInput\":");

        assertThat(reasonOf(bundle(manifestJson))).isEqualTo(Reason.MANIFEST_FIELD);
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(bundle(manifestJson)))
                .hasMessageContaining("clusterId");
    }

    @Test
    void testTheReaderDoesNotDependOnTheWritersManifestEncoding() {
        ObjectNode reordered = JacksonUtil.newObjectNode();
        List<String> keys = new ArrayList<>();
        validManifest().fieldNames().forEachRemaining(keys::add);
        for (int i = keys.size() - 1; i >= 0; i--) {
            reordered.set(keys.get(i), validManifest().get(keys.get(i)));
        }
        String reformatted = reordered.toPrettyString() + "\n";

        assertThat(CommunityGrantOfflineBundle.parse(bundle(reformatted)))
                .usingRecursiveComparison()
                .isEqualTo(CommunityGrantOfflineBundle.parse(vector("01-valid.tbicb")));
    }

    @Test
    void testAnUnknownManifestKeyIsIgnored() {
        ObjectNode manifest = validManifest();
        manifest.put("supportRef", "abc");
        manifest.putObject("futureDetail").put("nested", 1);
        manifest.putArray("futureList").add(1).add(2);

        assertThat(CommunityGrantOfflineBundle.parse(bundle(manifest)))
                .usingRecursiveComparison()
                .isEqualTo(CommunityGrantOfflineBundle.parse(vector("01-valid.tbicb")));
    }

    @Test
    void testBothDeclaredLengthsAreReadAsUnsigned() {
        byte[] manifest = JacksonUtil.toString(validManifest()).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(
                frame((byte) 1, manifest.length, 0xFFFFFFFFL, relsig(), manifest, standInChecker())))
                .hasMessageContaining("4294967295")
                .hasMessageNotContaining("-1");
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(
                frame((byte) 1, 0xFFFFFFFFL, standInChecker().length, relsig(), manifest, standInChecker())))
                .hasMessageContaining("4294967295")
                .hasMessageNotContaining("-1");
    }

    @Test
    void testADeclaredLengthNeverSizesAnAllocation() {
        byte[] manifest = {'{'};

        assertThat(reasonOf(frame((byte) 1, 1, Integer.MAX_VALUE, relsig(), manifest, new byte[0])))
                .isEqualTo(Reason.LENGTH_MISMATCH);
    }

    @Test
    void testACorruptSignatureParsesAndIsHandedOnUnchanged() {
        byte[] data = vector("15-relsig-not-tbicsig1.tbicb");

        CommunityGrantOfflineBundle parsed = CommunityGrantOfflineBundle.parse(data);

        assertThat(parsed.signature()).hasSize(74).isEqualTo(Arrays.copyOfRange(data, 17, 91));
        assertThat(parsed.signature()).isNotEqualTo(relsig());
    }

    @Test
    void testAnExpiredChallengeParsesAndReportsItselfOnlyAsAdvisory() {
        CommunityGrantOfflineBundle parsed = CommunityGrantOfflineBundle.parse(vector("14-expired-challenge.tbicb"));

        assertThat(parsed.challengeExpiresAt()).isEqualTo(1609545600000L);
        assertThat(parsed.isChallengeExpired(System.currentTimeMillis())).isTrue();
        assertThat(parsed.checkerInput()).isEqualTo(goldenCheckerInput());
    }

    @Test
    void testTheExpiryBoundaryInstantIsStillLive() {
        CommunityGrantOfflineBundle parsed = CommunityGrantOfflineBundle.parse(vector("01-valid.tbicb"));

        assertThat(parsed.isChallengeExpired(CHALLENGE_EXPIRES_AT - 1)).isFalse();
        assertThat(parsed.isChallengeExpired(CHALLENGE_EXPIRES_AT)).isFalse();
        assertThat(parsed.isChallengeExpired(CHALLENGE_EXPIRES_AT + 1)).isTrue();
    }

    @Test
    void testTheGoldenBundleRoundTripsEveryValueItCarries() {
        CommunityGrantOfflineBundle parsed = CommunityGrantOfflineBundle.parse(vector("01-valid.tbicb"));

        assertThat(parsed.os()).isEqualTo("linux");
        assertThat(parsed.arch()).isEqualTo("amd64");
        assertThat(parsed.clusterId()).isEqualTo(UUID.fromString(CLUSTER_ID));
        assertThat(parsed.checkerFileName()).isEqualTo(CHECKER_FILE_NAME);
        assertThat(parsed.issuedAt()).isEqualTo(ISSUED_AT);
        assertThat(parsed.challengeExpiresAt()).isEqualTo(CHALLENGE_EXPIRES_AT);
        assertThat(parsed.checkerInput()).isEqualTo(goldenCheckerInput());
        assertThat(parsed.signature()).isEqualTo(relsig());
        assertThat(parsed.checker()).isEqualTo(standInChecker());
        assertThat(HexFormat.of().formatHex(sha256(parsed.checker()))).isEqualTo(CHECKER_SHA256);
    }

    @Test
    void testTheSynthesizedManifestIsTheOneTheVectorsRecord() {
        JsonNode material = vectorsManifest().get("fixtureMaterial");

        assertThat(JacksonUtil.toString(validManifest())).isEqualTo(material.get("validManifestJson").asText());
        assertThat(material.get("referenceMillis").asLong()).isEqualTo(REFERENCE_MILLIS);
    }

    @Test
    void testTheCheckerInputIsHandedOnUnreadAndCompact() {
        ObjectNode checkerInput = validCheckerInput();
        checkerInput.put("keyId", 0);
        checkerInput.put("publicKey", "not base64!!");
        checkerInput.remove("challenge");
        checkerInput.put("futureInput", true);
        String manifestJson = JacksonUtil.toString(validManifest().set("checkerInput", checkerInput))
                .replace("\"futureInput\":", "\n  \"futureInput\" : ");

        CommunityGrantOfflineBundle parsed = CommunityGrantOfflineBundle.parse(bundle(manifestJson));

        assertThat(parsed.checkerInput().json()).isEqualTo("{\"clusterId\":\"" + CLUSTER_ID + "\",\"keyId\":0,"
                + "\"publicKey\":\"not base64!!\",\"referenceMillis\":" + REFERENCE_MILLIS + ",\"futureInput\":true}");
    }

    @Test
    void testADuplicateKeyInsideTheCheckerInputIsRefusedByName() {
        String manifestJson = JacksonUtil.toString(validManifest())
                .replace("\"referenceMillis\":", "\"keyId\":2,\"referenceMillis\":");

        assertThat(reasonOf(bundle(manifestJson))).isEqualTo(Reason.MANIFEST_FIELD);
        assertThatThrownBy(() -> CommunityGrantOfflineBundle.parse(bundle(manifestJson)))
                .hasMessageContaining("keyId");
    }

    // The checker size cap is applied by CommunityGrantReportRunner to the extracted checker, not by the parser.
    @Test
    void testABundleLargerThanTheCheckerCapStillParses() {
        byte[] oversized = new byte[33554433];
        Arrays.fill(oversized, (byte) 7);
        byte[] manifest = JacksonUtil.toString(validManifest()).getBytes(StandardCharsets.UTF_8);

        CommunityGrantOfflineBundle parsed = CommunityGrantOfflineBundle.parse(
                frame((byte) 1, manifest.length, oversized.length, relsig(), manifest, oversized));

        assertThat(parsed.checker()).hasSize(oversized.length);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenVectors")
    void testNoRefusalMessageIsUnsafeToPublish(String fileName, int sizeBytes, String sha256, String outcome) {
        if (!"REJECT".equals(outcome)) {
            return;
        }
        String message = messageOf(vector(fileName));

        assertThat(message.toLowerCase()).doesNotContain("password", "secret", "token", "postgres://", "jdbc:");
        assertThat(message).doesNotContain(PORTAL_PUBLIC_KEY, CHALLENGE, CLUSTER_ID);
    }

    @Test
    void testTheExtractedMembersVerifyAgainstEachOtherThroughTheRealVerifier() {
        CommunityGrantOfflineBundle parsed = CommunityGrantOfflineBundle.parse(vector("01-valid.tbicb"));

        assertThatCode(() -> verifierTrusting(CommunityGrantBundleFixtures.TRUSTS_RELEASE_KEY)
                .verify(parsed.checker(), parsed.signature())).doesNotThrowAnyException();
        assertThatThrownBy(() -> verifierTrusting(CommunityGrantSigningFixtures.TRUSTS_A)
                .verify(parsed.checker(), parsed.signature()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static CommunityGrantReleaseSignatureVerifier verifierTrusting(String configured) {
        CommunityGrantReleaseSignatureVerifier verifier = new CommunityGrantReleaseSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "checkerReleasePublicKeys", configured);
        verifier.init();
        return verifier;
    }

    private static Stream<Arguments> goldenVectors() {
        List<Arguments> vectors = new ArrayList<>();
        for (JsonNode fixture : vectorsManifest().get("fixtures")) {
            vectors.add(Arguments.of(fixture.get("file").asText(), fixture.get("sizeBytes").asInt(),
                    fixture.get("sha256").asText(), fixture.get("expectedOutcome").asText(),
                    fixture.get("expectedReason").asText(null)));
        }
        return vectors.stream();
    }

    private static Stream<Arguments> manifestsBrokenOneRuleAtATime() {
        List<Arguments> cases = new ArrayList<>();
        List<String> keys = List.of("bundleVersion", "os", "arch", "clusterId", "checkerInput",
                "challengeExpiresAt", "issuedAt", "checkerFileName");
        for (String key : keys) {
            cases.add(Arguments.of(key + " missing", withoutKey(key), key));
            cases.add(Arguments.of(key + " of the wrong JSON type", withValue(key,
                    validManifest().get(key).isTextual() ? IntNode.valueOf(1) : TextNode.valueOf("x")), key));
        }
        cases.add(Arguments.of("bundleVersion disagreeing with the version byte", withInt("bundleVersion", 2), "bundleVersion"));
        cases.add(Arguments.of("an unknown os", withText("os", "solaris"), "os"));
        cases.add(Arguments.of("an unknown arch", withText("arch", "riscv64"), "arch"));
        cases.add(Arguments.of("a clusterId that is not a UUID", withText("clusterId", "not-a-uuid"), "clusterId"));
        cases.add(Arguments.of("an uppercase clusterId", withText("clusterId", CLUSTER_ID.toUpperCase(Locale.ROOT)), "clusterId"));
        cases.add(Arguments.of("an empty checkerInput", withValue("checkerInput", JacksonUtil.newObjectNode()), "checkerInput"));
        cases.add(Arguments.of("a checkerInput that is an array", withValue("checkerInput", JacksonUtil.newArrayNode().add(1)), "checkerInput"));
        cases.add(Arguments.of("a null checkerInput", withValue("checkerInput", NullNode.getInstance()), "checkerInput"));
        cases.add(Arguments.of("challengeExpiresAt of zero", withLong("challengeExpiresAt", 0), "challengeExpiresAt"));
        cases.add(Arguments.of("a negative challengeExpiresAt", withLong("challengeExpiresAt", -1), "challengeExpiresAt"));
        cases.add(Arguments.of("issuedAt of zero", withLong("issuedAt", 0), "issuedAt"));
        cases.add(Arguments.of("issuedAt after the expiry", withLong("issuedAt", CHALLENGE_EXPIRES_AT + 1), "issuedAt"));
        cases.add(Arguments.of("an empty checkerFileName", withText("checkerFileName", ""), "checkerFileName"));
        cases.add(Arguments.of("a checkerFileName over 255 characters", withText("checkerFileName", "x".repeat(256)), "checkerFileName"));
        cases.add(Arguments.of("a checkerFileName carrying a path", withText("checkerFileName", "bin/tb-instance-check"), "checkerFileName"));
        cases.add(Arguments.of("a checkerFileName carrying a windows path", withText("checkerFileName", "bin\\tb-instance-check"), "checkerFileName"));
        cases.add(Arguments.of("a checkerFileName carrying a traversal", withText("checkerFileName", "../tb-instance-check"), "checkerFileName"));
        return cases.stream();
    }

    private static JsonNode withoutKey(String key) {
        ObjectNode manifest = validManifest();
        manifest.remove(key);
        return manifest;
    }

    private static JsonNode withValue(String key, JsonNode value) {
        return validManifest().set(key, value);
    }

    private static JsonNode withText(String key, String value) {
        return validManifest().put(key, value);
    }

    private static JsonNode withInt(String key, int value) {
        return validManifest().put(key, value);
    }

    private static JsonNode withLong(String key, long value) {
        return validManifest().put(key, value);
    }

    private static CommunityGrantCheckerInput goldenCheckerInput() {
        return new CommunityGrantCheckerInput("{\"clusterId\":\"" + CLUSTER_ID + "\",\"keyId\":" + KEY_ID
                + ",\"publicKey\":\"" + PORTAL_PUBLIC_KEY + "\",\"challenge\":\"" + CHALLENGE
                + "\",\"referenceMillis\":" + REFERENCE_MILLIS + "}");
    }

    private static Reason reasonOf(byte[] data) {
        try {
            CommunityGrantOfflineBundle.parse(data);
        } catch (CommunityGrantBundleFormatException e) {
            return e.getReason();
        }
        throw new AssertionError("Expected the bundle to be refused, but it parsed");
    }

    private static String messageOf(byte[] data) {
        try {
            CommunityGrantOfflineBundle.parse(data);
        } catch (CommunityGrantBundleFormatException e) {
            return e.getMessage();
        }
        throw new AssertionError("Expected the bundle to be refused, but it parsed");
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
