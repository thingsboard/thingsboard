/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.subscription;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbEntityDataSubCtxTest {

    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    private final Integer cmdId = RandomUtils.nextInt();
    private final Integer subscriptionId = RandomUtils.nextInt();
    private final String serviceId = RandomStringUtils.randomAlphanumeric(10);
    private final String sessionId = RandomStringUtils.randomAlphanumeric(10);

    private final int maxEntitiesPerDataSubscription = 100;

    private TbEntityDataSubCtx subCtx;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private WebSocketSessionRef webSocketSessionRef;

    @BeforeEach
    public void setUp() {
        when(webSocketSessionRef.getSessionId()).thenReturn(sessionId);
        subCtx = new TbEntityDataSubCtx(serviceId, webSocketService, mock(), mock(), mock(), mock(), webSocketSessionRef, cmdId, maxEntitiesPerDataSubscription);

        Map<Integer, EntityId> subToEntityIdMap = new HashMap<>();
        subToEntityIdMap.put(subscriptionId, deviceId);
        ReflectionTestUtils.setField(subCtx, "subToEntityIdMap", subToEntityIdMap);

        long now = System.currentTimeMillis();
        long oldTs = now - 1_000_000;

        Map<String, TsValue> latestCtxValues = new HashMap<>();
        latestCtxValues.put("key", new TsValue(oldTs, "15"));
        Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
        latest.put(EntityKeyType.TIME_SERIES, latestCtxValues);

        EntityData entityData = new EntityData();
        entityData.setEntityId(deviceId);
        entityData.setLatest(latest);

        PageData<EntityData> data = new PageData<>(List.of(entityData), 1, 1, true);
        ReflectionTestUtils.setField(subCtx, "data", data);
    }

    @Test
    public void testSendLatestWsMsg() {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> telemetry = List.of(
                new BasicTsKvEntry(ts - 50000, new LongDataEntry("key", 42L), 34L),
                new BasicTsKvEntry(ts - 20000, new LongDataEntry("key", 17L), 78L)
        );

        TelemetrySubscriptionUpdate subUpdate = new TelemetrySubscriptionUpdate(subscriptionId, telemetry);

        subCtx.sendWsMsg(sessionId, subUpdate, EntityKeyType.TIME_SERIES, true);

        Map<EntityKeyType, Map<String, TsValue>> expectedLatest = new HashMap<>();
        Map<String, TsValue> expectedLatestCtxValues = new HashMap<>();
        expectedLatestCtxValues.put("key", new TsValue(ts - 20000, "17")); // use latest telemetry
        expectedLatest.put(EntityKeyType.TIME_SERIES, expectedLatestCtxValues);

        EntityData expectedEntityData = new EntityData();
        expectedEntityData.setEntityId(deviceId);
        expectedEntityData.setLatest(expectedLatest);

        List<EntityData> expected = List.of(expectedEntityData);

        ArgumentCaptor<CmdUpdate> cmdUpdateCaptor = ArgumentCaptor.forClass(CmdUpdate.class);
        then(webSocketService).should().sendUpdate(eq(sessionId), cmdUpdateCaptor.capture());
        CmdUpdate cmdUpdate = cmdUpdateCaptor.getValue();
        assertThat(cmdUpdate).isInstanceOf(EntityDataUpdate.class);
        EntityDataUpdate entityDataUpdate = (EntityDataUpdate) cmdUpdate;
        assertThat(entityDataUpdate.getCmdId()).isEqualTo(cmdId);
        assertThat(entityDataUpdate.getData()).isNull();
        assertThat(entityDataUpdate.getUpdate()).isEqualTo(expected);
        assertThat(entityDataUpdate.getAllowedEntities()).isEqualTo(maxEntitiesPerDataSubscription);
    }

}
