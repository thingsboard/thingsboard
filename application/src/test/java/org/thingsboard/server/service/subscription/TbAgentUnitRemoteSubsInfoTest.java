// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.subscription;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.subscription.TbAgentUnitRemoteSubsInfo.TbAgentUnitSubsUpdateInfo;
import org.thingsboard.server.service.subscription.TbEntityRemoteSubsInfo.TbEntitySubsUpdateInfo;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The log stream on the agent is started on the first log subscriber and stopped on the last one, and this
 * class is the only place that computes those two transitions - from subscription events arriving out of any
 * number of core nodes.
 */
class TbAgentUnitRemoteSubsInfoTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentAppUnitId UNIT_ID = new AgentAppUnitId(UUID.randomUUID());
    private static final String SERVICE_A = "core-a";
    private static final String SERVICE_B = "core-b";

    @Test
    void firstLogSubIsTheOnlyOneThatReadsAsAFirstSub() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);

        TbAgentUnitSubsUpdateInfo first = update(subsInfo, SERVICE_A, ComponentLifecycleEvent.CREATED, logs(), 1);
        assertThat(first.isEmptyLogSubsBeforeEvent()).isTrue();
        assertThat(first.isEmptyLogSubsAfterEvent()).isFalse();

        // a second core node subscribing while the first still holds a log sub is not a first sub
        TbAgentUnitSubsUpdateInfo second = update(subsInfo, SERVICE_B, ComponentLifecycleEvent.CREATED, logs(), 1);
        assertThat(second.isEmptyLogSubsBeforeEvent()).isFalse();
        assertThat(second.isEmptyLogSubsAfterEvent()).isFalse();
    }

    @Test
    void onlyTheLastLogSubUnsubscribingReadsAsALastSub() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);
        update(subsInfo, SERVICE_A, ComponentLifecycleEvent.CREATED, logs(), 1);
        update(subsInfo, SERVICE_B, ComponentLifecycleEvent.CREATED, logs(), 1);

        TbAgentUnitSubsUpdateInfo firstGone = update(subsInfo, SERVICE_A, ComponentLifecycleEvent.DELETED, logs(), 2);
        assertThat(firstGone.isEmptyLogSubsBeforeEvent()).isFalse();
        assertThat(firstGone.isEmptyLogSubsAfterEvent()).isFalse();
        assertThat(firstGone.isEmpty()).isFalse();

        TbAgentUnitSubsUpdateInfo lastGone = update(subsInfo, SERVICE_B, ComponentLifecycleEvent.DELETED, logs(), 2);
        assertThat(lastGone.isEmptyLogSubsBeforeEvent()).isFalse();
        assertThat(lastGone.isEmptyLogSubsAfterEvent()).isTrue();
        assertThat(lastGone.isEmpty()).isTrue();
    }

    @Test
    void anUpdateDroppingOnlyTheLogFlagReadsAsALastSub() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);
        update(subsInfo, SERVICE_A, ComponentLifecycleEvent.CREATED, logsAndAlarms(), 1);

        TbAgentUnitSubsUpdateInfo alarmsOnly = update(subsInfo, SERVICE_A, ComponentLifecycleEvent.UPDATED, alarms(), 2);

        assertThat(alarmsOnly.isEmptyLogSubsBeforeEvent()).isFalse();
        assertThat(alarmsOnly.isEmptyLogSubsAfterEvent()).isTrue();
        // the unit still has a subscriber, so the record itself must stay
        assertThat(alarmsOnly.isEmpty()).isFalse();
    }

    @Test
    void nonLogSubscriptionsNeverTouchTheLogTransitions() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);

        TbAgentUnitSubsUpdateInfo created = update(subsInfo, SERVICE_A, ComponentLifecycleEvent.CREATED, alarms(), 1);
        assertThat(created.isEmptyLogSubsBeforeEvent()).isTrue();
        assertThat(created.isEmptyLogSubsAfterEvent()).isTrue();

        TbAgentUnitSubsUpdateInfo deleted = update(subsInfo, SERVICE_A, ComponentLifecycleEvent.DELETED, alarms(), 2);
        assertThat(deleted.isEmptyLogSubsBeforeEvent()).isTrue();
        assertThat(deleted.isEmptyLogSubsAfterEvent()).isTrue();
        assertThat(deleted.isEmpty()).isTrue();
    }

    @Test
    void anOutOfOrderEventIsReportedAsADuplicateAndChangesNothing() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);
        update(subsInfo, SERVICE_A, ComponentLifecycleEvent.CREATED, logs(), 5);

        TbAgentUnitSubsUpdateInfo stale = update(subsInfo, SERVICE_A, ComponentLifecycleEvent.DELETED, logs(), 4);

        assertThat(stale.isDuplicate()).isTrue();
        assertThat(subsInfo.getSubs()).containsKey(SERVICE_A);
    }

    @Test
    void nodeShutdownOfTheLastLogSubscriberReadsAsALastSub() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);
        update(subsInfo, SERVICE_A, ComponentLifecycleEvent.CREATED, logs(), 1);
        update(subsInfo, SERVICE_B, ComponentLifecycleEvent.CREATED, logs(), 1);

        TbAgentUnitSubsUpdateInfo firstGone = (TbAgentUnitSubsUpdateInfo) subsInfo.removeAndGetUpdateInfo(SERVICE_A);
        assertThat(firstGone.isEmptyLogSubsAfterEvent()).isFalse();

        TbAgentUnitSubsUpdateInfo lastGone = (TbAgentUnitSubsUpdateInfo) subsInfo.removeAndGetUpdateInfo(SERVICE_B);
        assertThat(lastGone.isEmptyLogSubsBeforeEvent()).isFalse();
        assertThat(lastGone.isEmptyLogSubsAfterEvent()).isTrue();
        assertThat(lastGone.isEmpty()).isTrue();
    }

    @Test
    void removingAnUnknownServiceReportsNothing() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);

        assertThat(subsInfo.removeAndGetUpdateInfo(SERVICE_A)).isNull();
    }

    private TbAgentUnitSubsUpdateInfo update(TbAgentUnitRemoteSubsInfo subsInfo, String serviceId,
                                             ComponentLifecycleEvent type, TbSubscriptionsInfo info, int seqNumber) {
        TbEntitySubsUpdateInfo updateInfo = subsInfo.updateAndCheckIsEmpty(serviceId, TbEntitySubEvent.builder()
                .tenantId(TENANT_ID)
                .entityId(UNIT_ID)
                .type(type)
                .info(info.copy(seqNumber))
                .seqNumber(seqNumber)
                .build());
        assertThat(updateInfo).isInstanceOf(TbAgentUnitSubsUpdateInfo.class);
        return (TbAgentUnitSubsUpdateInfo) updateInfo;
    }

    private TbSubscriptionsInfo logs() {
        return subscriptions(true, false);
    }

    private TbSubscriptionsInfo alarms() {
        return subscriptions(false, true);
    }

    private TbSubscriptionsInfo logsAndAlarms() {
        return subscriptions(true, true);
    }

    private TbSubscriptionsInfo subscriptions(boolean logs, boolean alarms) {
        return new TbSubscriptionsInfo(false, alarms, logs, false, null, false, null, 0);
    }
}
