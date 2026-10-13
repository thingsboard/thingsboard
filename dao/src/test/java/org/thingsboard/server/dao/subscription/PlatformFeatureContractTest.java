// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two things this repository silently depends on the licence client for. Both are properties of the
 * published client rather than of any code here, so both are asserted against a real
 * {@link AbstractTbLicenseClient} instead of a mock - a mock would happily agree with whatever we assumed.
 */
public class PlatformFeatureContractTest {

    @Test
    public void aPlanThatSaysNothingAboutAFeatureGrantsIt() {
        // The central guarantee of the whole feature: absent means enabled. Every existing licence predates
        // these keys and therefore carries none of them, so if the client ever resolved an absent key to
        // false instead, every one of those instances would lose integrations, the scheduler and reporting
        // at once - with no error anywhere to point at. Goes red on a client upgrade that flips the
        // convention, which is the only way this can change.
        BasicSubscriptionService subscriptionService = new BasicSubscriptionService();
        LicenseActivationStub.wire(subscriptionService, new FixedPlanLicenseClient(Map.of()), 2);

        for (PlatformFeature feature : PlatformFeature.values()) {
            assertThat(subscriptionService.isFeatureEnabled(TenantId.SYS_TENANT_ID, feature))
                    .as("%s must be granted by a plan that carries no '%s' key", feature, feature.getPlanDataKey())
                    .isTrue();
        }
    }

    @Test
    public void eachFeatureLooksUpTheKeyTheLicenceServerActuallyPublishes() {
        // PlanDataConstants is an interface of compile-time String constants, so these values are inlined
        // into PlatformFeature.class when this repository is built. A client published with different key
        // values would not fail to link: every lookup would simply miss, and - because an absent key means
        // enabled - every withheld feature would silently read as granted. Spelling the values out here is
        // what makes that rename visible, since only recompiling against the new client changes them.
        Map<PlatformFeature, String> publishedKeys = Map.of(
                PlatformFeature.INTEGRATIONS, "integrations",
                PlatformFeature.SCHEDULER, "scheduler",
                PlatformFeature.REPORTING, "reporting");

        // The keys are compared against the whole enum, so a feature added without its published key named
        // here fails rather than being pinned by nothing.
        assertThat(publishedKeys.keySet()).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(PlatformFeature.class));
        publishedKeys.forEach((feature, key) -> assertThat(feature.getPlanDataKey()).isEqualTo(key));
    }

}
