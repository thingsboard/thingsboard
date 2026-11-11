// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.license.shared.SubscriptionData;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;

/**
 * One reading of a licence, whether it is the one this node runs on or one a pasted key would install.
 * Keyed on plan data rather than on a client, because a previewed key has no client.
 */
public final class SubscriptionInfoMapper {

    private SubscriptionInfoMapper() {
    }

    public static SubscriptionInfo toSubscriptionInfo(PlanData planData, SubscriptionData subscriptionData,
                                                      int licenseVersion, boolean offline) {
        SubscriptionInfo info = new SubscriptionInfo();
        if (subscriptionData != null) {
            info.setSubscriptionId(subscriptionData.getSubscriptionId());
            info.setSubscriptionPlanName(subscriptionData.getSubscriptionPlanName());
            info.setPlanUiType(subscriptionData.getPlanUiType());
            info.setPerpetual(subscriptionData.isPerpetual());
            info.setCurrentPeriodStartTs(subscriptionData.getCurrentPeriodStartTs());
            info.setCurrentPeriodEndTs(subscriptionData.getCurrentPeriodEndTs());
            Long endTs = subscriptionData.getEndTs();
            // The unlimited-horizon sentinel is not a date; null is what this field already means "no horizon" with.
            info.setEndTs(endTs != null && PlanDataConstants.isUnlimitedHorizon(endTs) ? null : endTs);
            info.setUpcomingInvoiceDate(subscriptionData.getUpcomingInvoiceDate());
            info.setUpcomingInvoiceAmountDue(subscriptionData.getUpcomingInvoiceAmountDue());
            info.setPlanExtraDeviceEnabled(subscriptionData.isPlanExtraDeviceEnabled());
            info.setPlanExtraAiCreditsEnabled(subscriptionData.isPlanExtraAiCreditsEnabled());
            info.setPlanExtraAgentEnabled(subscriptionData.isPlanExtraAgentEnabled());
            info.setPlanExtraInstanceEnabled(subscriptionData.isPlanExtraInstanceEnabled());
            info.setPlanEdgeEnabled(subscriptionData.isPlanEdgeEnabled());
            info.setPlanExtraEdgeEnabled(subscriptionData.isPlanExtraEdgeEnabled());
            info.setPlanTrendzEnabled(subscriptionData.isPlanTrendzEnabled());
            info.setInstancesCount(subscriptionData.getActiveProdInstancesCount());
        }
        info.setOffline(offline);
        info.setMaxDevices(reportedQuota(planData, PlanDataConstants.MAX_DEVICES_KEY, licenseVersion));
        info.setMaxAssets(reportedQuota(planData, PlanDataConstants.MAX_ASSETS_KEY, licenseVersion));
        info.setMaxEdges(reportedQuota(planData, PlanDataConstants.MAX_EDGES_KEY, licenseVersion));
        info.setMaxAgents(reportedQuota(planData, PlanDataConstants.MAX_AGENTS_KEY, licenseVersion));
        // Verbatim: the instance quota already stores unlimited as the sentinel and none as 0, and unlike the
        // entity quotas it has no v1 rule reading an absent key as unlimited.
        info.setMaxInstances(longValue(planData, PlanDataConstants.MAX_INSTANCES_KEY));
        info.setMaxAiCredits(reportedAiCredits(planData));
        info.setWhiteLabelingEnabled(booleanValue(planData, PlanDataConstants.WHITELABELING_KEY));
        info.setEdgeEnabled(booleanValue(planData, PlanDataConstants.EDGE_KEY));
        info.setTrendzEnabled(booleanValue(planData, PlanDataConstants.TRENDZ_KEY));
        info.setDevelopment(booleanValue(planData, PlanDataConstants.DEVELOPMENT_KEY));
        info.setCommunityGrantLicense(booleanValue(planData, CommunityGrantPlan.COMMUNITY_GRANT_KEY));
        return info;
    }

    /**
     * The quota as the API reports it: the real limit, or {@link PlanDataConstants#UNLIMITED_QUOTA} when
     * unlimited. Unlimited and "none" are opposite grants, so they must not both arrive as 0.
     */
    public static long reportedQuota(PlanData planData, String key, int licenseVersion) {
        return reportedQuota(longValue(planData, key), licenseVersion);
    }

    /**
     * The same, once the limit has already been read off the plan. Private: {@code BasicSubscriptionService}
     * has a {@code reportedQuota} of its own whose unlimited sentinel is the opposite of this one's, and the
     * two must not be reachable from the same place.
     */
    private static long reportedQuota(long limit, int licenseVersion) {
        return isUnlimited(limit, licenseVersion) ? PlanDataConstants.UNLIMITED_QUOTA : limit;
    }

    /**
     * The AI credit grant, in credits rather than in the 1M-credit packs plan data counts: used credits are
     * reported raw, and a grant in packs beside them would read as "3 / 1".
     * <p>
     * Clamped to the same {@link Integer#MAX_VALUE} the licence server clamps the minted AI token to, so the
     * reported grant can never promise more than the token allows. No unlimited sentinel: a non-positive pack
     * count mints no token at all, which is no AI rather than unlimited AI.
     */
    private static long reportedAiCredits(PlanData planData) {
        long creditPacks = longValue(planData, PlanDataConstants.MAX_AI_CREDITS_KEY);
        if (creditPacks <= 0) {
            return 0;
        }
        // Multiplied only below the clamp, so an absurd pack count cannot overflow the product.
        return creditPacks > Integer.MAX_VALUE / PlanDataConstants.AI_CREDITS_UNIT
                ? Integer.MAX_VALUE
                : creditPacks * PlanDataConstants.AI_CREDITS_UNIT;
    }

    /**
     * The single definition of "unlimited" for the device, asset and edge quotas. A v1 licence predates these
     * keys, so an absent or zero quota means unlimited; a v2 licence records every quota explicitly, so zero
     * means zero and only a negative value grants unlimited.
     */
    public static boolean isUnlimited(long limit, int licenseVersion) {
        if (limit < 0) {
            return true;
        }
        return licenseVersion < 2 && limit == 0;
    }

    public static long longValue(PlanData planData, String key) {
        PlanItem item = planData != null ? planData.get(key) : null;
        return item != null && item.getValue() != null && item.getValue().isNumber() ? item.getValue().asLong() : 0L;
    }

    public static boolean booleanValue(PlanData planData, String key) {
        PlanItem item = planData != null ? planData.get(key) : null;
        return item != null && item.getValue() != null && item.getValue().asBoolean(false);
    }

}
