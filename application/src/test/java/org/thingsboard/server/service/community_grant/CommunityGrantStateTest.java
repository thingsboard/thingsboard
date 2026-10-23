// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityGrantStateTest {

    @Test
    void testTerminalAndPollableFlags() {
        assertThat(CommunityGrantState.REGISTERED.isTerminal()).isTrue();
        assertThat(CommunityGrantState.ALREADY_REGISTERED.isTerminal()).isTrue();
        assertThat(CommunityGrantState.WRONG_INSTALLATION.isTerminal()).isTrue();
        assertThat(CommunityGrantState.RESUME.isTerminal()).isFalse();
        assertThat(CommunityGrantState.RESUME.isPollable()).isFalse();
        assertThat(CommunityGrantState.NOT_STARTED.isPollable()).isFalse();
        assertThat(CommunityGrantState.AWAITING_SIGNUP.isPollable()).isTrue();
        assertThat(CommunityGrantState.COLLECTING.isPollable()).isTrue();
        assertThat(CommunityGrantState.VALIDATING.isPollable()).isTrue();
    }

    @Test
    void testUnderReviewIsPollableAndNotTerminal() {
        assertThat(CommunityGrantState.UNDER_REVIEW.isTerminal()).isFalse();
        assertThat(CommunityGrantState.UNDER_REVIEW.isPollable()).isTrue();
    }

    @Test
    void testTheHandOverIsPolledAndIsNotAnEnding() {
        assertThat(CommunityGrantState.REPORT_HANDED_OVER.isTerminal()).isFalse();
        assertThat(CommunityGrantState.REPORT_HANDED_OVER.isPollable()).isTrue();
        assertThat(CommunityGrantState.REPORT_HANDED_OVER.isAwaitingOrPastDecision()).isTrue();
    }

    @Test
    void testAwaitingOrPastDecisionCoversTheDecidedStatesAndUnderReview() {
        assertThat(CommunityGrantState.REGISTERED.isAwaitingOrPastDecision()).isTrue();
        assertThat(CommunityGrantState.ALREADY_REGISTERED.isAwaitingOrPastDecision()).isTrue();
        assertThat(CommunityGrantState.UNDER_REVIEW.isAwaitingOrPastDecision()).isTrue();
        assertThat(CommunityGrantState.VALIDATING.isAwaitingOrPastDecision()).isFalse();
        assertThat(CommunityGrantState.RESUME.isAwaitingOrPastDecision()).isFalse();
        assertThat(CommunityGrantState.NOT_STARTED.isAwaitingOrPastDecision()).isFalse();
    }

}
