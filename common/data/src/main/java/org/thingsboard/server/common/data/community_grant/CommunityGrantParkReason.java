// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.community_grant;

/**
 * Why a flow parked at {@link CommunityGrantState#RESUME}.
 */
public enum CommunityGrantParkReason {

    /** The portal says the claim token expired. */
    LINK_EXPIRED,
    /**
     * The sign-up form was never completed within this deployment's own deadline, which also ends an
     * enrollment whose portal was never reachable.
     */
    SIGNUP_ABANDONED,
    /** The instance checker failed its whole retry budget. Retryable. */
    CHECK_FAILED,
    /** The portal stopped answering for the whole failure budget after sign-up. Retryable. */
    PORTAL_UNREACHABLE
}
