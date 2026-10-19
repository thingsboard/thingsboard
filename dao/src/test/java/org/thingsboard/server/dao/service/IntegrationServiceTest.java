// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class IntegrationServiceTest extends AbstractServiceTest {

    @Autowired
    ConverterService converterService;
    @MockitoSpyBean
    IntegrationDao integrationDao;
    @Autowired
    IntegrationService integrationService;

    private final IdComparator<Integration> idComparator = new IdComparator<>();

    private final String INTEGRATION_BASE_NAME = "INTEGRATION_";

    private final JsonNode CUSTOM_CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

    private final ObjectNode INTEGRATION_CONFIGURATION = JacksonUtil.newObjectNode()
            .putObject("metadata").put("key1", "val1");

    private final List<Integration> savedIntegrations = new LinkedList<>();

    private ConverterId converterId;

    @Before
    public void beforeRun() {
        Converter savedConverter = createConverter(tenantId);
        converterId = savedConverter.getId();
    }

    private Converter createConverter(TenantId tenantId) {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        return converterService.saveConverter(converter);
    }

    @After
    public void after() {
        clearSavedIntegrations();
    }

    @Test
    public void testSaveIntegration() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Assert.assertNotNull(savedIntegration);
        Assert.assertNotNull(savedIntegration.getId());
        Assert.assertTrue(savedIntegration.getCreatedTime() > 0);
        Assert.assertEquals(integration.getTenantId(), savedIntegration.getTenantId());
        Assert.assertEquals(integration.getDefaultConverterId(), savedIntegration.getDefaultConverterId());
        Assert.assertEquals(integration.getRoutingKey(), savedIntegration.getRoutingKey());

        savedIntegration.setName("My new integration");

        integrationService.saveIntegration(savedIntegration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertEquals(foundIntegration.getName(), savedIntegration.getName());

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test
    public void testSaveIntegrationWithEmptyRoutingKey() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Assertions.assertThrows(DataValidationException.class, () -> {
            integrationService.saveIntegration(integration);
        });
    }

    @Test
    public void testSaveIntegrationWithEmptyTenant() {
        Integration integration = new Integration();
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Assertions.assertThrows(DataValidationException.class, () -> {
            integrationService.saveIntegration(integration);
        });
    }

    @Test
    public void testSaveIntegrationWithInvalidTenant() {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(converterId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integration.setTenantId(new TenantId(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            integrationService.saveIntegration(integration);
        });
    }

    @Test
    public void testSaveIntegrationWithEmptyConverterId() {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setTenantId(tenantId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Assertions.assertThrows(DataValidationException.class, () -> {
            integrationService.saveIntegration(integration);
        });
    }

    @Test
    public void testUpdateIntegrationType() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        savedIntegration.setType(IntegrationType.HTTP);
        Assertions.assertThrows(DataValidationException.class, () -> {
            integrationService.saveIntegration(savedIntegration);
        });
    }

    @Test
    public void testFindIntegrationById() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);
        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test
    public void testDeleteIntegration() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNotNull(foundIntegration);
        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
        foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNull(foundIntegration);
    }

    @Test
    public void testFindTenantIntegrations() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();
        Converter converter = createConverter(tenantId);
        ConverterId converterId = converter.getId();

        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            integration.setDefaultConverterId(converterId);
            integration.setName("Integration" + i);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            integrations.add(integrationService.saveIntegration(integration));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Integration> pageData = null;
        do {
            pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        integrations.sort(idComparator);
        loadedIntegrations.sort(idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        integrationService.deleteIntegrationsByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindIntegrationUseCache() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(0)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);

        for (int i = 0; i < 10; i++) {
            foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
            verify(integrationDao, times(0)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
            Assert.assertNotNull(foundIntegration);
            Assert.assertEquals(savedIntegration, foundIntegration);
        }

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test
    public void testSaveIntegrationEvictCache() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(0)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);


        savedIntegration.setName("New name");
        savedIntegration = integrationService.saveIntegration(savedIntegration);

        foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(0)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test
    public void testDeleteIntegrationEvictCache() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(0)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());

        foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNull(foundIntegration);
        verify(integrationDao, times(1)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
    }

    @Test
    public void testDeleteIntegrationsByTenantIdEvictAllEntries() {
        final int integrationNumber = 10;
        List<Integration> savedIntegrations = new LinkedList<>();
        for (int i = 0; i < integrationNumber; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            integration.setDefaultConverterId(converterId);
            integration.setName("My integration" + i);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            savedIntegrations.add(
                    integrationService.saveIntegration(integration)
            );
        }

        for (int i = 0; i < integrationNumber; i++) {
            Integration integration = savedIntegrations.get(i);
            Integration foundIntegration = integrationService.findIntegrationById(integration.getTenantId(), integration.getId());
            verify(integrationDao, times(0)).findById(eq(tenantId), eq(integration.getUuidId()));
            Assert.assertNotNull(foundIntegration);
            Assert.assertEquals(integration, foundIntegration);
        }

        integrationService.deleteIntegrationsByTenantId(tenantId);

        for (int i = 0; i < integrationNumber; i++) {
            Integration integration = savedIntegrations.get(i);
            Integration foundIntegration = integrationService.findIntegrationById(integration.getTenantId(), integration.getId());
            verify(integrationDao, times(1)).findById(eq(tenantId), eq(integration.getUuidId()));
            Assert.assertNull(foundIntegration);
        }
    }

    @Test
    public void testFindAllCoreIntegrations() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<Integration> integrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertNotEquals("List with integrations expected, but list is empty!", 0, integrations.size());
        Assert.assertEquals("List of found integrations doesn't correspond the size of previously saved integrations!",
                numIntegrations, integrations.size()
        );

        boolean allMatch = savedIntegrations.stream()
                .allMatch(integration ->
                        integrations.stream()
                                .anyMatch(foundIntegration -> foundIntegration.getId().equals(integration.getId())
                                )
                );
        Assert.assertTrue("Found integrations don't correspond the created integrations!", allMatch);
    }

    @Test
    public void testFindAllCoreIntegrationsAfterRemovingIntegrationCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<Integration> integrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertNotEquals("List with integrations expected, but list is empty!", 0, integrations.size());
        Assert.assertEquals("List of found integrations doesn't correspond the size of previously saved integrations!",
                numIntegrations, integrations.size()
        );

        boolean allMatch = savedIntegrations.stream()
                .allMatch(integration ->
                        integrations.stream()
                                .anyMatch(foundIntegration -> foundIntegration.getId().equals(integration.getId())
                                )
                );
        Assert.assertTrue("Found integrations don't correspond the created integrations!", allMatch);
        clearSavedIntegrations();


        List<Integration> emptyIntegrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integrations is null!", emptyIntegrations);
        Assert.assertEquals("List with integrations expected to be empty, but it's not!", 0, emptyIntegrations.size());

    }

    @Test
    public void testFindAllCoreIntegrationsTypeIsNullCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }


        List<Integration> integrations = integrationService
                .findAllCoreIntegrations(null, false, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertEquals("List with integrations expected to be empty, but it's not!", 0, integrations.size());
    }

    @Test
    public void testFindAllCoreIntegrationsDifferentTypesCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    i % 2 == 0 ? IntegrationType.OCEANCONNECT : IntegrationType.MQTT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<Integration> integrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertNotEquals("List with integrations expected, but list is empty!", 0, integrations.size());
        Assert.assertEquals("List of found integrations doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrations.size()
        );
        boolean allMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrations.stream().anyMatch(foundIntegration -> foundIntegration.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integrations don't correspond the created integrations!", allMatch);
        boolean allTypeMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allTypeMatch &= integration.getType().equals(IntegrationType.OCEANCONNECT);
        }
        Assert.assertTrue("Found integrations have different type!", allTypeMatch);


        integrations = integrationService
                .findAllCoreIntegrations(IntegrationType.MQTT, false, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertNotEquals("List with integrations expected, but list is empty!", 0, integrations.size());
        Assert.assertEquals("List of found integrations doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrations.size()
        );
        for (int i = 1; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrations.stream().anyMatch(foundIntegration -> foundIntegration.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integrations don't correspond the created integrations!", allMatch);
        for (int i = 1; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allTypeMatch &= integration.getType().equals(IntegrationType.MQTT);
        }
        Assert.assertTrue("Found integrations have different type!", allTypeMatch);


        integrations = integrationService
                .findAllCoreIntegrations(IntegrationType.HTTP, false, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertEquals("List with integrations expected to be empty, but it's not!", 0, integrations.size());
    }

    @Test
    public void testFindAllCoreIntegrationsRemoteEnabledCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            boolean isOdd = i % 2 == 0;

            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    isOdd,
                    !isOdd
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<Integration> remoteIntegrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, true, false);
        Assert.assertNotNull("List of found integrations is null!", remoteIntegrations);
        Assert.assertNotEquals("List with integrations expected, but list is empty!", 0, remoteIntegrations.size());
        Assert.assertEquals("List of found integrations doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, remoteIntegrations.size()
        );
        boolean allMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= remoteIntegrations.stream().anyMatch(foundIntegration -> foundIntegration.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integrations don't correspond the created integrations!", allMatch);
        boolean allRemote = remoteIntegrations.stream()
                .allMatch(Integration::isRemote);
        Assert.assertTrue("Found integrations expected to be remote, but they aren't!", allRemote);


        List<Integration> enabledIntegrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, false, true);
        Assert.assertNotNull("List of found integrations is null!", remoteIntegrations);
        Assert.assertNotEquals("List with integrations expected, but list is empty!", 0, remoteIntegrations.size());
        Assert.assertEquals("List of found integrations doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, remoteIntegrations.size()
        );
        for (int i = 1; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= enabledIntegrations.stream().anyMatch(foundIntegration -> foundIntegration.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integrations don't correspond the created integrations!", allMatch);
        boolean allEnabled = enabledIntegrations.stream()
                .allMatch(Integration::isEnabled);
        Assert.assertTrue("Found integrations expected to be enabled, but they aren't!", allEnabled);


        List<Integration> integrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertEquals("List with integrations expected to be empty, but it's not!", 0, integrations.size());
    }

    @Test
    public void testFindAllCoreIntegrationsDifferentTypeRemoteEnabledCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            boolean isOdd = i % 2 == 0;

            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    isOdd ? IntegrationType.OCEANCONNECT : IntegrationType.MQTT,
                    isOdd,
                    !isOdd
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<Integration> integrations = integrationService
                .findAllCoreIntegrations(IntegrationType.OCEANCONNECT, true, false);
        Assert.assertNotNull("List of found integrations is null!", integrations);
        Assert.assertNotEquals("List with integrations expected, but list is empty!", 0, integrations.size());
        Assert.assertEquals("List of found integrations doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrations.size()
        );
        boolean allMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrations.stream().anyMatch(foundIntegration -> foundIntegration.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integrations don't correspond the created integrations!", allMatch);
        boolean allRemote = integrations.stream()
                .allMatch(Integration::isRemote);
        Assert.assertTrue("Found integrations expected to be remote, but they aren't!", allRemote);
    }

    private Integration saveIntegration(UUID id, TenantId tenantId, ConverterId converterId, String name, String routingKey, IntegrationType type, boolean isRemote, boolean isEnabled) {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(id));
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        integration.setRemote(isRemote);
        integration.setEnabled(isEnabled);
        return integrationService.saveIntegration(integration);
    }

    private void clearSavedIntegrations() {
        if (!savedIntegrations.isEmpty()) {
            savedIntegrations.forEach(integration ->
                    integrationService.deleteIntegration(
                            integration.getTenantId(),
                            integration.getId()
                    )
            );
            savedIntegrations.clear();
        }
    }

}
