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
package org.thingsboard.server.service.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.housekeeper.CleanUpService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.sync.tenant.util.Result;
import org.thingsboard.server.service.sync.tenant.util.TenantExportConfig;
import org.thingsboard.server.utils.TestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ObjectType.ADMIN_SETTINGS;
import static org.thingsboard.server.common.data.ObjectType.ATTRIBUTE_KV;
import static org.thingsboard.server.common.data.ObjectType.AUDIT_LOG;
import static org.thingsboard.server.common.data.ObjectType.CUSTOMER;
import static org.thingsboard.server.common.data.ObjectType.DASHBOARD;
import static org.thingsboard.server.common.data.ObjectType.DEVICE_PROFILE;
import static org.thingsboard.server.common.data.ObjectType.EVENT;
import static org.thingsboard.server.common.data.ObjectType.LATEST_TS_KV;
import static org.thingsboard.server.common.data.ObjectType.OTA_PACKAGE;
import static org.thingsboard.server.common.data.ObjectType.QUEUE;
import static org.thingsboard.server.common.data.ObjectType.RPC;
import static org.thingsboard.server.common.data.ObjectType.RULE_CHAIN;
import static org.thingsboard.server.common.data.ObjectType.RULE_NODE;
import static org.thingsboard.server.common.data.ObjectType.TENANT;
import static org.thingsboard.server.common.data.ObjectType.TS_KV;
import static org.thingsboard.server.utils.TestUtils.newRandomizedEntity;

@DaoSqlTest
@SuppressWarnings("unchecked")
public class TenantExportImportTest extends AbstractControllerTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private AuditLogDao auditLogDao;
    @Autowired
    private TimeseriesService timeseriesService;
    @Autowired
    private AttributesService attributesService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private EntityDaoRegistry entityDaoRegistry;
    @Autowired
    private TenantDao tenantDao;
    @Autowired
    private CleanUpService cleanUpService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TenantId tenantId;
    private Map<ObjectType, List<HasId>> createdEntities;
    private Map<ObjectType, Map<EntityId, Object>> createdRelatedEntities;

    @Before
    public void before() throws Exception {
        createdEntities = new HashMap<>();
        createdRelatedEntities = new HashMap<>();
        loginSysAdmin();
    }

    @After
    public void after() {

    }

    @Test
    public void testExportImport() throws Exception {
        Tenant tenant = newRandomizedEntity(Tenant.class);
        tenant.setTenantProfileId(tenantProfileId);
        this.tenantId = tenant.getId();
        saveEntity(tenant, TENANT);
        fillTenant();

        Result<ObjectType> exportResult = exportTenant(tenantId);
        createdEntities.forEach((type, entities) -> {
            int exportedCount = exportResult.getCount(type);
            if (exportedCount != entities.size()) {
                fail("expected " + entities.size() + " exported entities of type " + type + " but was " + exportedCount + ". " +
                        "expected: " + entities + ", actual: " + findAllOfType(type));
                return;
            }
            assertAllPresent(type, entities);
        });
        byte[] exportData = doGet("/api/tenant/export/result/" + tenantId + "/download").andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        loginSysAdmin();
        createdEntities.values().forEach(entities -> {
            for (HasId entity : entities) {
                if (entity.getId() instanceof EntityId entityId) {
                    cleanUpService.cleanUpRelatedData(tenantId, entityId);
                }
            }
        });
        jdbcTemplate.execute("TRUNCATE TABLE audit_log");
        deleteTenant(tenantId);
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            createdEntities.forEach((type, entities) -> {
                assertThat(findAllOfType(type)).isEmpty();
                entities.forEach(entity -> {
                    if (entity.getId() instanceof EntityId entityId) {
                        assertThat(findAuditLogs(entityId)).isEmpty();
                        assertThat(findEvents(entityId)).isEmpty();
                        assertThat(findAttributes(entityId)).isEmpty();
                        assertThat(findLatestTs(entityId)).isEmpty();
                        TsKvEntry latestKv = (TsKvEntry) createdRelatedEntities.get(LATEST_TS_KV).get(entityId);
                        assertThat(findTsHistory(entityId, latestKv.getKey())).isEmpty();
                    }
                });
            });
        });

        Result<ObjectType> importResult = importTenant(exportData);
        createdEntities.forEach((type, entities) -> {
            int importedCount = importResult.getCount(type);
            if (importedCount != entities.size()) {
                fail("expected " + entities.size() + " imported entities of type " + type + " but was " + importedCount + ". " +
                        "expected: " + entities + ", actual: " + findAllOfType(type));
                return;
            }
            assertAllPresent(type, entities);
        });
    }

    private void assertAllPresent(ObjectType type, List<HasId> expectedEntities) {
        assertThat(findAllOfType(type)).containsAll(expectedEntities);
        expectedEntities.forEach(entity -> {
            if (entity.getId() instanceof EntityId entityId) {
                assertThat((List) findAttributes(entityId)).containsExactly(createdRelatedEntities.get(ATTRIBUTE_KV).get(entityId));
                List<TsKvEntry> latestTs = findLatestTs(entityId);
                assertThat((List) latestTs).containsExactly(createdRelatedEntities.get(LATEST_TS_KV).get(entityId));
                assertThat((List) findTsHistory(entityId, latestTs.get(0).getKey())).containsExactly(createdRelatedEntities.get(TS_KV).get(entityId));
                assertThat((List) findAuditLogs(entityId)).containsExactly(createdRelatedEntities.get(AUDIT_LOG).get(entityId));
                assertThat((List) findEvents(entityId)).containsExactly(createdRelatedEntities.get(EVENT).get(entityId));
            }
        });
    }

    private Result<ObjectType> exportTenant(TenantId tenantId) {
        TenantExportConfig exportConfig = new TenantExportConfig();
        exportConfig.setTenantId(tenantId.getId());
        doPost("/api/tenant/export", exportConfig, UUID.class);
        Result<ObjectType> result = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doGetTyped("/api/tenant/export/result/" + tenantId.getId(), new TypeReference<>() {}), Result::isDone);
        System.err.println(result);
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getError());
        }
        return result;
    }

    private Result<ObjectType> importTenant(byte[] data) throws Exception {
        MockMultipartFile dataFile = new MockMultipartFile("dataFile", "data.tar", "application/octet-stream", data);
        var request = MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/tenant/import").file(dataFile);
        setJwtToken(request);
        UUID tenantId = readResponse(mockMvc.perform(request).andExpect(status().isOk()), UUID.class);

        Result<ObjectType> result = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doGetTyped("/api/tenant/import/result/" + tenantId, new TypeReference<>() {}), Result::isDone);
        System.err.println(result);
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getError());
        }
        return result;
    }

    private void fillTenant() {
        Customer customer = newRandomizedEntity(Customer.class);
        customer.setTenantId(tenantId);
        saveEntity(customer, CUSTOMER);

        AdminSettings adminSettings = newRandomizedEntity(AdminSettings.class);
        adminSettings.setTenantId(tenantId);
        saveEntity(adminSettings, ADMIN_SETTINGS);

        Queue queue = newRandomizedEntity(Queue.class);
        queue.setTenantId(tenantId);
        saveEntity(queue, QUEUE);

        Rpc rpc = newRandomizedEntity(Rpc.class);
        rpc.setTenantId(tenantId);
        saveEntity(rpc, RPC);

        RuleChain ruleChain = newRandomizedEntity(RuleChain.class);
        ruleChain.setTenantId(tenantId);
        saveEntity(ruleChain, RULE_CHAIN);

        for (int i = 0; i < 10; i++) {
            RuleNode ruleNode = newRandomizedEntity(RuleNode.class);
            ruleNode.setRuleChainId(ruleChain.getId());
            saveEntity(ruleNode, RULE_NODE);
            createRelation(ruleNode.getId(), ruleChain.getId());
            createRelation(ruleChain.getId(), ruleNode.getId());
        }

        Dashboard dashboard = newRandomizedEntity(Dashboard.class);
        dashboard.setTenantId(tenantId);
        saveEntity(dashboard, DASHBOARD);

        DeviceProfile deviceProfile = newRandomizedEntity(DeviceProfile.class);

        OtaPackage otaPackage = newRandomizedEntity(OtaPackage.class);
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfile.getId());
        saveEntity(otaPackage, OTA_PACKAGE);

        deviceProfile.setTenantId(tenantId);
        deviceProfile.setFirmwareId(otaPackage.getId());
        deviceProfile.setSoftwareId(otaPackage.getId());
        deviceProfile.setDefaultEdgeRuleChainId(null);
        deviceProfile.setDefaultRuleChainId(ruleChain.getId());
        deviceProfile.setDefaultDashboardId(dashboard.getId());
        saveEntity(deviceProfile, DEVICE_PROFILE);
    }

    private <T extends HasId<? extends UUIDBased>> T saveEntity(T entity, ObjectType type) {
        entity = (T) entityDaoRegistry.getDao(type).save(tenantId, entity);
        if (entity.getId() instanceof EntityId entityId) {
            createAttribute(entityId);
            createTelemetry(entityId);
            createAuditLog(entityId);
            createEvent(entityId);
        }
        createdEntities.computeIfAbsent(type, k -> new ArrayList<>()).add(entity);
        return entity;
    }

    private List<Object> findAllOfType(ObjectType type) {
        if (type == TENANT) {
            Tenant tenant = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());
            if (tenant == null) {
                return Collections.emptyList();
            }
            return List.of(tenant);
        }
        TenantEntityDao<?> dao = entityDaoRegistry.getTenantEntityDao(type);
        PageData stored = dao.findAllByTenantId(tenantId, new PageLink(1000));
        return stored.getData();
    }

    private void createRelation(EntityId to, EntityId from) {
        EntityRelation relation = TestUtils.newRandomizedEntity(EntityRelation.class);
        relation.setTo(to);
        relation.setFrom(from);
        relationService.saveRelation(tenantId, relation);
    }

    @SneakyThrows
    private void createAttribute(EntityId entityId) {
        BaseAttributeKvEntry attributeKv = newRandomizedEntity(BaseAttributeKvEntry.class);
        attributesService.save(tenantId, entityId, AttributeScope.SHARED_SCOPE, attributeKv).get();
        createdRelatedEntities.computeIfAbsent(ATTRIBUTE_KV, k -> new HashMap<>()).put(entityId, attributeKv);
    }

    @SneakyThrows
    private List<AttributeKvEntry> findAttributes(EntityId entityId) {
        return attributesService.findAll(tenantId, entityId, AttributeScope.SHARED_SCOPE).get();
    }

    @SneakyThrows
    private void createTelemetry(EntityId entityId) {
        BasicTsKvEntry tsKvEntry = newRandomizedEntity(BasicTsKvEntry.class);
        timeseriesService.save(tenantId, entityId, tsKvEntry).get();
        createdRelatedEntities.computeIfAbsent(TS_KV, k -> new HashMap<>()).put(entityId, tsKvEntry);
        createdRelatedEntities.computeIfAbsent(LATEST_TS_KV, k -> new HashMap<>()).put(entityId, tsKvEntry);
    }

    @SneakyThrows
    private List<TsKvEntry> findLatestTs(EntityId entityId) {
        return timeseriesService.findAllLatest(tenantId, entityId).get();
    }

    @SneakyThrows
    private List<TsKvEntry> findTsHistory(EntityId entityId, String key) {
        return timeseriesService.findAll(tenantId, entityId, List.of(new BaseReadTsKvQuery(key, 0, System.currentTimeMillis(), 100, "ASC"))).get();
    }

    @SneakyThrows
    private void createEvent(EntityId entityId) {
        LifecycleEvent event = LifecycleEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId("test")
                .lcEventType("test")
                .success(true)
                .build();
        eventService.saveAsync(event).get();
        createdRelatedEntities.computeIfAbsent(EVENT, k -> new HashMap<>()).put(entityId, findEvents(entityId).get(0));
    }

    private List<EventInfo> findEvents(EntityId entityId) {
        return eventService.findEvents(tenantId, entityId, EventType.LC_EVENT, new TimePageLink(100, 0)).getData();
    }

    @SneakyThrows
    private void createAuditLog(EntityId entityId) {
        AuditLog auditLog = newRandomizedEntity(AuditLog.class);
        auditLog.setTenantId(tenantId);
        auditLog.setEntityId(entityId);
        auditLog = auditLogDao.save(tenantId, auditLog);
        createdRelatedEntities.computeIfAbsent(AUDIT_LOG, k -> new HashMap<>()).put(entityId, auditLog);
    }

    private List<AuditLog> findAuditLogs(EntityId entityId) {
        return auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId.getId(), entityId, Arrays.stream(ActionType.values()).toList(), new TimePageLink(100, 0)).getData();
    }

}
