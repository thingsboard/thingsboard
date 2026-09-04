// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

class ComponentActorTest {
    ComponentActor componentActor;

    @BeforeEach
    void setUp() {
        componentActor = Mockito.mock(ComponentActor.class);
    }

    @Test
    void scheduleStatsPersistTickTest() {
        Assertions.assertNull(componentActor.statsScheduledFuture);
        ScheduledFuture<?> statsScheduledFuture = Mockito.mock(ScheduledFuture.class);
        ActorSystemContext systemContext = Mockito.mock(ActorSystemContext.class);
        ReflectionTestUtils.setField(componentActor, "systemContext", systemContext);
        ComponentMsgProcessor<?> processor = Mockito.mock(ComponentMsgProcessor.class);
        componentActor.processor = processor;
        BDDMockito.willReturn(statsScheduledFuture).given(processor).scheduleStatsPersistTick(any(), anyLong());
        BDDMockito.willCallRealMethod().given(componentActor).scheduleStatsPersistTick();

        componentActor.scheduleStatsPersistTick();

        Assertions.assertNotNull(componentActor.statsScheduledFuture);
    }

    @Test
    void destroyTest() {
        ScheduledFuture<?> statsScheduledFuture = Mockito.mock(ScheduledFuture.class);
        componentActor.statsScheduledFuture = statsScheduledFuture;
        Assertions.assertNotNull(componentActor.statsScheduledFuture);
        Throwable cause = new Throwable();
        EntityId id = Mockito.mock(EntityId.class);
        ReflectionTestUtils.setField(componentActor, "id", id);
        BDDMockito.willCallRealMethod().given(componentActor).destroy(any(), any());

        componentActor.destroy(TbActorStopReason.STOPPED, cause);

        Mockito.verify(statsScheduledFuture).cancel(false);
        Assertions.assertNull(componentActor.statsScheduledFuture);
    }

}
