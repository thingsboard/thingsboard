// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.subscription;

import lombok.Data;

@Data
public class SubscriptionInfo {

    private String subscriptionId;
    private String subscriptionPlanName;
    private String planUiType;
    private boolean isPerpetual;
    private boolean isOffline;
    private Long currentPeriodStartTs;
    private Long currentPeriodEndTs;
    private Long endTs;
    private Long upcomingInvoiceDate;
    private Long upcomingInvoiceAmountDue;
    private boolean planExtraDeviceEnabled;
    private boolean planEdgeEnabled;
    private boolean planExtraEdgeEnabled;
    private boolean planTrendzEnabled;
    private boolean planExtraAiCreditsEnabled;
    private boolean planExtraInstanceEnabled;
    private boolean planExtraAgentEnabled;

    private long dataTs;
    private String licenseServerEndpoint;

    /**
     * Entity quotas this licence covers, and the production instances it covers. Each is {@code -1} when the
     * licence grants it without limit, 0 when it grants none, and the cap itself otherwise: unlimited and
     * none are opposite grants and a consumer has to be able to tell them apart.
     */
    private long maxDevices;
    private long maxAssets;
    private long maxEdges;
    private long maxAgents;
    private long maxInstances;
    /** The AI credit grant, in credits rather than in the 1M-credit packs plan data counts. */
    private long maxAiCredits;
    private boolean whiteLabelingEnabled;
    private boolean edgeEnabled;
    private boolean trendzEnabled;
    private boolean development;
    /**
     * Whether this instance runs keyless, in non-production mode, with no subscription behind it at all.
     * Narrower than {@link #development}, which is also true of a real Development-plan licence: here there is
     * nothing to manage, nothing to refresh against and no licence timestamp, since no licence server was ever
     * contacted.
     */
    private boolean nonProduction;

    private long devicesCount;
    private long assetsCount;
    private long edgesCount;
    private long agentsCount;
    /**
     * Production instances active on this subscription, as the licence server counts them - the same
     * figure the instance quota is enforced against, not this cluster's node count. The two drift while a
     * node is starting but has not activated, and while a slot is still held by a node that died.
     */
    private long instancesCount;
    private long usedAiCredits;

    private boolean isCommunityGrantLicense;

}
