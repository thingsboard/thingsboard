// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.integration;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.integration.IntegrationDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JpaIntegrationDaoTest extends AbstractJpaDaoTest {

    List<Integration> savedIntegrations = new ArrayList<>();
    List<Converter> savedConverters = new ArrayList<>();

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private ConverterDao converterDao;

    @After
    public void tearDown() {
        savedIntegrations.forEach(integration -> integrationDao.removeById(integration.getTenantId(), integration.getUuidId()));
        savedIntegrations.clear();
        savedConverters.forEach(converter -> converterDao.removeById(converter.getTenantId(), converter.getUuidId()));
        savedConverters.clear();
    }

    @Test
    public void testFindIntegrationsByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID converterId1 = Uuids.timeBased();
        saveConverter(converterId1, tenantId1, "TEST_CONVERTER", ConverterType.UPLINK).getUuidId();
        saveTernary(tenantId1, converterId1);
        assertEquals(60, integrationDao.find(TenantId.SYS_TENANT_ID).size());

        PageLink pageLink = new PageLink(20, 0, "INTEGRATION_");
        PageData<Integration> integrations1 = integrationDao.findByTenantId(tenantId1, pageLink);
        assertEquals(20, integrations1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Integration> integrations2 = integrationDao.findByTenantId(tenantId1, pageLink);
        assertEquals(10, integrations2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Integration> integrations3 = integrationDao.findByTenantId(tenantId1, pageLink);
        assertEquals(0, integrations3.getData().size());
    }

    @Test
    public void testFindIntegrationByRoutingKey() {
        UUID integrationId1 = Uuids.timeBased();
        UUID integrationId2 = Uuids.timeBased();
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        UUID converterId1 = Uuids.timeBased();
        UUID converterId2 = Uuids.timeBased();
        saveConverter(converterId1, tenantId1, "TEST_CONVERTER_1", ConverterType.UPLINK).getUuidId();
        saveConverter(converterId2, tenantId1, "TEST_CONVERTER_2", ConverterType.UPLINK).getUuidId();

        String routingKey = StringUtils.randomAlphanumeric(15);
        String routingKey2 = StringUtils.randomAlphanumeric(15);
        savedIntegrations.add(saveIntegration(integrationId1, tenantId1, converterId1, "TEST_INTEGRATION", routingKey, IntegrationType.OCEANCONNECT));
        savedIntegrations.add(saveIntegration(integrationId2, tenantId2, converterId2, "TEST_INTEGRATION", routingKey2, IntegrationType.OCEANCONNECT));

        Optional<Integration> integrationOpt1 = integrationDao.findByRoutingKey(tenantId1, routingKey);
        assertTrue("Optional expected to be non-empty", integrationOpt1.isPresent());
        assertEquals(integrationId1, integrationOpt1.get().getId().getId());

        integrationOpt1 = integrationDao.findByRoutingKey(tenantId2, routingKey2);
        assertTrue("Optional expected to be non-empty", integrationOpt1.isPresent());
        assertEquals(integrationId2, integrationOpt1.get().getId().getId());

        Optional<Integration> integrationOpt2 = integrationDao.findByRoutingKey(tenantId1, "NON_EXISTENT_ROUTING_KEY");
        assertFalse("Optional expected to be empty", integrationOpt2.isPresent());
    }

    @Test
    public void testFindCoreIntegrationsProjectionPreservesBooleanFields() {
        UUID tenantId = Uuids.timeBased();
        UUID converterId = Uuids.timeBased();
        saveConverter(converterId, tenantId, "TEST_CONVERTER_CORE", ConverterType.UPLINK);

        UUID integrationId = Uuids.timeBased();
        Integration integration = new Integration();
        integration.setId(new IntegrationId(integrationId));
        integration.setTenantId(new TenantId(tenantId));
        integration.setDefaultConverterId(new ConverterId(converterId));
        integration.setName("CORE_INTEGRATION");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.MQTT);
        // Distinguishable boolean pattern: no two of the three share a value in a way that would hide a
        // transposed adjacent pair in the JPQL constructor projection (enabled/isRemote and isRemote/allow*).
        integration.setEnabled(true);
        integration.setRemote(false);
        integration.setAllowCreateDevicesOrAssets(true);
        savedIntegrations.add(integrationDao.save(new TenantId(tenantId), integration));

        // Assert through both the repository query that owns the projection and the DAO method that delegates to it.
        Integration fromRepository = findProjectedById(
                integrationRepository.findCoreIntegrations(IntegrationType.MQTT, false, true), integrationId);
        Integration fromDao = findProjectedById(
                integrationDao.findAllCoreIntegrations(IntegrationType.MQTT, false, true), integrationId);

        for (Integration projected : List.of(fromRepository, fromDao)) {
            assertEquals(integrationId, projected.getId().getId());
            assertEquals(tenantId, projected.getTenantId().getId());
            assertEquals("CORE_INTEGRATION", projected.getName());
            assertEquals(IntegrationType.MQTT, projected.getType());
            assertTrue("enabled must round-trip through the projection", projected.isEnabled());
            assertFalse("isRemote must round-trip through the projection", projected.isRemote());
            assertTrue("allowCreateDevicesOrAssets must round-trip through the projection", projected.isAllowCreateDevicesOrAssets());
        }
    }

    private Integration findProjectedById(List<Integration> integrations, UUID integrationId) {
        return integrations.stream()
                .filter(integration -> integration.getId().getId().equals(integrationId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Core integrations projection did not return the saved integration"));
    }

    private void saveTernary(UUID tenantId1, UUID converterId1) {
        UUID tenantId2 = Uuids.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID integrationId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID converterId = i % 2 == 0 ? converterId1 : saveConverter(Uuids.timeBased(), tenantId1, "TEST_CONVERTER_" + i, ConverterType.UPLINK).getUuidId();
            savedIntegrations.add(saveIntegration(integrationId, tenantId, converterId, "INTEGRATION_" + i, StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT));
        }
    }

    private Integration saveIntegration(UUID id, UUID tenantId, UUID converterId, String name, String routingKey, IntegrationType type) {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(id));
        integration.setTenantId(new TenantId(tenantId));
        integration.setDefaultConverterId(new ConverterId(converterId));
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        return integrationDao.save(new TenantId(tenantId), integration);
    }

    private Converter saveConverter(UUID id, UUID tenantId, String name, ConverterType type) {
        Converter converter = new Converter();
        converter.setId(new ConverterId(id));
        converter.setTenantId(new TenantId(tenantId));
        converter.setName(name);
        converter.setType(type);
        Converter savedConverter = converterDao.save(new TenantId(tenantId), converter);
        savedConverters.add(savedConverter);
        return savedConverter;
    }
}
