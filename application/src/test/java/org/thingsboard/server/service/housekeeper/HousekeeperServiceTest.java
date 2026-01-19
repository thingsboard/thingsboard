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
package org.thingsboard.server.service.housekeeper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateDao;
import org.thingsboard.server.gen.transport.TransportProtos.HousekeeperTaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.service.housekeeper.processor.TsHistoryDeletionTaskProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.core.housekeeper.task-reprocessing-delay-ms=2000",
        "queue.core.housekeeper.poll-interval-ms=1000",
        "queue.core.housekeeper.max-reprocessing-attempts=5",
        "queue.core.housekeeper.task-processing-timeout-ms=5000",
})
public class HousekeeperServiceTest extends AbstractControllerTest {

    @SpyBean
    private HousekeeperService housekeeperService;
    @SpyBean
    private HousekeeperReprocessingService housekeeperReprocessingService;
    @Autowired
    private EventService eventService;
    @Autowired
    private TimeseriesService timeseriesService;
    @Autowired
    private AttributesService attributesService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private AlarmService alarmService;
    @Autowired
    private AlarmDao alarmDao;
    @Autowired
    private RelationService relationService;
    @Autowired
    private ApiUsageStateDao apiUsageStateDao;
    @Autowired
    private EntityServiceRegistry entityServiceRegistry;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private DashboardService dashboardService;
    @SpyBean
    private TsHistoryDeletionTaskProcessor tsHistoryDeletionTaskProcessor;

    private TenantId tenantId;

    private static final String TELEMETRY_KEY = "telemetry1";
    private static final String ATTRIBUTE_KEY = "_attribute1";
    private static final String KV_VALUE = "ewfewfwef";

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
        this.tenantId = super.tenantId;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void whenDeviceIsDeleted_thenCleanUpRelatedData() throws Exception {
        Device device = createDevice("test", "test");
        createRelatedData(device.getId());

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoRelatedData(device.getId());
        });
    }

    @Test
    public void whenRuleChainIsDeleted_thenCleanUpRelatedData() throws Exception {
        RuleChainMetaData ruleChainMetaData = createRuleChain();
        RuleChainId ruleChainId = ruleChainMetaData.getRuleChainId();
        RuleNodeId ruleNode1Id = ruleChainMetaData.getNodes().get(0).getId();
        RuleNodeId ruleNode2Id = ruleChainMetaData.getNodes().get(1).getId();
        createRelatedData(ruleChainId);
        createRelatedData(ruleNode1Id);
        createRelatedData(ruleNode2Id);

        doDelete("/api/ruleChain/" + ruleChainId).andExpect(status().isOk());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoRelatedData(ruleNode1Id);
            verifyNoRelatedData(ruleNode2Id);
            verifyNoRelatedData(ruleChainId);
        });
    }

    @Test
    public void whenUserIsDeleted_thenCleanUpRelatedData() throws Exception {
        Device device = createDevice("test", "test");
        UserId userId = customerUserId;
        createRelatedData(userId);

        List<AlarmId> alarms = new ArrayList<>();
        int count = 112;
        for (int i = 0; i < count; i++) {
            Alarm alarm = Alarm.builder()
                    .type("test" + i)
                    .tenantId(tenantId)
                    .originator(device.getId())
                    .severity(AlarmSeverity.MAJOR)
                    .build();
            alarm = doPost("/api/alarm", alarm, Alarm.class);
            AlarmId alarmId = alarm.getId();
            alarm = doPost("/api/alarm/" + alarmId + "/assign/" + userId, "", Alarm.class);
            assertThat(alarm.getAssigneeId()).isEqualTo(userId);
            alarms.add(alarmId);
        }
        List<AlarmId> assignedAlarms = alarmService.findAlarmIdsByAssigneeId(tenantId, userId, 0, null, 5000).stream()
                .map(TbPair::getFirst).map(AlarmId::new).toList();
        assertThat(assignedAlarms).size().isEqualTo(count);
        assertThat(assignedAlarms).containsAll(alarms);

        doDelete("/api/user/" + userId).andExpect(status().isOk());

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoRelatedData(userId);
            assertThat(alarmService.findAlarmIdsByAssigneeId(tenantId, userId, 0, null, 5000)).size().isZero();
        });
    }

    @Test
    public void whenDeviceIsDeleted_thenDeleteAllAlarms() throws Exception {
        Device device = createDevice("test", "test");
        for (int i = 1; i <= 1000; i++) {
            createAlarm(device.getId());
        }

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoAlarms(device.getId());
        });
    }

    @Test
    public void whenAssetIsDeleted_thenDeleteAllAlarms() throws Exception {
        Asset asset = createAsset();
        for (int i = 1; i <= 1000; i++) {
            createAlarm(asset.getId());
        }

        doDelete("/api/asset/" + asset.getId()).andExpect(status().isOk());

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoAlarms(asset.getId());
        });
    }

    @Test
    public void whenDashboardIsDeleted_thenDeleteAllAlarms() throws Exception {
        Dashboard dashboard = createDashboard();
        for (int i = 1; i <= 1000; i++) {
            createAlarm(dashboard.getId());
        }

        doDelete("/api/dashboard/" + dashboard.getId()).andExpect(status().isOk());

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoAlarms(dashboard.getId());
        });
    }

    @Test
    public void whenCustomerIsDeleted_thenDeleteAllAlarms() throws Exception {
        Customer customer = createCustomer();
        for (int i = 1; i <= 1000; i++) {
            createAlarm(customer.getId());
        }

        doDelete("/api/customer/" + customer.getId()).andExpect(status().isOk());

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoAlarms(customer.getId());
        });
    }

    @Test
    public void whenUserIsDeleted_thenDeleteAllAlarms() throws Exception {
        UserId userId = customerUserId;
        for (int i = 1; i <= 1000; i++) {
            createAlarm(userId);
        }

        doDelete("/api/user/" + userId).andExpect(status().isOk());

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            verifyNoAlarms(userId);
        });
    }

    @Test
    public void whenTenantIsDeleted_thenDeleteAllEntitiesAndCleanUpRelatedData() throws Exception {
        loginDifferentTenant();
        tenantId = differentTenantId;

        createRelatedData(tenantId);

        MobileApp androidApp = validMobileApp(TenantId.SYS_TENANT_ID, "my.android.package", PlatformType.ANDROID);
        androidApp = doPost("/api/mobile/app", androidApp, MobileApp.class);
        MobileAppId androidAppId = androidApp.getId();

        MobileApp iosApp = validMobileApp(TenantId.SYS_TENANT_ID, "my.ios.package", PlatformType.IOS);
        iosApp = doPost("/api/mobile/app", iosApp, MobileApp.class);
        MobileAppId iosAppId = androidApp.getId();

        OAuth2Client oAuth2Client = createOauth2Client(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);
        OAuth2ClientId oAuth2ClientId = savedOAuth2Client.getId();

        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        mobileAppBundle.setTitle("Test bundle");
        mobileAppBundle.setAndroidAppId(androidApp.getId());
        mobileAppBundle.setIosAppId(iosApp.getId());

        MobileAppBundle savedAppBundle = doPost("/api/mobile/bundle?oauth2ClientIds=" + savedOAuth2Client.getId().getId(), mobileAppBundle, MobileAppBundle.class);
        MobileAppBundleId appBundleId = savedAppBundle.getId();

        createDifferentTenantCustomer();
        createRelatedData(differentTenantCustomerId);
        loginDifferentTenant();

        List<DeviceId> devices = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            Device device = createDevice("test" + i, "test" + i);
            devices.add(device.getId());
        }
        DeviceId firstDevice = devices.get(0);
        createRelatedData(firstDevice);
        DeviceId lastDevice = devices.get(devices.size() - 1);
        createRelatedData(lastDevice);

        Asset asset = createAsset();
        createRelatedData(asset.getId());
        createRelation(firstDevice, asset.getId());
        createAlarm(firstDevice, asset.getId());

        RuleChainMetaData ruleChainMetaData = createRuleChain();
        RuleChainId ruleChainId = ruleChainMetaData.getRuleChainId();
        RuleNodeId ruleNode1Id = ruleChainMetaData.getNodes().get(0).getId();
        RuleNodeId ruleNode2Id = ruleChainMetaData.getNodes().get(1).getId();
        createRelatedData(ruleChainId);
        createRelatedData(ruleNode1Id);
        createRelatedData(ruleNode2Id);

        UserId userId = savedDifferentTenantUser.getId();
        createRelatedData(userId);

        ApiUsageState tenantApiUsageState = apiUsageStateDao.findApiUsageStateByEntityId(differentTenantId);

        loginSysAdmin();
        deleteDifferentTenant();

        await().atMost(60, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            for (DeviceId deviceId : devices) {
                verifyNoRelatedData(deviceId);
            }
            verifyNoRelatedData(asset.getId());
            verifyNoRelatedData(ruleNode1Id);
            verifyNoRelatedData(ruleNode2Id);
            verifyNoRelatedData(ruleChainId);
            verifyNoRelatedData(userId);
            verifyNoRelatedData(differentTenantCustomerId);
            verifyNoRelatedData(tenantApiUsageState.getId());
            verifyNoRelatedData(androidAppId);
            verifyNoRelatedData(iosAppId);
            verifyNoRelatedData(oAuth2ClientId);
            verifyNoRelatedData(appBundleId);
            verifyNoRelatedData(tenantId);
        });
    }

    @Test
    public void whenTaskProcessingFails_thenReprocess() throws Exception {
        Exception error = new RuntimeException("Just a test");
        doThrow(error).when(tsHistoryDeletionTaskProcessor).process(any());

        Device device = createDevice("test", "test");
        createRelatedData(device.getId());

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        int attempts = 2;
        await().atMost(TIMEOUT, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            for (int i = 0; i <= attempts; i++) {
                int attempt = i;
                verify(housekeeperReprocessingService).submitForReprocessing(argThat(getTaskMatcher(device.getId(), HousekeeperTaskType.DELETE_TS_HISTORY,
                        task -> task.getAttempt() == attempt)), argThat(e -> e.getMessage().equals(error.getMessage())));
            }
        });

        assertThat(getTimeseriesHistory(device.getId())).isNotEmpty();
        doCallRealMethod().when(tsHistoryDeletionTaskProcessor).process(any());
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getTimeseriesHistory(device.getId())).isEmpty();
        });
    }

    @Test
    public void whenTaskProcessingTimedOut_thenInterruptAndReprocess() throws Exception {
        ExecutorService someExecutor = Executors.newSingleThreadExecutor();
        AtomicBoolean taskInterrupted = new AtomicBoolean(false);
        AtomicBoolean underlyingTaskInterrupted = new AtomicBoolean(false);
        doAnswer(invocationOnMock -> {
            Future<?> future = someExecutor.submit(() -> {
                try {
                    Thread.sleep(TimeUnit.HOURS.toMillis(24));
                } catch (InterruptedException e) {
                    underlyingTaskInterrupted.set(true);
                }
            });
            try {
                future.get();
            } catch (InterruptedException e) {
                taskInterrupted.set(true);
                future.cancel(true);
                throw e;
            }
            return null;
        }).when(tsHistoryDeletionTaskProcessor).process(any());

        Device device = createDevice("test", "test");
        createRelatedData(device.getId());

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        int attempts = 2;
        await().atMost(TIMEOUT, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            for (int i = 0; i <= attempts; i++) {
                int attempt = i;
                verify(housekeeperReprocessingService).submitForReprocessing(argThat(getTaskMatcher(device.getId(), HousekeeperTaskType.DELETE_TS_HISTORY,
                        task -> task.getAttempt() == attempt)), argThat(error -> error instanceof TimeoutException));
            }
        });
        assertThat(taskInterrupted).isTrue();
        assertThat(underlyingTaskInterrupted).isTrue();

        assertThat(getTimeseriesHistory(device.getId())).isNotEmpty();
        doCallRealMethod().when(tsHistoryDeletionTaskProcessor).process(any());
        someExecutor.shutdown();

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getTimeseriesHistory(device.getId())).isEmpty();
        });
    }

    @Test
    public void whenReprocessingAttemptsExceeded_thenDropTheTask() throws Exception {
        TimeoutException error = new TimeoutException("Test timeout");
        doThrow(error).when(tsHistoryDeletionTaskProcessor).process(any());

        Device device = createDevice("test", "test");
        createRelatedData(device.getId());

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        int maxAttempts = 5;
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            for (int i = 1; i <= maxAttempts; i++) {
                verifyTaskProcessing(device.getId(), HousekeeperTaskType.DELETE_TS_HISTORY, i);
            }
        });

        Mockito.clearInvocations(housekeeperService);
        doCallRealMethod().when(tsHistoryDeletionTaskProcessor).process(any());
        TimeUnit.SECONDS.sleep(2);
        verify(housekeeperService, never()).processTask(argThat(getTaskMatcher(device.getId(), HousekeeperTaskType.DELETE_TS_HISTORY, null)));
    }

    private void verifyTaskProcessing(EntityId entityId, HousekeeperTaskType taskType, int expectedAttempt) throws Exception {
        verify(housekeeperService).processTask(argThat(getTaskMatcher(entityId, taskType, task -> task.getAttempt() == expectedAttempt)));
    }

    private ArgumentMatcher<ToHousekeeperServiceMsg> getTaskMatcher(EntityId entityId, HousekeeperTaskType taskType,
                                                                    Predicate<HousekeeperTaskProto> additionalCheck) {
        return msg -> {
            HousekeeperTask task = JacksonUtil.fromString(msg.getTask().getValue(), HousekeeperTask.class);
            return task.getEntityId().equals(entityId) && task.getTaskType() == taskType && (additionalCheck == null || additionalCheck.test(msg.getTask()));
        };
    }

    private void createRelatedData(EntityId entityId) throws Exception {
        createTelemetry(entityId);
        for (AttributeScope scope : AttributeScope.values()) {
            createAttribute(entityId, scope, scope + ATTRIBUTE_KEY);
        }
        createEvent(entityId);
    }

    private void verifyNoRelatedData(EntityId entityId) throws Exception {
        assertThat(entityServiceRegistry.getServiceByEntityType(entityId.getEntityType()).findEntity(tenantId, entityId)).isEmpty();

        assertThat(getLatestTelemetry(entityId)).isNull();
        assertThat(getTimeseriesHistory(entityId)).isEmpty();
        for (AttributeScope scope : AttributeScope.values()) {
            assertThat(attributesService.findAll(tenantId, entityId, scope).get()).isEmpty();
        }
        assertThat(getEvents(entityId)).isEmpty();
        assertThat(alarmDao.findEntityAlarmRecordsByEntityId(tenantId, entityId)).isEmpty();
        verifyNoAlarms(entityId);
        assertThat(relationService.findByTo(tenantId, entityId, RelationTypeGroup.COMMON)).isEmpty();
        assertThat(relationService.findByFrom(tenantId, entityId, RelationTypeGroup.COMMON)).isEmpty();
    }

    private void verifyNoAlarms(EntityId entityId) {
        assertThat(alarmService.findAlarmIdsByOriginatorId(tenantId, entityId, 0, null, 10)).isEmpty();
    }

    private void createAttribute(EntityId entityId, AttributeScope scope, String key) throws Exception {
        attributesService.save(tenantId, entityId, scope, new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, KV_VALUE))).get();
    }

    private void createTelemetry(EntityId entityId) throws Exception {
        timeseriesService.save(tenantId, entityId, new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(TELEMETRY_KEY, KV_VALUE))).get();
    }

    private void createEvent(EntityId entityId) {
        LifecycleEvent event = LifecycleEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId("test")
                .lcEventType("test")
                .success(true)
                .build();
        eventService.saveAsync(event);
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> !getEvents(entityId).isEmpty());
    }

    private void createRelation(DeviceId to, AssetId from) {
        EntityRelation relation = new EntityRelation(from, to, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        relationService.saveRelation(tenantId, relation);
    }

    private void createAlarm(DeviceId deviceId, EntityId propagatedEntityId) {
        Alarm alarm = doPost("/api/alarm", Alarm.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .severity(AlarmSeverity.CRITICAL)
                .type("test alarm for " + deviceId)
                .propagate(true)
                .build(), Alarm.class);

        List<EntityAlarm> entityAlarms = alarmDao.findEntityAlarmRecords(tenantId, alarm.getId());
        assertThat(entityAlarms).anyMatch(entityAlarm -> entityAlarm.getEntityId().equals(deviceId) && entityAlarm.getAlarmType().equals(alarm.getType()));
        assertThat(entityAlarms).anyMatch(entityAlarm -> entityAlarm.getEntityId().equals(propagatedEntityId) && entityAlarm.getAlarmType().equals(alarm.getType()));
        assertThat(alarmService.findAlarmIdsByOriginatorId(tenantId, deviceId, 0, null, 10)).isNotEmpty();
    }

    private void createAlarm(EntityId entityId) {
        doPost("/api/alarm", Alarm.builder()
                .tenantId(tenantId)
                .originator(entityId)
                .severity(AlarmSeverity.CRITICAL)
                .type("test alarm for " + entityId + " " + RandomStringUtils.randomAlphabetic(10))
                .build(), Alarm.class);
        assertThat(alarmService.findAlarmIdsByOriginatorId(tenantId, entityId, 0, null, 10)).isNotEmpty();
    }

    private TsKvEntry getLatestTelemetry(EntityId entityId) throws Exception {
        return timeseriesService.findLatest(tenantId, entityId, HousekeeperServiceTest.TELEMETRY_KEY).get().orElse(null);
    }

    private List<TsKvEntry> getTimeseriesHistory(EntityId entityId) throws Exception {
        return timeseriesService.findAll(tenantId, entityId, List.of(new BaseReadTsKvQuery(HousekeeperServiceTest.TELEMETRY_KEY, 0, System.currentTimeMillis(), 10, "DESC"))).get();
    }

    private List<EventInfo> getEvents(EntityId entityId) {
        return eventService.findEvents(tenantId, entityId, EventType.LC_EVENT, new TimePageLink(100)).getData()
                .stream().filter(event -> Optional.ofNullable(event.getBody()).map(body -> body.get("event"))
                        .map(JsonNode::asText).orElse("").equals("test"))
                .collect(Collectors.toList());
    }

    private Asset createAsset() {
        Asset asset = new Asset();
        asset.setName("test");
        asset.setType("test");
        return doPost("/api/asset", asset, Asset.class);
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle(StringUtils.randomAlphabetic(10));
        return customerService.saveCustomer(customer);
    }

    private Dashboard createDashboard() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(StringUtils.randomAlphabetic(10));
        return dashboardService.saveDashboard(dashboard);
    }

    private RuleChainMetaData createRuleChain() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Test");
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        RuleChainId ruleChainId = ruleChain.getId();

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChainId);

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode1.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        ruleChainService.saveRuleChainMetaData(tenantId, metaData, Function.identity());
        return ruleChainService.loadRuleChainMetaData(tenantId, ruleChainId);
    }

    private MobileApp validMobileApp(TenantId tenantId, String mobileAppName, PlatformType platformType) {
        MobileApp mobileApp = new MobileApp();
        mobileApp.setTenantId(tenantId);
        mobileApp.setStatus(MobileAppStatus.DRAFT);
        mobileApp.setPkgName(mobileAppName);
        mobileApp.setPlatformType(platformType);
        mobileApp.setAppSecret(StringUtils.randomAlphanumeric(24));
        return mobileApp;
    }

}
