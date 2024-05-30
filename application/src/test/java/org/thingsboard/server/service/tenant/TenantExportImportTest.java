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

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
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
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.sync.tenant.util.TenantExportConfig;
import org.thingsboard.server.service.sync.tenant.util.TenantExportResult;
import org.thingsboard.server.service.sync.tenant.util.TenantImportResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ObjectType.ADMIN_SETTINGS;
import static org.thingsboard.server.common.data.ObjectType.CUSTOMER;
import static org.thingsboard.server.common.data.ObjectType.DASHBOARD;
import static org.thingsboard.server.common.data.ObjectType.DEVICE_PROFILE;
import static org.thingsboard.server.common.data.ObjectType.OTA_PACKAGE;
import static org.thingsboard.server.common.data.ObjectType.QUEUE;
import static org.thingsboard.server.common.data.ObjectType.RPC;
import static org.thingsboard.server.common.data.ObjectType.RULE_CHAIN;
import static org.thingsboard.server.common.data.ObjectType.RULE_NODE;
import static org.thingsboard.server.common.data.ObjectType.TENANT;
import static org.thingsboard.server.service.tenant.EntityUtils.easyRandom;
import static org.thingsboard.server.service.tenant.EntityUtils.newRandomizedEntity;

@DaoSqlTest
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

    private TenantId tenantId;
    private Map<ObjectType, List<Object>> createdEntities;

    @Before
    public void before() throws Exception {
        createdEntities = new HashMap<>();
        loginSysAdmin();
    }

    @After
    public void after() {

    }

    @Test
    public void testExportImport() throws Exception {
        Tenant tenant = newRandomizedEntity(Tenant.class);
        tenant.setTenantProfileId(tenantProfileId);
        saveEntity(tenant, TENANT);
        this.tenantId = tenant.getId();
        fillTenant();

        TenantExportResult exportResult = exportTenant(tenantId);
        createdEntities.forEach((type, entities) -> {
            AtomicInteger exportedCount = exportResult.getStats().get(type);
            if (exportedCount.get() != entities.size()) {
                fail("expected " + entities.size() + " exported entities of type " + type + ". expected: " + entities + ", actual: " + findAllOfType(type));
                return;
            }
            assertThat(findAllOfType(type)).containsAll(entities);
        });
        byte[] exportData = doGet("/api/tenant/export/result/" + tenantId + "/download").andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        loginSysAdmin();
        deleteTenant(tenantId);
        createdEntities.forEach((type, entities) -> {
            assertThat(findAllOfType(type)).isEmpty();
        });

        TenantImportResult importResult = importTenant(exportData);
        createdEntities.forEach((type, entities) -> {
            AtomicInteger importedCount = importResult.getStats().get(type);
            if (importedCount.get() != entities.size()) {
                fail("expected " + entities.size() + " imported entities of type " + type + ". expected: " + entities + ", actual: " + findAllOfType(type));
                return;
            }
            assertThat(findAllOfType(type)).containsAll(entities);
        });
    }

    private TenantExportResult exportTenant(TenantId tenantId) {
        TenantExportConfig exportConfig = new TenantExportConfig();
        exportConfig.setTenantId(tenantId.getId());
        doPost("/api/tenant/export", exportConfig, UUID.class);
        TenantExportResult result = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doGet("/api/tenant/export/result/" + tenantId.getId(), TenantExportResult.class), TenantExportResult::isDone);
        System.err.println(result);
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getError());
        }
        return result;
    }

    private TenantImportResult importTenant(byte[] data) throws Exception {
        MockMultipartFile dataFile = new MockMultipartFile("dataFile", "data.tar", "application/octet-stream", data);
        var request = MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/tenant/import").file(dataFile);
        setJwtToken(request);
        UUID tenantId = readResponse(mockMvc.perform(request).andExpect(status().isOk()), UUID.class);

        TenantImportResult result = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doGet("/api/tenant/import/result/" + tenantId, TenantImportResult.class), TenantImportResult::isDone);
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
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setFirmwareId(null);
        deviceProfile.setSoftwareId(null);
        deviceProfile.setDefaultEdgeRuleChainId(null);
        deviceProfile.setDefaultRuleChainId(ruleChain.getId());
        deviceProfile.setDefaultDashboardId(dashboard.getId());
        saveEntity(deviceProfile, DEVICE_PROFILE);

        OtaPackage otaPackage = newRandomizedEntity(OtaPackage.class);
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(null);
        saveEntity(otaPackage, OTA_PACKAGE);

//        deviceProfile.setFirmwareId(otaPackage.getId()); // fixme - constraint
//        deviceProfile.setSoftwareId(otaPackage.getId());
//        saveEntity(deviceProfile);
    }

    private <T> T saveEntity(T entity, ObjectType type) {
        entity = (T) entityDaoRegistry.getObjectDao(type).save(tenantId, entity);
        if (entity instanceof HasId<?> withId && withId.getId() instanceof EntityId entityId) {
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
        EntityRelation relation = easyRandom.nextObject(EntityRelation.class);
        relation.setTo(to);
        relation.setFrom(from);
        relationService.saveRelation(tenantId, relation);
    }

    @SneakyThrows
    private void createAttribute(EntityId entityId) {
        attributesService.save(tenantId, entityId, easyRandom.nextObject(AttributeScope.class), newRandomizedEntity(BaseAttributeKvEntry.class)).get();
    }

    @SneakyThrows
    private void createTelemetry(EntityId entityId) {
        timeseriesService.save(tenantId, entityId, newRandomizedEntity(BasicTsKvEntry.class)).get();
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
    }

    @SneakyThrows
    private void createAuditLog(EntityId entityId) {
        AuditLog auditLog = newRandomizedEntity(AuditLog.class);
        auditLog.setTenantId(tenantId);
        auditLog.setEntityId(entityId);
        auditLogDao.save(tenantId, auditLog);
    }

}
