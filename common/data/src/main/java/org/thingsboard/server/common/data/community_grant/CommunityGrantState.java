// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.community_grant;

/**
 * The instance-local view of the community grant flow, largely a projection of the portal's answer.
 */
public enum CommunityGrantState {

    NOT_STARTED,
    AWAITING_SIGNUP,
    COLLECTING,
    VALIDATING,
    /**
     * The operator handed a by-hand report to the portal. A wait, not an ending: where the portal is
     * reachable a later poll resolves it; offline, the answer is read at the portal.
     */
    REPORT_HANDED_OVER,
    REGISTERED,
    UNDER_REVIEW,
    /** This cluster id is already enrolled by somebody else, found by the probe before any token is minted. */
    ALREADY_REGISTERED,
    /** The polled claim token is bound to a different installation; this one is not registered at all. */
    WRONG_INSTALLATION,
    RESUME;

    /** A state the flow never leaves on its own. Nothing polls from here and no token is held past it. */
    public boolean isTerminal() {
        return this == REGISTERED || this == ALREADY_REGISTERED || this == WRONG_INSTALLATION;
    }

    /** A state from which the portal is worth asking again. {@link #RESUME}'s token is already gone. */
    public boolean isPollable() {
        return this == AWAITING_SIGNUP || this == COLLECTING || this == VALIDATING || this == UNDER_REVIEW
                || this == REPORT_HANDED_OVER;
    }

    /**
     * A state no local action may move the flow out of: the portal has decided, or still owes a decision on a
     * registration or report it already has.
     */
    public boolean isAwaitingOrPastDecision() {
        return isTerminal() || this == UNDER_REVIEW || this == REPORT_HANDED_OVER;
    }

}
