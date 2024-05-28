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
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.sync.tenant.TenantExportService;
import org.thingsboard.server.service.sync.tenant.TenantImportService;
import org.thingsboard.server.service.sync.tenant.util.TenantExportConfig;
import org.thingsboard.server.service.sync.tenant.util.TenantExportResult;
import org.thingsboard.server.service.sync.tenant.util.TenantImportResult;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.service.tenant.EntityUtils.easyRandom;
import static org.thingsboard.server.service.tenant.EntityUtils.newRandomizedEntity;

@DaoSqlTest
public class TenantExportServiceTest extends AbstractControllerTest {

    @Autowired
    private TenantExportService tenantExportService;
    @Autowired
    private TenantImportService tenantImportService;
    @Autowired
    private EventService eventService;
    @Autowired
    private TimeseriesService timeseriesService;
    @Autowired
    private AttributesService attributesService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private EntityDaoRegistry entityDaoRegistry;

    private TenantId tenantId;

    @Before
    public void beforeEach() throws Exception {
    }

    @After
    public void afterEach() {

    }

    @Test
    public void testExportImport() throws Exception {
        Tenant tenant = newRandomizedEntity(Tenant.class);
        tenant.setTenantProfileId(tenantProfileId);
        saveEntity(tenant);
        this.tenantId = tenant.getId();

        fillTenant();

        TenantExportResult exportResult = exportTenant(tenantId);
        byte[] data = tenantExportService.downloadResult(tenantId).getBody().getContentAsByteArray();

        loginSysAdmin();
        deleteTenant(tenantId);

        TenantImportResult importResult = importTenant(data);

    }


    private TenantExportResult exportTenant(TenantId tenantId) throws Exception {
        TenantExportConfig exportConfig = new TenantExportConfig();
        exportConfig.setTenantId(tenantId.getId());
        tenantExportService.exportTenant(exportConfig);
        TenantExportResult result = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> tenantExportService.getResult(tenantId), TenantExportResult::isDone);
        System.err.println(result);
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getError());
        }
        return result;
    }

    private TenantImportResult importTenant(byte[] data) throws Exception {
        TenantId tenantId = tenantImportService.importTenant(new ByteArrayInputStream(data));
        TenantImportResult result = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> tenantImportService.getResult(tenantId), TenantImportResult::isDone);
        System.err.println(result);
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getError());
        }
        return result;
    }

    private void fillTenant() {
        Customer customer = newRandomizedEntity(Customer.class);
        customer.setTenantId(tenantId);
        saveEntity(customer);

        AdminSettings adminSettings = newRandomizedEntity(AdminSettings.class);
        adminSettings.setTenantId(tenantId);
        saveEntity(adminSettings);

        Queue queue = newRandomizedEntity(Queue.class);
        queue.setTenantId(tenantId);
        saveEntity(queue);

        Rpc rpc = newRandomizedEntity(Rpc.class);
        rpc.setTenantId(tenantId);
        saveEntity(rpc);

        RuleChain ruleChain = newRandomizedEntity(RuleChain.class);
        ruleChain.setTenantId(tenantId);
        saveEntity(ruleChain);

        for (int i = 0; i < 10; i++) {
            RuleNode ruleNode = newRandomizedEntity(RuleNode.class);
            ruleNode.setRuleChainId(ruleChain.getId());
            saveEntity(ruleNode);
            createRelation(ruleNode.getId(), ruleChain.getId());
            createRelation(ruleChain.getId(), ruleNode.getId());
        }

        Dashboard dashboard = newRandomizedEntity(Dashboard.class);
        dashboard.setTenantId(tenantId);
        saveEntity(dashboard);

        DeviceProfile deviceProfile = newRandomizedEntity(DeviceProfile.class);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setFirmwareId(null);
        deviceProfile.setSoftwareId(null);
        deviceProfile.setDefaultEdgeRuleChainId(null);
        deviceProfile.setDefaultRuleChainId(ruleChain.getId());
        deviceProfile.setDefaultDashboardId(dashboard.getId());
        saveEntity(deviceProfile);

        OtaPackage otaPackage = newRandomizedEntity(OtaPackage.class);
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfile.getId());
        saveEntity(otaPackage);
//        deviceProfile.setFirmwareId(otaPackage.getId()); // fixme - constraint
//        deviceProfile.setSoftwareId(otaPackage.getId());
//        saveEntity(deviceProfile);
    }

    private <T> T saveEntity(T entity) {
        return saveEntity(entity, entity.getClass().getSimpleName());
    }

    private <T> T saveEntity(T entity, String type) {
        entity = (T) entityDaoRegistry.getTenantEntityDao(type).save(tenantId, entity);
        if (entity instanceof HasId withId && withId.getId() instanceof EntityId entityId) {
            createAttribute(entityId);
            createTelemetry(entityId);
            createAuditLog(entityId);
            createEvent(entityId);
        }
        System.err.println("Created " + type + ": " + entity);
        return entity;
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
        saveEntity(auditLog);
    }

}
