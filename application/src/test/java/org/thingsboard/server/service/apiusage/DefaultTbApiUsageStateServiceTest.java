/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.apiusage;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.ApiUsageLimitTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.telemetry.InternalTelemetryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultTbApiUsageStateServiceTest {

    @Mock
    TenantApiUsageState tenantUsageStateMock;

    @Mock
    private NotificationRuleProcessor notificationRuleProcessor;

    @Mock
    private ApiUsageStateService apiUsageStateService;

    @Mock
    private TenantService tenantService;

    @Mock
    private InternalTelemetryService tsWsService;

    @Mock
    private MailExecutorService mailExecutor;
    
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112"));

    @Spy
    @InjectMocks
    DefaultTbApiUsageStateService service;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(service, "tsWsService", tsWsService);
    }

    @Test
    public void givenTenantIdFromEntityStatesMap_whenGetApiUsageState() {
        service.myUsageStates.put(tenantId, tenantUsageStateMock);
        ApiUsageState tenantUsageState = service.getApiUsageState(tenantId);
        assertThat(tenantUsageState, is(tenantUsageStateMock.getApiUsageState()));
        Mockito.verify(service, never()).getOrFetchState(tenantId, tenantId);
    }

    @Test
    public void testAllApiFeaturesDisabledWhenLimitReached() {
        doReturn(null).when(tsWsService).saveTimeseriesInternal(any());

        TenantApiUsageState tenantUsageStateMock = mock(TenantApiUsageState.class);
        ApiUsageState apiUsageState = getApiUsageState();
        when(tenantUsageStateMock.getApiUsageState()).thenReturn(apiUsageState);

        doReturn(BaseApiUsageState.StatsCalculationResult.builder()
                .newValue(200L)
                .valueChanged(true)
                .newHourlyValue(200L)
                .hourlyValueChanged(true)
                .build())
                .when(tenantUsageStateMock).calculate(any(ApiUsageRecordKey.class), anyLong(), anyString());

        doReturn(200L).when(tenantUsageStateMock).getProfileThreshold(any(ApiUsageRecordKey.class));
        doReturn(50L).when(tenantUsageStateMock).getProfileWarnThreshold(any(ApiUsageRecordKey.class));
        doReturn(300L).when(tenantUsageStateMock).get(any(ApiUsageRecordKey.class));

        when(tenantUsageStateMock.getEntityType()).thenReturn(EntityType.TENANT);
        when(tenantUsageStateMock.getEntityId()).thenReturn(tenantId);

        Map<ApiFeature, ApiUsageStateValue> expectedResult = getExpectedResult();
        when(tenantUsageStateMock.checkStateUpdatedDueToThreshold(any())).thenReturn(expectedResult);
        service.myUsageStates.put(tenantId, tenantUsageStateMock);

        when(apiUsageStateService.update(apiUsageState)).thenReturn(apiUsageState);

        Tenant dummyTenant = new Tenant();
        dummyTenant.setEmail("test@example.com");
        when(tenantService.findTenantById(any())).thenReturn(dummyTenant);

        TransportProtos.ToUsageStatsServiceMsg.Builder msgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder();
        UUID uuid = tenantId.getId();
        msgBuilder.setTenantIdMSB(uuid.getMostSignificantBits())
                .setTenantIdLSB(uuid.getLeastSignificantBits());
        msgBuilder.setCustomerIdMSB(0)
                .setCustomerIdLSB(0);
        msgBuilder.setServiceId("TEST_SERVICE");

        List<TransportProtos.UsageStatsKVProto> usageStats = new ArrayList<>();
        for (ApiUsageRecordKey recordKey : ApiUsageRecordKey.values()) {
            if (recordKey.getApiFeature() != null) {
                TransportProtos.UsageStatsKVProto stat = TransportProtos.UsageStatsKVProto.newBuilder()
                        .setKey(recordKey.name())
                        .setValue(1000L)
                        .build();
                usageStats.add(stat);
                msgBuilder.addValues(stat);
            }
        }

        TransportProtos.ToUsageStatsServiceMsg statsMsg = msgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> queueMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), statsMsg);
        TbCallback callback = mock(TbCallback.class);

        service.process(queueMsg, callback);
        verify(callback).onSuccess();

        for (ApiFeature feature : expectedResult.keySet()) {
            verify(notificationRuleProcessor, atLeastOnce()).process(argThat(trigger ->
                    trigger instanceof ApiUsageLimitTrigger &&
                            ((ApiUsageLimitTrigger) trigger).getStatus() == ApiUsageStateValue.DISABLED &&
                            ((ApiUsageLimitTrigger) trigger).getState().getApiFeature().getApiStateKey().equals(feature.getApiStateKey())
            ));
        }
    }


    @NotNull
    private ApiUsageState getApiUsageState() {
        ApiUsageState apiUsageState = new ApiUsageState();

        apiUsageState.setTenantId(tenantId);
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTbelExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        return apiUsageState;
    }

    private Map<ApiFeature, ApiUsageStateValue> getExpectedResult() {
        Map<ApiFeature, ApiUsageStateValue> expectedResult = new HashMap<>();
        for (ApiUsageRecordKey recordKey : ApiUsageRecordKey.values()) {
            if (recordKey.getApiFeature() != null) {
                expectedResult.put(recordKey.getApiFeature(), ApiUsageStateValue.DISABLED);
            }
        }

        return expectedResult;
    }
}