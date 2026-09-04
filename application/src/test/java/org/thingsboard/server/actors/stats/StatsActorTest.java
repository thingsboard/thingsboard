// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StatsActorTest {

    StatsActor statsActor;
    ActorSystemContext actorSystemContext;
    EventService eventService;
    TbServiceInfoProvider serviceInfoProvider;

    @BeforeEach
    void setUp() {
        actorSystemContext = mock(ActorSystemContext.class);

        eventService = mock(EventService.class);
        willReturn(eventService).given(actorSystemContext).getEventService();
        serviceInfoProvider = mock(TbServiceInfoProvider.class);
        willReturn(serviceInfoProvider).given(actorSystemContext).getServiceInfoProvider();

        statsActor = new StatsActor(actorSystemContext);
    }

    @Test
    void givenEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction() {
        StatsPersistMsg emptyStats = new StatsPersistMsg(0, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
        statsActor.onStatsPersistMsg(emptyStats);
        verify(actorSystemContext, never()).getEventService();
    }

    @Test
    void givenNonEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction() {
        statsActor.onStatsPersistMsg(new StatsPersistMsg(0, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(1)).saveAsync(any(Event.class));
        statsActor.onStatsPersistMsg(new StatsPersistMsg(1, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(2)).saveAsync(any(Event.class));
        statsActor.onStatsPersistMsg(new StatsPersistMsg(1, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(3)).saveAsync(any(Event.class));
    }

}
