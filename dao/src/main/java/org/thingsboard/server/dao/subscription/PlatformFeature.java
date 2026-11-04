// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.Getter;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;

/**
 * The professional feature set: platform features a licence may withhold, each identified by the plan-data
 * key that carries it. All three read as <b>enabled when the key is absent</b>; white labeling is not a
 * member because it uses the opposite convention and its own methods.
 */
@Getter
public enum PlatformFeature {

    INTEGRATIONS(PlanDataConstants.INTEGRATIONS_KEY, "Integrations", SubscriptionEntry.INTEGRATIONS),
    SCHEDULER(PlanDataConstants.SCHEDULER_KEY, "Scheduler", SubscriptionEntry.SCHEDULER),
    REPORTING(PlanDataConstants.REPORTING_KEY, "Reporting", SubscriptionEntry.REPORTING);

    private final String planDataKey;
    private final String displayName;
    private final SubscriptionEntry subscriptionEntry;

    PlatformFeature(String planDataKey, String displayName, SubscriptionEntry subscriptionEntry) {
        this.planDataKey = planDataKey;
        this.displayName = displayName;
        this.subscriptionEntry = subscriptionEntry;
    }
}
