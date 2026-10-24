// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.community_grant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * The persisted community grant state, stored as a single JSON document in {@code admin_settings}. The claim
 * token lives in {@code tb_cluster}, though the sign-up URL stored here quotes it.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommunityGrantFlowState implements Serializable {

    private static final long serialVersionUID = 1L;

    private CommunityGrantMode mode;
    private CommunityGrantState state;
    private String signUpUrl;
    /** When {@code start()} minted the current token; bounds how long an abandoned sign-up is polled. */
    private Long startedAt;
    private Long lastPolledAt;
    private Long lastOfflineCheckAt;
    /** When {@code requestAccess()} last attempted to reach the portal, successful or not; the cooldown's base. */
    private Long lastRequestAccessAt;

    /**
     * Set on {@code RESUME} only. Outside {@link #clearClaim()}: it is written by the same update that sets
     * {@code RESUME}.
     */
    private CommunityGrantParkReason parkReason;

    /** The last by-hand checker run that is running or failed. */
    private CommunityGrantOfflineRun offlineRun;

    /** Drops everything scoped to the current claim token. */
    public void clearClaim() {
        signUpUrl = null;
        startedAt = null;
    }

    public static CommunityGrantFlowState initial() {
        CommunityGrantFlowState flowState = new CommunityGrantFlowState();
        flowState.setMode(CommunityGrantMode.ONLINE);
        flowState.setState(CommunityGrantState.NOT_STARTED);
        return flowState;
    }

}
