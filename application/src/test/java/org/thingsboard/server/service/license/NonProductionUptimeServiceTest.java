// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.queue.discovery.PartitionService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Accrual only: whether a keyless deployment past its non-production allowance gets locked is
 * {@code NonProductionEntitlementEnforcer}'s concern (dao module), not this class's - see that class's
 * javadoc for why the enforcement check cannot live behind the same node-type restriction accrual needs.
 */
public class NonProductionUptimeServiceTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final PartitionService partitionService = mock(PartitionService.class);
    private final TbClusterStore tbClusterStore = mock(TbClusterStore.class);
    private final NonProductionUptimeService uptimeService =
            new NonProductionUptimeService(subscriptionService, partitionService, tbClusterStore);

    @Test
    public void aLicensedInstanceNeverConsultsThePartitionOrAccrues() {
        when(subscriptionService.isNonProductionMode()).thenReturn(false);

        uptimeService.tick();

        verify(partitionService, never()).isSystemPartitionMine(ServiceType.TB_CORE);
        verify(tbClusterStore, never()).tickNonProductionUptime(anyLong(), anyLong());
    }

    @Test
    public void owningThePartitionAccrues() {
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        when(partitionService.isSystemPartitionMine(ServiceType.TB_CORE)).thenReturn(true);

        uptimeService.tick();

        verify(tbClusterStore).tickNonProductionUptime(anyLong(), eq(TbClusterStore.MAX_DELTA_MS));
    }

    @Test
    public void notOwningThePartitionDoesNotAccrue() {
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        when(partitionService.isSystemPartitionMine(ServiceType.TB_CORE)).thenReturn(false);

        uptimeService.tick();

        verify(tbClusterStore, never()).tickNonProductionUptime(anyLong(), anyLong());
    }

}
