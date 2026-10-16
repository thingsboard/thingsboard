// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.apiusage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoUnit.MONTHS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.thingsboard.server.common.data.ApiFeature.SMS;
import static org.thingsboard.server.common.data.ApiUsageRecordKey.SMS_EXEC_COUNT;

@DaoSqlTest
public class DefaultTbApiUsageStateServiceTest extends AbstractControllerTest {

    @Autowired
    DefaultTbApiUsageStateService service;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    private TenantId tenantId;
    private TenantProfile savedTenantProfile;

    private static final int MAX_ENABLE_VALUE = 5000;
    private static final long VALUE_WARNING = 4500L;
    private static final long VALUE_DISABLE = 5500L;
    private static final double WARN_THRESHOLD_VALUE = 0.8;

    @Before
    public void init() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = createTenantProfile();
        savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        Tenant savedTenant = saveTenant(tenant);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);
    }

    @Test
    public void testProcess_transitionFromWarningToDisabled() {
        sendUsageStats(tenantId, ApiUsageRecordKey.STORAGE_DP_COUNT, VALUE_WARNING);
        assertEquals(ApiUsageStateValue.WARNING, apiUsageStateService.findTenantApiUsageState(tenantId).getDbStorageState());

        sendUsageStats(tenantId, ApiUsageRecordKey.STORAGE_DP_COUNT, VALUE_DISABLE);
        assertEquals(ApiUsageStateValue.DISABLED, apiUsageStateService.findTenantApiUsageState(tenantId).getDbStorageState());
    }

    @Test
    public void checkStartOfNextCycle_setsNextCycleToNextMonth() throws Exception {
        ApiUsageState apiUsageState = new ApiUsageState(new ApiUsageStateId(UUID.randomUUID()));
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTbelExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEdgeState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(tenantId);

        long now = System.currentTimeMillis();
        long currentCycleTs = now - TimeUnit.DAYS.toMillis(30);
        long nextCycleTs = now - TimeUnit.MINUTES.toMillis(5); // < 1h ago
        TenantApiUsageState tenantApiUsageState = new TenantApiUsageState(savedTenantProfile, apiUsageState);
        tenantApiUsageState.setCycles(currentCycleTs, nextCycleTs);
        Map<EntityId, BaseApiUsageState> map = new HashMap<>();
        map.put(tenantId, tenantApiUsageState);

        Field fieldToSet = DefaultTbApiUsageStateService.class.getDeclaredField("myUsageStates");
        fieldToSet.setAccessible(true);
        fieldToSet.set(service, map);

        service.checkStartOfNextCycle();

        long firstOfNextMonth = LocalDate.now()
                .with((temporal) -> temporal.with(DAY_OF_MONTH, 1)
                        .plus(1, MONTHS))
                .atStartOfDay(UTC).toInstant().toEpochMilli();
        assertThat(tenantApiUsageState.getNextCycleTs()).isEqualTo(firstOfNextMonth);
    }

    @Test
    public void checkTenantCreatedWithSmsDisabledApiUsage() throws Exception {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Test profile");
        TenantProfileData tenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .smsEnabled(false)
                .build();

        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        Tenant tenant = new Tenant();
        tenant.setTitle("My test tenant");
        tenant.setTenantProfileId(tenantProfile.getId());
        tenant = saveTenant(tenant);

        ApiUsageState apiUsageState = apiUsageStateService.findApiUsageStateByEntityId(tenant.getId());
        assertThat(apiUsageState.getSmsExecState()).isEqualTo(ApiUsageStateValue.DISABLED);

        TenantId finalTenantId = tenant.getId();
        ApiUsageStateId finalApiUsageStateId = apiUsageState.getId();


        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Optional<TsKvEntry> smsApiState = tsService.findLatest(finalTenantId, finalApiUsageStateId, SMS.getApiStateKey()).get();
            return smsApiState.isPresent() && smsApiState.get().getValueAsString().equals(ApiUsageStateValue.DISABLED.name());
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Optional<TsKvEntry> smsApiState = tsService.findLatest(finalTenantId, finalApiUsageStateId, SMS_EXEC_COUNT.getApiLimitKey()).get();
            return smsApiState.isPresent() && smsApiState.get().getLongValue().isPresent() && smsApiState.get().getLongValue().get().equals(0L);
        });

        // enable SMS and check that the ApiUsageState is updated accordingly
        config = DefaultTenantProfileConfiguration.builder()
                .smsEnabled(true)
                .maxSms(10)
                .build();
        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        await().atMost(5, TimeUnit.SECONDS).until(() ->
                apiUsageStateService.findApiUsageStateByEntityId(finalTenantId).getSmsExecState() == ApiUsageStateValue.ENABLED);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Optional<TsKvEntry> smsApiState = tsService.findLatest(finalTenantId, finalApiUsageStateId, SMS.getApiStateKey()).get();
            return smsApiState.isPresent() && smsApiState.get().getValueAsString().equals(ApiUsageStateValue.ENABLED.name());
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Optional<TsKvEntry> smsApiState = tsService.findLatest(finalTenantId, finalApiUsageStateId, SMS_EXEC_COUNT.getApiLimitKey()).get();
            return smsApiState.isPresent() && smsApiState.get().getLongValue().isPresent() && smsApiState.get().getLongValue().get().equals(10L);
        });

        // disable SMS and check that the ApiUsageState is updated accordingly
        config = DefaultTenantProfileConfiguration.builder()
                .smsEnabled(false)
                .build();
        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        await().atMost(5, TimeUnit.SECONDS).until(() ->
                apiUsageStateService.findApiUsageStateByEntityId(finalTenantId).getSmsExecState() == ApiUsageStateValue.DISABLED);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Optional<TsKvEntry> smsApiState = tsService.findLatest(finalTenantId, finalApiUsageStateId, SMS.getApiStateKey()).get();
            return smsApiState.isPresent() && smsApiState.get().getValueAsString().equals(ApiUsageStateValue.DISABLED.name());
        });

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Optional<TsKvEntry> smsApiState = tsService.findLatest(finalTenantId, finalApiUsageStateId, SMS_EXEC_COUNT.getApiLimitKey()).get();
            return smsApiState.isPresent() && smsApiState.get().getLongValue().isPresent() && smsApiState.get().getLongValue().get().equals(0L);
        });
    }

    @Test
    public void testEdgeStateTransitions() throws Exception {
        TenantProfile edgeProfile = createProfileWithThreshold("Edge Test Profile", DefaultTenantProfileConfiguration.builder()
                .maxEdgeEvents(100)
                .warnThreshold(0.8)
                .build());
        Tenant edgeTenant = createTenantWithProfile("Edge tenant", edgeProfile);
        TenantId edgeTenantId = edgeTenant.getId();

        sendUsageStats(edgeTenantId, ApiUsageRecordKey.EDGE_EVENT_COUNT, 85);
        assertEquals(ApiUsageStateValue.WARNING, apiUsageStateService.findTenantApiUsageState(edgeTenantId).getEdgeState());

        sendUsageStats(edgeTenantId, ApiUsageRecordKey.EDGE_EVENT_COUNT, 20);
        assertEquals(ApiUsageStateValue.DISABLED, apiUsageStateService.findTenantApiUsageState(edgeTenantId).getEdgeState());
    }

    @Test
    public void testIntegrationStateTransitions() throws Exception {
        TenantProfile integrationProfile = createProfileWithThreshold("Integration Test Profile", DefaultTenantProfileConfiguration.builder()
                .maxIntegrationMessages(100)
                .warnThreshold(0.8)
                .build());
        Tenant integrationTenant = createTenantWithProfile("Integration tenant", integrationProfile);
        TenantId integrationTenantId = integrationTenant.getId();

        sendUsageStats(integrationTenantId, ApiUsageRecordKey.INTEGRATION_MSG_COUNT, 85);
        assertEquals(ApiUsageStateValue.WARNING, apiUsageStateService.findTenantApiUsageState(integrationTenantId).getIntegrationState());

        sendUsageStats(integrationTenantId, ApiUsageRecordKey.INTEGRATION_MSG_COUNT, 20);
        assertEquals(ApiUsageStateValue.DISABLED, apiUsageStateService.findTenantApiUsageState(integrationTenantId).getIntegrationState());
    }

    @Test
    public void testEdgeDisableDoesNotAffectOtherFeatures() throws Exception {
        TenantProfile profile = createProfileWithThreshold("Edge Isolation Profile", DefaultTenantProfileConfiguration.builder()
                .maxEdgeEvents(50)
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .warnThreshold(0.8)
                .build());
        Tenant tenant = createTenantWithProfile("Edge isolation tenant", profile);
        TenantId tid = tenant.getId();

        sendUsageStats(tid, ApiUsageRecordKey.EDGE_EVENT_COUNT, 100);

        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tid);
        assertEquals(ApiUsageStateValue.DISABLED, state.getEdgeState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getTransportState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getReExecState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getDbStorageState());
    }

    @Test
    public void testIntegrationDisableDoesNotAffectOtherFeatures() throws Exception {
        TenantProfile profile = createProfileWithThreshold("Integration Isolation Profile", DefaultTenantProfileConfiguration.builder()
                .maxIntegrationMessages(50)
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .warnThreshold(0.8)
                .build());
        Tenant tenant = createTenantWithProfile("Integration isolation tenant", profile);
        TenantId tid = tenant.getId();

        sendUsageStats(tid, ApiUsageRecordKey.INTEGRATION_MSG_COUNT, 100);

        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tid);
        assertEquals(ApiUsageStateValue.DISABLED, state.getIntegrationState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getTransportState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getReExecState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getDbStorageState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getEdgeState());
    }

    @Test
    public void testMultipleFeatureThresholdsIndependent() throws Exception {
        TenantProfile profile = createProfileWithThreshold("Multi Feature Profile", DefaultTenantProfileConfiguration.builder()
                .maxEdgeEvents(100)
                .maxIntegrationMessages(200)
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .warnThreshold(0.8)
                .build());
        Tenant tenant = createTenantWithProfile("Multi feature tenant", profile);
        TenantId tid = tenant.getId();

        // Edge at WARNING, Integration at ENABLED
        sendUsageStats(tid, ApiUsageRecordKey.EDGE_EVENT_COUNT, 85);
        sendUsageStats(tid, ApiUsageRecordKey.INTEGRATION_MSG_COUNT, 50);

        ApiUsageState state = apiUsageStateService.findTenantApiUsageState(tid);
        assertEquals(ApiUsageStateValue.WARNING, state.getEdgeState());
        assertEquals(ApiUsageStateValue.ENABLED, state.getIntegrationState());

        // Edge at DISABLED, Integration at WARNING
        sendUsageStats(tid, ApiUsageRecordKey.EDGE_EVENT_COUNT, 20);
        sendUsageStats(tid, ApiUsageRecordKey.INTEGRATION_MSG_COUNT, 120);

        state = apiUsageStateService.findTenantApiUsageState(tid);
        assertEquals(ApiUsageStateValue.DISABLED, state.getEdgeState());
        assertEquals(ApiUsageStateValue.WARNING, state.getIntegrationState());
    }

    @Test
    public void testZeroThresholdMeansUnlimited() throws Exception {
        TenantProfile profile = createProfileWithThreshold("Unlimited Profile", DefaultTenantProfileConfiguration.builder()
                .maxEdgeEvents(0)
                .warnThreshold(0.8)
                .build());
        Tenant tenant = createTenantWithProfile("Unlimited tenant", profile);
        TenantId tid = tenant.getId();

        sendUsageStats(tid, ApiUsageRecordKey.EDGE_EVENT_COUNT, 1_000_000);

        assertEquals(ApiUsageStateValue.ENABLED, apiUsageStateService.findTenantApiUsageState(tid).getEdgeState());
    }

    private void sendUsageStats(TenantId tenantId, ApiUsageRecordKey recordKey, long value) {
        TransportProtos.UsageStatsServiceMsg statsMsg = TransportProtos.UsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                        .setRecordKey(ProtoUtils.toProto(recordKey))
                        .setValue(value)
                        .build())
                .build();

        TransportProtos.ToUsageStatsServiceMsg msg = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setServiceId("testService")
                .addMsgs(statsMsg)
                .build();

        service.process(new TbProtoQueueMsg<>(UUID.randomUUID(), msg), TbCallback.EMPTY);
    }

    private TenantProfile createProfileWithThreshold(String name, DefaultTenantProfileConfiguration config) {
        TenantProfile profile = new TenantProfile();
        profile.setName(name);
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(config);
        profile.setProfileData(profileData);
        return doPost("/api/tenantProfile", profile, TenantProfile.class);
    }

    private Tenant createTenantWithProfile(String title, TenantProfile profile) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTitle(title);
        tenant.setTenantProfileId(profile.getId());
        return saveTenant(tenant);
    }

    private TenantProfile createTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Tenant Profile");
        tenantProfile.setDescription("Tenant Profile" + " Test");

        TenantProfileData tenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .warnThreshold(WARN_THRESHOLD_VALUE)
                .build();

        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        return tenantProfile;
    }

}
