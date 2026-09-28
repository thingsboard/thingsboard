// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanItem;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two properties of the published licence client this phase depends on: that
 * {@link AbstractTbLicenseClient#getPlanBooleanValue(String)} resolves an arbitrary key through the abstract
 * {@code getPlanItemOpt} lookup rather than through a fixed set of keys it declares itself, and that a key
 * the plan does not carry resolves to false rather than to true.
 * <p>
 * Both matter in opposite directions. The generic lookup is what lets a plan carry the grant key without
 * every reader having to declare it. Absent-means-false is what keeps every licence that does not carry the
 * key out of the new enforcement entirely; a client upgrade that changed it would go red here rather than
 * silently gate every such instance, or silently gate none.
 * <p>
 * What this cannot reach is how a real client builds its plan data in the first place: the test double
 * supplies the map directly, so a client release that dropped unknown keys while deserializing plan data
 * would still pass. That path lives in the client artifact and is pinned there.
 */
public class CommunityGrantPlanKeyContractTest {

    @Test
    public void anArbitraryKeyIsResolvedThroughTheGenericPlanItemLookup() {
        AbstractTbLicenseClient client = new FixedPlanLicenseClient(
                Map.of(CommunityGrantPlan.COMMUNITY_GRANT_KEY, new PlanItem(true)));
        assertThat(client.getPlanBooleanValue(CommunityGrantPlan.COMMUNITY_GRANT_KEY)).isTrue();
    }

    @Test
    public void aPlanThatDoesNotCarryTheKeyResolvesToFalse() {
        AbstractTbLicenseClient client = new FixedPlanLicenseClient(Map.of());
        assertThat(client.getPlanBooleanValue(CommunityGrantPlan.COMMUNITY_GRANT_KEY)).isFalse();
    }

    @Test
    public void theKeyIsTheOneThePortalPublishes() {
        // Spelled out rather than derived: CommunityGrantPlan takes the key from the client library, so this
        // is the assertion that pins the library's value to the literal the portal actually writes. A
        // portal-side rename that reaches a client release lands here rather than passing silently.
        assertThat(CommunityGrantPlan.COMMUNITY_GRANT_KEY).isEqualTo("communitygrant");
    }
}
