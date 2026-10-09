// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The edge quota is a soft, creation-time cap for every licence, including the ones the capacity gate applies
 * to: an instance holding more edges than its plan covers starts normally and simply cannot add another.
 * <p>
 * That asymmetry with devices and assets is deliberate, and this test records it. Anyone tempted to "finish
 * the job" by adding an edge branch to the startup gate or the upgrade pre-flight will fail here first.
 */
public class EdgeQuotaStaysSoftTest {

    @Test
    public void anOverCapEdgeCountDoesNotBlockStartupEvenOnAGrantLicense() {
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        DeviceService deviceService = mock(DeviceService.class);
        when(deviceService.countDevices()).thenReturn(10L);

        // No edge collaborator is passed, because the gate does not have one. Adding an edge branch would
        // mean adding a constructor argument, which breaks compilation here rather than passing silently.
        assertThatCode(() -> new LicenseCapacityStartupGate(subscriptionService, deviceService, mock(AssetService.class)).check())
                .doesNotThrowAnyException();
    }

    @Test
    public void anOverCapEdgeCountStillBlocksNewEdgeCreation() {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanLongValue(PlanDataConstants.MAX_EDGES_KEY)).thenReturn(5L);
        EdgeService edgeService = mock(EdgeService.class);
        when(edgeService.countEdges()).thenReturn(7L);

        BasicSubscriptionService subscriptionService = new BasicSubscriptionService();
        LicenseActivationStub.wire(subscriptionService, client, 2);
        ReflectionTestUtils.setField(subscriptionService, "edgeService", edgeService);

        assertThat(subscriptionService.isCreateEdgeAllowed(TenantId.SYS_TENANT_ID)).isFalse();
        assertThatExceptionOfType(SubscriptionException.class)
                .isThrownBy(() -> subscriptionService.createEdgeAllowed(TenantId.SYS_TENANT_ID))
                .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.LIMIT_REACHED));
    }
}
