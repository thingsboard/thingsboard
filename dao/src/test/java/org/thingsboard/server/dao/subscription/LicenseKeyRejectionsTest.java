// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

public class LicenseKeyRejectionsTest {

    /**
     * Every code the licence server can send, and the sentence it is answered with - the ones with a verdict of
     * their own and the ones that fall back alike. A code added to {@link LicenseErrorCode} later fails this
     * test until it is listed here, which is the point: answering a new failure with the generic "wasn't
     * recognized" is a decision about what an operator is told, and it should be made rather than defaulted to.
     */
    private static final Map<LicenseErrorCode, String> EXPECTED_SENTENCES = Map.ofEntries(
            entry(LicenseErrorCode.INVALID_LICENSE_SECRET, LicenseKeyRejections.NOT_RECOGNIZED),
            entry(LicenseErrorCode.SUBSCRIPTION_NOT_FOUND, LicenseKeyRejections.NOT_RECOGNIZED),
            entry(LicenseErrorCode.INVALID_OFFLINE_LICENSE_DATA_CHECK, LicenseKeyRejections.NOT_RECOGNIZED),
            entry(LicenseErrorCode.CLUSTER_ID_MISMATCH, LicenseKeyRejections.BOUND_ELSEWHERE),
            entry(LicenseErrorCode.INVALID_CLUSTER_ID_CHECK, LicenseKeyRejections.BOUND_ELSEWHERE),
            entry(LicenseErrorCode.SUBSCRIPTION_NOT_ACTIVE, LicenseKeyRejections.NOT_ACTIVE),
            entry(LicenseErrorCode.ACTIVE_INSTANCES_CAPACITY_EXCEEDED, LicenseKeyRejections.NO_AVAILABLE_INSTANCE),
            entry(LicenseErrorCode.UNSUPPORTED_SOFTWARE_VERSION, LicenseKeyRejections.VERSION_NOT_COVERED),
            entry(LicenseErrorCode.OFFLINE_LICENSE_ALREADY_ISSUED, LicenseKeyRejections.ALREADY_OFFLINE),
            entry(LicenseErrorCode.CONNECTION_ERROR, LicenseKeyRejections.PORTAL_UNREACHABLE),
            entry(LicenseErrorCode.INVALID_SERVER_CERTIFICATE, LicenseKeyRejections.PORTAL_UNREACHABLE),
            entry(LicenseErrorCode.CLUSTER_ID_NOT_FOUND, LicenseKeyRejections.NO_CLUSTER_IDENTITY),
            // The instance check reaches the apply path: a deployment whose data file still describes this key
            // re-checks its existing activation instead of activating anew, and these are the three ways the
            // portal can refuse that check.
            entry(LicenseErrorCode.INSTANCE_NOT_FOUND, LicenseKeyRejections.STALE_ACTIVATION),
            entry(LicenseErrorCode.INVALID_LICENSE_CHECK_SECRET, LicenseKeyRejections.STALE_ACTIVATION),
            entry(LicenseErrorCode.INSTANCE_NOT_ACTIVE, LicenseKeyRejections.STALE_ACTIVATION),
            // The fourth: the instance id read out of that same activation record was not one the licence
            // server would even look up.
            entry(LicenseErrorCode.INVALID_LICENSE_CHECK_REQUEST, LicenseKeyRejections.STALE_ACTIVATION),
            entry(LicenseErrorCode.GENERAL_SERVER_ERROR, LicenseKeyRejections.PORTAL_UNREACHABLE),
            entry(LicenseErrorCode.GENERAL_ERROR, LicenseKeyRejections.CHECK_NOT_COMPLETED),
            // Below: no sentence of its own. A claim is refused on the sign-up screen, which says its own
            // thing about it rather than sending the operator back to a key they never pasted.
            entry(LicenseErrorCode.CLAIM_LICENSE_REJECTED, LicenseKeyRejections.NOT_RECOGNIZED),
            // Neither can reach a key rejection: RATE_LIMITED is raised only on the claim-free-licence call,
            // and INSTANCE_REGISTRY_NOT_FOUND only by the offline client's periodic check, which reports to
            // the listener and so locks the node rather than answering an operator who pasted a key.
            entry(LicenseErrorCode.RATE_LIMITED, LicenseKeyRejections.NOT_RECOGNIZED),
            entry(LicenseErrorCode.INSTANCE_REGISTRY_NOT_FOUND, LicenseKeyRejections.NOT_RECOGNIZED));

    @Test
    public void everyMappedCodeGetsItsOwnSentence() {
        assertThat(EXPECTED_SENTENCES.keySet())
                .as("every LicenseErrorCode has to be accounted for here, whether it is mapped or falls back")
                .containsExactlyInAnyOrder(LicenseErrorCode.values());
        EXPECTED_SENTENCES.forEach((code, sentence) ->
                assertThat(messageFor(code)).as("%s", code).isEqualTo(sentence));
    }

    @Test
    public void noMessageLeaksAnExceptionText() {
        // The licence server's own message can name a subscription or a stack; none of it may reach a caller.
        LicenseException error = new LicenseException("Subscription 4f2b is not active at row 812!",
                LicenseErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        assertThat(LicenseKeyRejections.messageFor(error)).doesNotContain("4f2b").isEqualTo(LicenseKeyRejections.NOT_ACTIVE);
    }

    @Test
    public void anUnmappedFailureFallsBackRatherThanThrowing() {
        // Not a LicenseException at all, so there is no code to read: the caller still gets a sentence.
        assertThat(LicenseKeyRejections.messageFor(new IllegalStateException("boom")))
                .isEqualTo(LicenseKeyRejections.NOT_RECOGNIZED);
    }

    private static String messageFor(LicenseErrorCode code) {
        return LicenseKeyRejections.messageFor(new LicenseException("raw", code));
    }

}
