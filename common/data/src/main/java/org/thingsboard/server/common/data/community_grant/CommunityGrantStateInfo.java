// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.community_grant;

/**
 * Everything the widget is allowed to know: never a license key, an owner identity or a policy detail. Keep
 * the TypeScript mirror in {@code community-grant.models.ts} in step.
 */
public record CommunityGrantStateInfo(CommunityGrantMode mode,
                                      CommunityGrantState state,
                                      String signUpUrl,
                                      Long lastPolledAt,
                                      CommunityGrantParkReason parkReason,
                                      String portalUrl,
                                      String offlineReport,
                                      boolean offlineRunInProgress,
                                      String offlineRunError) {

    public static CommunityGrantStateInfo of(CommunityGrantFlowState flowState, String portalUrl) {
        return of(flowState, portalUrl, null, false, null);
    }

    public static CommunityGrantStateInfo of(CommunityGrantFlowState flowState, String portalUrl,
                                             String offlineReport, boolean offlineRunInProgress,
                                             String offlineRunError) {
        return new CommunityGrantStateInfo(flowState.getMode(), flowState.getState(),
                flowState.getSignUpUrl(), flowState.getLastPolledAt(), flowState.getParkReason(),
                portalUrl, offlineReport, offlineRunInProgress, offlineRunError);
    }

    /** Only the report and hand-off screens show the report, and only once a run has been recorded. */
    public static boolean carriesOfflineReport(CommunityGrantFlowState flowState) {
        CommunityGrantState state = flowState.getState();
        return (state == CommunityGrantState.VALIDATING || state == CommunityGrantState.REPORT_HANDED_OVER)
                && flowState.getLastOfflineCheckAt() != null;
    }

}
