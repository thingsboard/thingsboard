// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.CHECKER;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.OTHER_CHECKER;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.OTHER_SIGNATURE;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.PUBLIC_KEY_A;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.PUBLIC_KEY_B;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.SIGNATURE;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.SIGNATURE_FROM_WRONG_KEY_UNDER_KEY_ID_1;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.SIGNATURE_UNDER_KEY_ID_2;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.TRUSTS_A;
import static org.thingsboard.server.service.community_grant.CommunityGrantSigningFixtures.TRUSTS_A_AND_B;

class CommunityGrantReleaseSignatureVerifierTest {

    @Test
    void testAGenuineReleaseSignatureVerifies() {
        assertThatCode(() -> verifierTrusting(TRUSTS_A).verify(CHECKER, SIGNATURE)).doesNotThrowAnyException();
    }

    @Test
    void testASingleFlippedBitInTheBinaryIsRejected() {
        byte[] tampered = CHECKER.clone();
        tampered[10] ^= 0x01;

        assertRejected(TRUSTS_A, tampered, SIGNATURE, "does not match its release signature");
    }

    @Test
    void testATruncatedBinaryIsRejected() {
        byte[] truncated = Arrays.copyOf(CHECKER, CHECKER.length - 1);

        assertRejected(TRUSTS_A, truncated, SIGNATURE, "does not match its release signature");
    }

    @Test
    void testABinaryWithBytesAppendedIsRejected() {
        byte[] extended = Arrays.copyOf(CHECKER, CHECKER.length + 1);

        assertRejected(TRUSTS_A, extended, SIGNATURE, "does not match its release signature");
    }

    @Test
    void testASignatureForADifferentBinaryIsRejected() {
        assertRejected(TRUSTS_A, CHECKER, OTHER_SIGNATURE, "does not match its release signature");
        // The swapped signature still verifies against its own binary, so the rejection is about the pairing.
        assertThatCode(() -> verifierTrusting(TRUSTS_A).verify(OTHER_CHECKER, OTHER_SIGNATURE))
                .doesNotThrowAnyException();
    }

    @Test
    void testASignatureFromTheWrongPrivateKeyUnderATrustedKeyIdIsRejected() {
        assertRejected(TRUSTS_A, CHECKER, SIGNATURE_FROM_WRONG_KEY_UNDER_KEY_ID_1,
                "does not match its release signature");
    }

    // An all-ones S is not a valid encoding, so the provider throws instead of returning false.
    @Test
    void testASignatureWhoseEncodingTheProviderRejectsOutrightIsStillARejection() {
        byte[] unencodable = SIGNATURE.clone();
        Arrays.fill(unencodable, 42, 74, (byte) 0xFF);

        assertRejected(TRUSTS_A, CHECKER, unencodable, "does not match its release signature");
    }

    @Test
    void testACorruptedMagicIsRejected() {
        byte[] corrupted = SIGNATURE.clone();
        corrupted[3] = 'X';

        assertRejected(TRUSTS_A, CHECKER, corrupted, "not a ThingsBoard release signature");
    }

    @Test
    void testAnUnknownVersionIsRejected() {
        byte[] futureVersion = SIGNATURE.clone();
        futureVersion[8] = 0x02;

        assertRejected(TRUSTS_A, CHECKER, futureVersion, "version 2");
    }

    // The untrusted-key message also names key id 0, so the reserved phrase is asserted and the untrusted one excluded.
    @Test
    void testTheReservedKeyIdZeroIsRejectedAsReservedRatherThanAsUntrusted() {
        byte[] reservedKeyId = SIGNATURE.clone();
        reservedKeyId[9] = 0x00;

        for (String trustedKeys : new String[]{TRUSTS_A, TRUSTS_A_AND_B, ""}) {
            assertThatThrownBy(() -> verifierTrusting(trustedKeys).verify(CHECKER, reservedKeyId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reserved and never valid")
                    .hasMessageNotContaining("does not trust")
                    .hasMessageNotContaining(CommunityGrantReleaseSignatureVerifier.PUBLIC_KEYS_PROPERTY);
        }
    }

    @Test
    void testAKeyIdTheTrustStoreDoesNotHoldIsRejectedAsUntrusted() {
        byte[] unknownKeyId = SIGNATURE.clone();
        unknownKeyId[9] = 0x09;

        assertThatThrownBy(() -> verifierTrusting(TRUSTS_A).verify(CHECKER, unknownKeyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key id 9")
                .hasMessageContaining("does not trust")
                .hasMessageNotContaining("reserved");
        assertRejected(TRUSTS_A, CHECKER, SIGNATURE_UNDER_KEY_ID_2, "key id 2");
    }

    @Test
    void testASignatureFileOfAnyOtherLengthIsRejected() {
        assertRejected(TRUSTS_A, CHECKER, Arrays.copyOf(SIGNATURE, 73), "73 bytes");
        assertRejected(TRUSTS_A, CHECKER, Arrays.copyOf(SIGNATURE, 75), "75 bytes");
        assertRejected(TRUSTS_A, CHECKER, new byte[]{0x54}, "1 bytes");
    }

    @Test
    void testAnAbsentOrEmptySignatureFileIsRejected() {
        assertRejected(TRUSTS_A, CHECKER, null, "carries no release signature");
        assertRejected(TRUSTS_A, CHECKER, new byte[0], "carries no release signature");
    }

    @Test
    void testAnEmptyOrAbsentBinaryIsRejected() {
        assertRejected(TRUSTS_A, new byte[0], SIGNATURE, "empty");
        assertRejected(TRUSTS_A, null, SIGNATURE, "empty");
    }

    @Test
    void testNothingVerifiesWhenNoKeyIsConfigured() {
        for (String unconfigured : new String[]{"", "   ", null}) {
            assertThatThrownBy(() -> verifierTrusting(unconfigured).verify(CHECKER, SIGNATURE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(CommunityGrantReleaseSignatureVerifier.PUBLIC_KEYS_PROPERTY)
                    .hasMessageContaining("refuses");
        }
    }

    @Test
    void testAnUnparseablePropertyRefusesEveryUploadAndSaysSo() {
        assertThatThrownBy(() -> verifierTrusting("1:" + PUBLIC_KEY_A + ",2:not base64!").verify(CHECKER, SIGNATURE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CommunityGrantReleaseSignatureVerifier.PUBLIC_KEYS_PROPERTY)
                .hasMessageContaining("could not be parsed");
    }

    // init() runs once per bean in production; calling it again is the only way to observe a failed reparse.
    @Test
    void testAFailedReparseLeavesNothingTrustedRatherThanThePreviousKeys() {
        CommunityGrantReleaseSignatureVerifier verifier = verifierTrusting(TRUSTS_A);
        assertThatCode(() -> verifier.verify(CHECKER, SIGNATURE)).doesNotThrowAnyException();

        ReflectionTestUtils.setField(verifier, "checkerReleasePublicKeys", "1:" + PUBLIC_KEY_A + ",2:not base64!");
        verifier.init();

        assertThatThrownBy(() -> verifier.verify(CHECKER, SIGNATURE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not be parsed");
    }

    @Test
    void testATrustStoreWithTwoKeysAcceptsReleasesSignedUnderEither() {
        CommunityGrantReleaseSignatureVerifier verifier = verifierTrusting(TRUSTS_A_AND_B);

        assertThatCode(() -> verifier.verify(CHECKER, SIGNATURE)).doesNotThrowAnyException();
        assertThatCode(() -> verifier.verify(CHECKER, SIGNATURE_UNDER_KEY_ID_2)).doesNotThrowAnyException();
        // Each key id resolves to its own key.
        assertRejected(TRUSTS_A_AND_B, CHECKER, SIGNATURE_FROM_WRONG_KEY_UNDER_KEY_ID_1,
                "does not match its release signature");
    }

    @Test
    void testWhitespaceAroundEntriesIsTolerated() {
        assertThatCode(() -> verifierTrusting(" 1 : " + PUBLIC_KEY_A + " , 2 : " + PUBLIC_KEY_B + " ")
                .verify(CHECKER, SIGNATURE_UNDER_KEY_ID_2)).doesNotThrowAnyException();
    }

    @Test
    void testAKeyIdOutsideOneToTwoFiveFiveCannotBeConfigured() {
        assertUnparseable("0:" + PUBLIC_KEY_A, "reserved");
        assertUnparseable("256:" + PUBLIC_KEY_A, "1-255");
        assertUnparseable("-1:" + PUBLIC_KEY_A, "1-255");
    }

    @Test
    void testMalformedEntriesAreRefusedRatherThanSkipped() {
        assertUnparseable(PUBLIC_KEY_A, "keyId:base64PublicKey");
        assertUnparseable("one:" + PUBLIC_KEY_A, "not a number");
        assertUnparseable("1:" + PUBLIC_KEY_A + ",", "empty entry");
        assertUnparseable("1:" + PUBLIC_KEY_A + ",1:" + PUBLIC_KEY_B, "more than once");
        assertUnparseable("1:", "0 bytes");
        assertUnparseable("1:AAAA", "3 bytes");
    }

    @Test
    void testAnX509WrappedPublicKeyIsRefused() {
        byte[] raw = Base64.getDecoder().decode(PUBLIC_KEY_A);
        byte[] wrapped = new byte[12 + raw.length];
        System.arraycopy(new byte[]{0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00},
                0, wrapped, 0, 12);
        System.arraycopy(raw, 0, wrapped, 12, raw.length);

        assertUnparseable("1:" + Base64.getEncoder().encodeToString(wrapped), "44 bytes");
    }

    // KeyFactory accepts some off-curve values and only fails at verification time.
    @Test
    void testAKeyThatIsNotAValidEd25519PointIsRefused() {
        byte[] notAPoint = new byte[32];
        Arrays.fill(notAPoint, (byte) 0xFF);

        assertUnparseable("1:" + Base64.getEncoder().encodeToString(notAPoint), "not a usable Ed25519 public key");
    }

    @Test
    void testRefusalMessagesCarryNoKeyMaterialOrUploadedBytes() {
        byte[][] rejectedSignatures = {null, new byte[0], Arrays.copyOf(SIGNATURE, 75), OTHER_SIGNATURE,
                SIGNATURE_FROM_WRONG_KEY_UNDER_KEY_ID_1, SIGNATURE_UNDER_KEY_ID_2};
        for (byte[] rejected : rejectedSignatures) {
            assertThatThrownBy(() -> verifierTrusting(TRUSTS_A).verify(CHECKER, rejected))
                    .isInstanceOf(IllegalArgumentException.class)
                    .satisfies(e -> {
                        assertThat(e.getMessage()).doesNotContain(PUBLIC_KEY_A);
                        assertThat(e.getMessage()).doesNotContain(PUBLIC_KEY_B);
                        assertThat(e.getMessage()).doesNotContain("#!/bin/sh");
                    });
        }
    }

    private static void assertRejected(String trustedKeys, byte[] checker, byte[] signature, String expectedInMessage) {
        assertThatThrownBy(() -> verifierTrusting(trustedKeys).verify(checker, signature))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedInMessage);
    }

    private static void assertUnparseable(String configured, String expectedInMessage) {
        assertThatThrownBy(() -> CommunityGrantReleaseSignatureVerifier.parsePublicKeys(configured))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedInMessage);
    }

    private static CommunityGrantReleaseSignatureVerifier verifierTrusting(String configured) {
        CommunityGrantReleaseSignatureVerifier verifier = new CommunityGrantReleaseSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "checkerReleasePublicKeys", configured);
        verifier.init();
        return verifier;
    }

}
