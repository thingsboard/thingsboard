// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.DedicatedConverterConfig;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.DataValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_AWS_IOT_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_KNP_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_SIGFOX_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_THINGSPARK_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TPE_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
})
@DaoSqlTest
public class ConverterControllerTest extends AbstractControllerTest {
    private IdComparator<Converter> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    private static final JsonNode CUSTOM_UPLINK_CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");
    private static final JsonNode CUSTOM_DOWNLINK_CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("encoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

    private static final String DEFAULT_AWS_IOT_UPLINK_DECODER = "converters/tbel-aws-iot-decoder.raw";
    private static final String DEFAULT_AZURE_UPLINK_DECODER = "converters/tbel-azure-decoder.raw";
    private static final String DEFAULT_CHIRPSTACK_UPLINK_DECODER = "converters/tbel-chirpstack-decoder.raw";
    private static final String CHIRPSTACK_UPLINK_DECODER = "converter/chirpstack_uplink_decoder.raw";
    private static final String DEFAULT_KPN_UPLINK_DECODER = "converters/tbel-kpn-decoder.raw";
    private static final String DEFAULT_LORIOT_UPLINK_DECODER = "converters/tbel-loriot-decoder.raw";
    private static final String LORIOT_UPLINK_DECODER = "converter/loriot_uplink_decoder.raw";
    private static final String DEFAULT_SIGFOX_UPLINK_DECODER = "converters/tbel-sigfox-decoder.raw";
    private static final String DEFAULT_THINGPARK_UPLINK_DECODER = "converters/tbel-thingpark-decoder.raw";
    private static final String THINGPARK_UPLINK_DECODER = "converter/thingpark_uplink_decoder.raw";
    private static final String DEFAULT_TPE_UPLINK_DECODER = "converters/tbel-tpe-decoder.raw";
    private static final String TPE_UPLINK_DECODER = "converter/tpe_uplink_decoder.raw";
    private static final String DEFAULT_TTI_UPLINK_DECODER = "converters/tbel-tti-decoder.raw";
    private static final String TTI_UPLINK_DECODER = "converter/tti_uplink_decoder.raw";
    private static final String DEFAULT_TTN_UPLINK_DECODER = "converters/tbel-ttn-decoder.raw";
    private static final String TTN_UPLINK_DECODER = "converter/ttn_uplink_decoder.raw";
    private static final String TTN_UPLINK_JSON_DECODER = "converter/ttn_uplink_json_decoder.raw";
    private static final String LORIOT_UPLINK_DECODER_RETURN_ARRAY = "converter/loriot_uplink_decoder_array.raw";
    private static final String TTN_DECODED_PAYLOAD = "payload/ttn_payload_with_decoded.json";

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveConverter() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);

        Mockito.reset(tbClusterService, auditLogService);

        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Assert.assertNotNull(savedConverter);
        Assert.assertNotNull(savedConverter.getId());
        Assert.assertTrue(savedConverter.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedConverter.getTenantId());
        Assert.assertEquals(converter.getName(), savedConverter.getName());

        testNotifyEntityBroadcastEntityStateChangeEventManyTimeMsgToEdgeServiceNever(savedConverter, savedConverter.getId(), savedConverter.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, 1);

        savedConverter.setName("My new converter");
        savedConverter = doPost("/api/converter", savedConverter, Converter.class);

        Converter foundConverter = doGet("/api/converter/" + savedConverter.getId().getId().toString(), Converter.class);
        Assert.assertEquals(foundConverter.getName(), savedConverter.getName());

        testNotifyEntityBroadcastEntityStateChangeEventManyTimeMsgToEdgeServiceNever(savedConverter, savedConverter.getId(), savedConverter.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED, 1);
    }

    @Test
    public void testFindConverterById() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);
        Converter foundConverter = doGet("/api/converter/" + savedConverter.getId().getId().toString(), Converter.class);
        Assert.assertNotNull(foundConverter);
        Assert.assertEquals(savedConverter, foundConverter);
    }

    @Test
    public void testDeleteConverter() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Mockito.reset(tbClusterService, auditLogService);

        String converterIdStr = savedConverter.getId().getId().toString();
        doDelete("/api/converter/" + converterIdStr)
                .andExpect(status().isOk());

        doGet("/api/converter/" + converterIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Converter", converterIdStr))));

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedConverter, savedConverter.getId(), savedConverter.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, converterIdStr);

    }

    @Test
    public void testSaveConverterWithEmptyType() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Converter type " + msgErrorShouldBeSpecified;
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        converter.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(converter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException(msgError));
    }

    @Test
    public void testUpdateConverterType() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        converter.setType(ConverterType.UPLINK);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        savedConverter.setType(ConverterType.DOWNLINK);
        savedConverter.setConfiguration(CUSTOM_DOWNLINK_CONVERTER_CONFIGURATION);

        String msgError = "Converter type cannot be changed!";
        doPost("/api/converter", savedConverter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));
    }

    @Test
    public void testSaveConverterEnsuresUniqueNameAndType() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        converter.setType(ConverterType.UPLINK);
        converter.setTenantId(savedTenant.getId());
        doPost("/api/converter", converter, Converter.class);

        String msgError = "Converter with such name and type already exists!";
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));
    }

    @Test
    public void testUpdateConverterEnsuresUniqueNameAndType() throws Exception {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        converter.setType(ConverterType.UPLINK);
        converter.setTenantId(savedTenant.getId());
        doPost("/api/converter", converter, Converter.class);

        converter.setName("My converter 2");
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        savedConverter.setName("My converter");
        String msgError = "Converter with such name and type already exists!";
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));
    }

    @Test
    public void testSaveConvertersWithSameNameDifferentType() {
        Converter uplinkConverter = new Converter();
        uplinkConverter.setName("My converter");
        uplinkConverter.setType(ConverterType.UPLINK);
        uplinkConverter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        uplinkConverter.setTenantId(savedTenant.getId());
        Converter savedUplinkConverter = doPost("/api/converter", uplinkConverter, Converter.class);
        Assert.assertNotNull(savedUplinkConverter);
        Assert.assertNotNull(savedUplinkConverter.getId());

        Converter downlinkConverter = new Converter();
        downlinkConverter.setName("My converter");
        downlinkConverter.setType(ConverterType.DOWNLINK);
        downlinkConverter.setConfiguration(CUSTOM_DOWNLINK_CONVERTER_CONFIGURATION);
        downlinkConverter.setTenantId(savedTenant.getId());
        Converter savedDownlinkConverter = doPost("/api/converter", downlinkConverter, Converter.class);
        Assert.assertNotNull(savedDownlinkConverter);
        Assert.assertNotNull(savedDownlinkConverter.getId());
    }

    @Test
    public void testUpdateConverterWithSameNameDifferentType() {
        Converter uplinkConverter = new Converter();
        uplinkConverter.setName("My converter");
        uplinkConverter.setType(ConverterType.UPLINK);
        uplinkConverter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        uplinkConverter.setTenantId(savedTenant.getId());
        Converter savedUplinkConverter = doPost("/api/converter", uplinkConverter, Converter.class);
        Assert.assertNotNull(savedUplinkConverter);
        Assert.assertNotNull(savedUplinkConverter.getId());

        Converter downlinkConverter = new Converter();
        downlinkConverter.setName("Another converter");
        downlinkConverter.setType(ConverterType.DOWNLINK);
        downlinkConverter.setConfiguration(CUSTOM_DOWNLINK_CONVERTER_CONFIGURATION);
        downlinkConverter.setTenantId(savedTenant.getId());
        Converter savedDownlinkConverter = doPost("/api/converter", downlinkConverter, Converter.class);
        Assert.assertNotNull(savedDownlinkConverter);
        Assert.assertNotNull(savedDownlinkConverter.getId());

        savedDownlinkConverter.setName("My converter");
        Converter updatedDownlinkConverter = doPost("/api/converter", savedDownlinkConverter, Converter.class);
        Assert.assertNotNull(updatedDownlinkConverter);
        Assert.assertEquals("My converter", updatedDownlinkConverter.getName());
        Assert.assertEquals(ConverterType.DOWNLINK, updatedDownlinkConverter.getType());
    }

    @Test
    public void testSaveConverterWithEmptyName() throws Exception {
        Converter converter = new Converter();
        converter.setType(ConverterType.UPLINK);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Converter name " + msgErrorShouldBeSpecified;
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        converter.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(converter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException(msgError));
    }


    @Test
    public void testSaveConverterWithEmptyConfiguration() throws Exception {
        Converter converter = new Converter();
        converter.setType(ConverterType.UPLINK);
        converter.setName("My converter");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Converter configuration " + msgErrorShouldBeSpecified;
        doPost("/api/converter", converter)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        converter.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(converter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindTenantConverters() throws Exception {
        List<Converter> converters = new ArrayList<>();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 178;
        for (int i = 0; i < cntEntity; i++) {
            Converter converter = new Converter();
            converter.setName("Converter" + i);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
            converters.add(doPost("/api/converter", converter, Converter.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Converter(), new Converter(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);

        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);
    }

    @Test
    public void testFindTenantConvertersByIntegrationType() throws Exception {
        int mqttCntEntity = 27;
        for (int i = 0; i < mqttCntEntity; i++) {
            Converter converter = new Converter();
            converter.setName("Mqtt converter" + i);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
            converter.setIntegrationType(IntegrationType.MQTT);
            doPost("/api/converter", converter, Converter.class);
        }

        int httpCntEntity = 35;
        for (int i = 0; i < httpCntEntity; i++) {
            Converter converter = new Converter();
            converter.setName("Http converter" + i);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
            converter.setIntegrationType(IntegrationType.HTTP);
            doPost("/api/converter", converter, Converter.class);
        }

        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(mqttCntEntity + httpCntEntity, loadedConverters.size());

        List<Converter> loadedMqttConverters = new ArrayList<>();
        pageLink = new PageLink(23);
        do {
            pageData = doGetTypedWithPageLink("/api/converters?integrationType=MQTT&",
                    new TypeReference<>() {
                    }, pageLink);
            loadedMqttConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(mqttCntEntity, loadedMqttConverters.size());

        List<Converter> loadedHttpConverters = new ArrayList<>();
        pageLink = new PageLink(23);
        do {
            pageData = doGetTypedWithPageLink("/api/converters?integrationType=HTTP&",
                    new TypeReference<>() {
                    }, pageLink);
            loadedHttpConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(httpCntEntity, loadedHttpConverters.size());
    }

    @Test
    public void testFindTenantConvertersBySearchText() throws Exception {
        String title1 = "Converter title 1";
        List<Converter> converters = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Converter converter = new Converter();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            converter.setName(name);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
            converters.add(doPost("/api/converter", converter, Converter.class));
        }
        String title2 = "Converter title 2";
        List<Converter> converters1 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Converter converter = new Converter();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            converter.setName(name);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
            converters1.add(doPost("/api/converter", converter, Converter.class));
        }

        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Converter> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);

        List<Converter> loadedConverters1 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/converters?",
                    new TypeReference<PageData<Converter>>() {
                    }, pageLink);
            loadedConverters1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(converters1, idComparator);
        Collections.sort(loadedConverters1, idComparator);

        Assert.assertEquals(converters1, loadedConverters1);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = loadedConverters.size();
        for (Converter converter : loadedConverters) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new Converter(), new Converter(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, ActionType.DELETED, cntEntity, cntEntity, 1);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<PageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Converter converter : loadedConverters1) {
            doDelete("/api/converter/" + converter.getId().getId().toString())
                    .andExpect(status().isOk());
        }
        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/converters?",
                new TypeReference<PageData<Converter>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void whenDeletingConverter_thenReferencingIntegrationsAreChecked() throws Exception {
        Converter uplinkConverter = new Converter();
        uplinkConverter.setName("UP");
        uplinkConverter.setType(ConverterType.UPLINK);
        uplinkConverter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        uplinkConverter = doPost("/api/converter", uplinkConverter, Converter.class);

        Converter downlinkConverter = new Converter();
        downlinkConverter.setName("DOWN");
        downlinkConverter.setType(ConverterType.DOWNLINK);
        downlinkConverter.setConfiguration(JacksonUtil.newObjectNode().set("encoder", new TextNode("ab")));
        downlinkConverter = doPost("/api/converter", downlinkConverter, Converter.class);

        Integration integration = new Integration();
        integration.setName("III");
        integration.setType(IntegrationType.HTTP);
        integration.setRoutingKey("rrr");
        integration.setDefaultConverterId(uplinkConverter.getId());
        integration.setDownlinkConverterId(downlinkConverter.getId());
        integration.setEnabled(false);
        doPost("/api/integration", integration, Integration.class);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "The converter referenced by the integration cannot be deleted!";
        doDelete("/api/converter/" + uplinkConverter.getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(uplinkConverter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, new DataValidationException(msgError), uplinkConverter.getId().getId().toString());

        Mockito.reset(tbClusterService, auditLogService);

        msgError = "The downlink converter referenced by the integration cannot be deleted!";
        doDelete("/api/converter/" + downlinkConverter.getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(downlinkConverter,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, new DataValidationException(msgError), downlinkConverter.getId().getId().toString());
    }

    @Test
    public void testGettingDefaultPayloadForConverters() throws Exception {
        Map<IntegrationType, String> expectedPayloads = Map.of(
                IntegrationType.LORIOT, DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE,
                IntegrationType.CHIRPSTACK, DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE,
                IntegrationType.TTN, DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE,
                IntegrationType.TTI, DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE);

        for (IntegrationType integrationType : expectedPayloads.keySet()) {
            String integrationName = "TEST_" + integrationType;
            String urlTemplate = "/api/converter/{converterId}/debugIn?integrationName={integrationName}&integrationType={integrationType}&converterType={converterType}";
            ObjectNode resultNode = doGet(urlTemplate, ObjectNode.class, EntityId.NULL_UUID, integrationName, integrationType.name(), ConverterType.UPLINK.name());

            Assert.assertNotNull(resultNode);
            String metadataStr = resultNode.get("inMetadata").asText();
            Assert.assertFalse(String.format("Returned metadata is empty for integration: %s", integrationType.name()), StringUtils.isBlank(metadataStr));
            ObjectNode inMetadata = JacksonUtil.fromString(metadataStr, ObjectNode.class);

            Assert.assertNotNull(inMetadata);
            Assert.assertEquals(String.format("Unexpected payload %s for integration %s, expected is: %s", resultNode.get("inContent").asText(), integrationType.name(), expectedPayloads.get(integrationType)), expectedPayloads.get(integrationType), resultNode.get("inContent").asText());
            Assert.assertEquals(integrationName, inMetadata.get("integrationName").asText());
            Assert.assertEquals("JSON", resultNode.get("inContentType").asText());

        }
    }

    @Test
    public void givenUplinkPayloadWithUtf8Chars_testConverting() {
        String textPayload = "Привіт,Genève Hôpital Etterbeek-Ixelles,我们一起去玩吧。,اللغة العربية";
        String base64Payload = Base64.getEncoder().encodeToString(textPayload.getBytes(StandardCharsets.UTF_8));

        String actualPayload = convertForPayload(base64Payload).getFirst();
        assertThat(actualPayload).isEqualTo(textPayload);
    }

    @Test
    public void givenUplinkWithBinaryPayload_testConverting() {
        Map<String, int[]> expectedResults = Map.of(
                "ALxhTl8OTGM=", new int[]{0, 188, 97, 78, 95, 14, 76, 99},
                "f5bI+g==", new int[]{127, 150, 200, 250}
        );
        expectedResults.forEach((base64Payload, bytes) -> {
            int[] actualCharCodes = convertForPayload(base64Payload).getSecond();
            assertThat(actualCharCodes).containsExactly(bytes);
        });
    }

    @Test
    public void testSaveConverter_checkLimitWithoutCountingEdgeTemplateConverters() throws Exception {
        loginSysAdmin();
        long limit = 5;
        EntityInfo defaultTenantProfileInfo = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        TenantProfile defaultTenantProfile = doGet("/api/tenantProfile/" + defaultTenantProfileInfo.getId().getId().toString(), TenantProfile.class);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxConverters(limit).build());
        doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);

        loginTenantAdmin();

        // creation of edge template converters will not impact creation of core converters
        for (int i = 0; i < limit; i++) {
            Converter edgeConverter = createConverter("My edge converter before" + i, true);
            doPost("/api/converter", edgeConverter, Converter.class);
        }

        for (int i = 0; i < limit; i++) {
            Converter converter = createConverter("My converter" + i, false);
            doPost("/api/converter", converter, Converter.class);
        }

        // creation of edge template converters allowed in case core converters limit reached
        for (int i = 0; i < limit; i++) {
            Converter edgeConverter = createConverter("My edge converter after" + i, true);
            doPost("/api/converter", edgeConverter, Converter.class);
        }

        try {
            Converter converter = createConverter("Converter Out Of Limit", false);
            doPost("/api/converter", converter).andExpect(status().is4xxClientError());
        } finally {
            defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxConverters(0).build());
            loginSysAdmin();
            doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);
        }
    }

    @Test
    public void testTTIDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"eui-1000000000000001\",\"profile\":\"application-tti-name\"," +
                "\"telemetry\":{\"ts\":1684398325906,\"values\":{\"battery\":95,\"temperature\":36.6,\"saturation\":99}},\"attributes\":{\"sn\":32310067,\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "eui-$eui", "$applicationId", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TTI, 2);

        testDecoder(DEFAULT_TTI_UPLINK_DECODER, DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testTTINotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"eui-1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684398325906,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "eui-$eui", "$applicationId", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TTI, 2);

        testDecoder(TTI_UPLINK_DECODER, DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testTTNDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"eui-1000000000000001\",\"profile\":\"application-tts-name\"," +
                "\"telemetry\":{\"ts\":1684474415641,\"values\":{\"battery\":95,\"temperature\":36.6,\"saturation\":99}},\"attributes\":{\"sn\":32310067,\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "eui-$eui", "$applicationId", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TTN, 2);

        testDecoder(DEFAULT_TTN_UPLINK_DECODER, DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testTTNNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"eui-1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684474415641,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "eui-$eui", "$applicationId", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TTN, 2);

        testDecoder(TTN_UPLINK_DECODER, DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testTTNDecoderWithJsonPayloadContent() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"eui-70B3D57ED006F370\",\"profile\":\"vadym-application\"," +
                "\"telemetry\":{\"ts\":1744280858243,\"values\":{\"battery\":95,\"temperature\":36.6,\"saturation\":99}},\"attributes\":{\"sn\":32310067,\"fPort\":1,\"eui\":\"70B3D57ED006F370\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "eui-$eui", "$applicationId", Set.of("eui", "fPort"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TTN, 2);

        InputStream resourceAsStream = ObjectNode.class.getClassLoader().getResourceAsStream(TTN_DECODED_PAYLOAD);
        String payload = new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);

        testDecoder(TTN_UPLINK_JSON_DECODER, payload, expectedDecodedMessage, converter);
    }

    @Test
    public void testThingParkDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device 1000000000000001\",\"profile\":\"default\"," +
                "\"telemetry\":{\"ts\":1732828102138,\"values\":{\"battery\":95,\"temperature\":36.6,\"saturation\":99}},\"attributes\":{\"sn\":32310067,\"mType\":2,\"eui\":\"1000000000000001\",\"fCntDn\":2}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device $eui", "", Set.of("eui", "mType", "fCntDn"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.THINGPARK, 2);

        testDecoder(DEFAULT_THINGPARK_UPLINK_DECODER, DEFAULT_THINGSPARK_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testThingParkNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1732828102138,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"mType\":2,\"eui\":\"1000000000000001\",\"fCntDn\":2}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device $eui", "", Set.of("eui", "mType", "fCntDn"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.THINGPARK, 2);

        testDecoder(THINGPARK_UPLINK_DECODER, DEFAULT_THINGSPARK_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testTPEDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device 1000000000000001\",\"profile\":\"default\"," +
                "\"telemetry\":{\"ts\":1732828102138,\"values\":{\"battery\":95,\"temperature\":36.6,\"saturation\":99}},\"attributes\":{\"sn\":32310067,\"mType\":2,\"eui\":\"1000000000000001\",\"fCntDn\":2}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device $eui", "", Set.of("eui", "mType", "fCntDn"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TPE, 2);

        testDecoder(DEFAULT_TPE_UPLINK_DECODER, DEFAULT_TPE_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testTPENotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1732828102138,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"mType\":2,\"eui\":\"1000000000000001\",\"fCntDn\":2}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device $eui", "", Set.of("eui", "mType", "fCntDn"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TPE, 2);

        testDecoder(TPE_UPLINK_DECODER, DEFAULT_TPE_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testChirpStackDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"ASSET\",\"name\":\"Asset 1000000000000001\",\"profile\":\"Chirpstack default device profile\"," +
                "\"telemetry\":{\"ts\":1684741625404,\"values\":{\"battery\":95,\"temperature\":36.6,\"saturation\":99}},\"attributes\":{\"sn\":32310067,\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.ASSET, "Asset $eui", "$deviceProfileName", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.CHIRPSTACK, 2);

        testDecoder(DEFAULT_CHIRPSTACK_UPLINK_DECODER, DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testChirpStackNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"ASSET\",\"name\":\"Asset 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684741625404,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.ASSET, "Asset $eui", "$deviceProfileName", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.CHIRPSTACK, 2);

        testDecoder(CHIRPSTACK_UPLINK_DECODER, DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testLoriotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device name 1000000000000001\"," +
                "\"profile\":\"Device type\",\"telemetry\":{\"ts\":1684478801936,\"values\":{\"battery\":95,\"temperature\":36.6,\"saturation\":99}},\"attributes\":{\"sn\":32310067,\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device name $eui", "Device type", Set.of("eui", "fPort"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.LORIOT, 2);

        testDecoder(DEFAULT_LORIOT_UPLINK_DECODER, DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testLoriotNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device name 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684478801936,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device name $eui", "Device type", Set.of("eui", "fPort"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.LORIOT, 2);

        testDecoder(LORIOT_UPLINK_DECODER, DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testLoriotDecoderThatReturnArray() throws IOException {
        String expectedDecodedMessage = "[{\"entityType\":\"DEVICE\",\"name\":\"Device name 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684478801936,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"fPort\":85,\"eui\":\"1000000000000001\"}}]";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device name $eui", "Device type", Set.of("eui", "fPort"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.LORIOT, 2);

        testDecoder(LORIOT_UPLINK_DECODER_RETURN_ARRAY, DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage, converter);
    }

    @Test
    public void testAwsIotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"Production 1 - 3G7H1j-9zF\",\"deviceType\":\"default\"," +
                "\"groupName\":\"Production\",\"attributes\":{\"deviceId\":\"3G7H1j-9zF\"},\"telemetry\":{\"ts\":1686398400000,\"values\":" +
                "{\"temperature\":25.3,\"humidity\":62.8,\"pressure\":1012.5,\"latitude\":37.7749,\"longitude\":-122.4194," +
                "\"status\":\"active\",\"power_status\":\"on\",\"x\":0.02,\"y\":0.03,\"z\":0.01,\"fault_codes.0\":100,\"fault_codes.1\":204," +
                "\"fault_codes.2\":301,\"battery_level\":78.5}}}";
        testDecoder(DEFAULT_AWS_IOT_UPLINK_DECODER, DEFAULT_AWS_IOT_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testAzureDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"8F4A2C6D\",\"deviceType\":\"Packing machine\",\"groupName\":\"Control room\"," +
                "\"attributes\":{\"version\":1,\"manufacturer\":\"Example corporation\"},\"telemetry\":{\"ts\":1686306600000," +
                "\"values\":{\"receivedAlarms\":[{\"type\":\"temperature\",\"severity\":\"high\",\"message\":\"Temperature exceeds threshold.\"}," +
                "{\"type\":\"vibration\",\"severity\":\"critical\",\"message\":\"Excessive vibration detected.\"}],\"temperature\":25.5," +
                "\"pressure\":1013.25,\"x\":0.02,\"y\":0.03,\"z\":0.015,\"status\":\"ALARM\",\"batteryLevel\":100,\"batteryStatus\":\"Charging\"}}}";
        testDecoder(DEFAULT_AZURE_UPLINK_DECODER, DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testSigfoxDefaultConverters() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"Sigfox-2203961\",\"deviceType\":\"Sigfox device\",\"groupName\":\"Control room devices\"," +
                "\"attributes\":{\"sigfoxId\":\"2203961\",\"deviceTypeId\":\"630ceaea10d051194ec0246e\",\"autoCalibration\":\"on\"," +
                "\"zeroPointAdjusted\":false,\"transmitPower\":\"full\",\"powerControl\":\"off\",\"fwVersion\":2},\"telemetry\":{\"ts\":\"1686298419000\"," +
                "\"values\":{\"temperature\":28.7,\"humidity\":33,\"co2\":582,\"co2Baseline\":420,\"customData1\":\"37\",\"customData2\":\"2\"}}}";
        testDecoder(DEFAULT_SIGFOX_UPLINK_DECODER, DEFAULT_SIGFOX_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    @Test
    public void testKpnDefaultConverter() throws IOException {
        String expectedDecodedMessage = "{\"deviceName\":\"Device A\",\"deviceType\":\"thermostat\",\"customerName\":\"customer\",\"groupName\":\"thermostat devices\",\"attributes\":{\"model\":\"Model A\",\"serialNumber\":\"SN111\"},\"telemetry\":{\"temperature\":42,\"humidity\":80}}";
        testDecoder(DEFAULT_KPN_UPLINK_DECODER, DEFAULT_KNP_UPLINK_CONVERTER_MESSAGE, expectedDecodedMessage);
    }

    public void testDecoder(String decoderFileName, String payloadExample, String expectedResult) throws IOException {
        testDecoder(decoderFileName, payloadExample, expectedResult, null);
    }

    public void testDecoder(String decoderFileName, String payloadExample, String expectedResult, Converter converter) throws IOException {
        byte[] bytes = IOUtils.toByteArray(ConverterControllerTest.class.getClassLoader().getResourceAsStream(decoderFileName));

        String base64Payload = Base64.getEncoder().encodeToString(payloadExample.getBytes(StandardCharsets.UTF_8));

        ObjectNode inputParams = JacksonUtil.newObjectNode();
        inputParams.set("decoder", new TextNode(new String(bytes)));
        inputParams.set("metadata", JacksonUtil.newObjectNode());

        if (converter != null && converter.isDedicated()) {
            inputParams.set("payload", JacksonUtil.toJsonNode(payloadExample));
            inputParams.set("converter", JacksonUtil.valueToTree(converter));
            JsonNode unwrapped = doPost("/api/converter/unwrap/" + converter.getIntegrationType(), inputParams, JsonNode.class);
            String contentType = unwrapped.get("contentType").asText();
            if (ContentType.JSON.name().equals(contentType)) {
                String payloadString = unwrapped.get("payload").asText();
                String base64Encoded = Base64.getEncoder().encodeToString(payloadString.getBytes(StandardCharsets.UTF_8));
                inputParams.put("payload", base64Encoded);
            } else {
                inputParams.set("payload", unwrapped.get("payload"));
            }
            inputParams.set("metadata",unwrapped.get("metadata"));
        } else {
            inputParams.set("payload", new TextNode(base64Payload));
        }

        JsonNode response = doPost("/api/converter/testUpLink?scriptLang=TBEL", inputParams, JsonNode.class);
        String output;
        if (converter != null && converter.getConverterVersion() == 2) {
            output = response.get("outputMsg").toString();
        } else {
            output = response.get("output").asText();
        }
        assertThat(output).isEqualTo(expectedResult);
    }

    public DedicatedConverterConfig createDedicatedConverterConfig(EntityType type, String name, String profile, Set<String> attributes) {
        DedicatedConverterConfig config = new DedicatedConverterConfig();
        config.setType(type);
        config.setName(name);
        config.setProfile(profile);
        config.setAttributes(attributes);
        config.setGroup("");
        config.setCustomer("");
        config.setLabel("");
        return config;
    }

    private Converter createConverter(String name, boolean edgeTemplate) {
        return createConverter(name, edgeTemplate, ConverterType.UPLINK, CUSTOM_UPLINK_CONVERTER_CONFIGURATION, null, null);
    }

    private Converter createConverter(JsonNode config, IntegrationType integrationType, Integer version) {
        return createConverter(null, false, ConverterType.UPLINK, config, integrationType, version);
    }

    private Converter createConverter(String name, boolean edgeTemplate, ConverterType converterType, JsonNode config, IntegrationType integrationType, Integer version) {
        Converter converter = new Converter();
        converter.setName(name);
        converter.setType(converterType);
        converter.setEdgeTemplate(edgeTemplate);
        converter.setIntegrationType(integrationType);
        converter.setConverterVersion(version);
        converter.setConfiguration(config);
        return converter;
    }

    private Pair<String, int[]> convertForPayload(String base64Payload) {
        String decoderConfiguration = "" +
                "var payloadStr = decodeToString(payload);\n" +
                "var result = {\n" +
                "   telemetry: {\n" +
                "       payload: payload," +
                "       payloadStr: payloadStr\n" +
                "   }\n" +
                "};" +
                "function decodeToString(payload) {\n" +
                "   return String.fromCharCode.apply(String, payload);\n" +
                "}\n" +
                "return result;";

        ObjectNode inputParams = JacksonUtil.newObjectNode();
        inputParams.set("decoder", new TextNode(decoderConfiguration));
        inputParams.set("payload", new TextNode(base64Payload));
        inputParams.set("metadata", JacksonUtil.newObjectNode());

        JsonNode output = doPost("/api/converter/testUpLink", inputParams, JsonNode.class);
        JsonNode telemetry = JacksonUtil.toJsonNode(output.get("output").asText()).get("telemetry");
        String payloadStr = telemetry.get("payloadStr").asText();
        int[] payloadCharCodes = Streams.stream(telemetry.get("payload").iterator())
                .mapToInt(JsonNode::asInt)
                .toArray();
        return Pair.of(payloadStr, payloadCharCodes);
    }

}
