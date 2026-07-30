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
package org.thingsboard.server.service.edge.attributes;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AttributesEdgeSyncServiceTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("a00ec470-c6b4-11ef-8c88-63b5533fb5bc"));
    final DeviceId deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

    @Mock
    AttributesEdgeSyncStrategy deviceSyncStrategy;

    AttributesEdgeSyncService service;

    @BeforeEach
    void setup() {
        lenient().when(deviceSyncStrategy.getEntityType()).thenReturn(EntityType.DEVICE);
        service = new AttributesEdgeSyncService(List.of(deviceSyncStrategy));
        // init() with edges disabled only builds the strategy registry; enable edges and inject a direct executor afterwards
        service.init();
        ReflectionTestUtils.setField(service, "edgesEnabled", true);
        ReflectionTestUtils.setField(service, "executors", new ExecutorService[]{MoreExecutors.newDirectExecutorService()});
    }

    @Test
    void shouldConsultStrategyWhenGeneralRulesPass() {
        given(deviceSyncStrategy.isEdgeSyncRequired(any(AttributesSaveRequest.class))).willReturn(true);
        assertThat(service.isEdgeSyncRequired(saveRequest(null, AttributesSaveRequest.Strategy.PROCESS_ALL))).isTrue();

        given(deviceSyncStrategy.isEdgeSyncRequired(any(AttributesDeleteRequest.class))).willReturn(true);
        assertThat(service.isEdgeSyncRequired(deleteRequest(null))).isTrue();
    }

    @Test
    void shouldNotBeRequiredWhenEdgesDisabled() {
        ReflectionTestUtils.setField(service, "edgesEnabled", false);

        assertThat(service.isEdgeSyncRequired(saveRequest(null, AttributesSaveRequest.Strategy.PROCESS_ALL))).isFalse();
        assertThat(service.isEdgeSyncRequired(deleteRequest(null))).isFalse();
        then(deviceSyncStrategy).should(never()).isEdgeSyncRequired(any(AttributesSaveRequest.class));
        then(deviceSyncStrategy).should(never()).isEdgeSyncRequired(any(AttributesDeleteRequest.class));
    }

    @Test
    void shouldNotBeRequiredWhenUpdateOriginatedFromEdge() {
        assertThat(service.isEdgeSyncRequired(saveRequest(DataConstants.EDGE_MSG_SOURCE, AttributesSaveRequest.Strategy.PROCESS_ALL))).isFalse();
        assertThat(service.isEdgeSyncRequired(deleteRequest(DataConstants.EDGE_MSG_SOURCE))).isFalse();
        then(deviceSyncStrategy).should(never()).isEdgeSyncRequired(any(AttributesSaveRequest.class));
        then(deviceSyncStrategy).should(never()).isEdgeSyncRequired(any(AttributesDeleteRequest.class));
    }

    @Test
    void shouldNotBeRequiredWhenSaveStrategyDoesNotPersistAttributes() {
        assertThat(service.isEdgeSyncRequired(saveRequest(null, AttributesSaveRequest.Strategy.WS_ONLY))).isFalse();
        then(deviceSyncStrategy).should(never()).isEdgeSyncRequired(any(AttributesSaveRequest.class));
    }

    @Test
    void shouldNotBeRequiredWhenNoStrategyRegisteredForEntityType() {
        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(new AssetId(UUID.randomUUID()))
                .scope(AttributeScope.SHARED_SCOPE)
                .entry(new BaseAttributeKvEntry(new StringDataEntry("key", "value"), 100L))
                .build();

        assertThat(service.isEdgeSyncRequired(request)).isFalse();
        then(deviceSyncStrategy).should(never()).isEdgeSyncRequired(any(AttributesSaveRequest.class));
    }

    @Test
    void shouldDispatchUpdateToStrategy() {
        var request = saveRequest(null, AttributesSaveRequest.Strategy.PROCESS_ALL);

        service.onAttributesUpdate(request);

        then(deviceSyncStrategy).should().onAttributesUpdate(request);
    }

    @Test
    void shouldDispatchDeleteToStrategy() {
        var request = deleteRequest(null);

        service.onAttributesDelete(request);

        then(deviceSyncStrategy).should().onAttributesDelete(request);
    }

    @Test
    void shouldIgnoreEventsForEntityTypesWithoutStrategy() {
        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(new AssetId(UUID.randomUUID()))
                .scope(AttributeScope.SHARED_SCOPE)
                .keys(List.of("key"))
                .build();

        service.onAttributesDelete(request);

        then(deviceSyncStrategy).should(never()).onAttributesDelete(any());
    }

    AttributesSaveRequest saveRequest(String msgSource, AttributesSaveRequest.Strategy strategy) {
        return AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .entry(new BaseAttributeKvEntry(new StringDataEntry("Modbus", "{}"), 100L))
                .msgSource(msgSource)
                .strategy(strategy)
                .build();
    }

    AttributesDeleteRequest deleteRequest(String msgSource) {
        return AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .keys(List.of("Modbus"))
                .msgSource(msgSource)
                .build();
    }
}
