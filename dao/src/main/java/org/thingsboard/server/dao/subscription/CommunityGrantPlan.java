// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.license.shared.PlanDataConstants;

/**
 * The plan-data key the licence portal sets on the plans the additional device- and asset-count enforcement
 * applies to.
 * <p>
 * The key is taken from the licence client library, so a portal-side rename that reaches a client release
 * turns into a compile error here rather than into every grant licence silently ceasing to be recognised.
 */
public final class CommunityGrantPlan {

    /** Set by the licence portal on plans this enforcement applies to. Absent on every other plan. */
    public static final String COMMUNITY_GRANT_KEY = PlanDataConstants.COMMUNITY_GRANT_KEY;

    private CommunityGrantPlan() {
    }

}
