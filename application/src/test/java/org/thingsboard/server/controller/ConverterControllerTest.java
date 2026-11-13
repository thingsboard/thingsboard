// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
import org.thingsboard.server.common.data.script.ScriptLanguage;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private static final String CHIRPSTACK_UPLINK_DECODER = "converter/chirpstack_uplink_decoder.raw";
    private static final String LORIOT_UPLINK_DECODER = "converter/loriot_uplink_decoder.raw";
    private static final String THINGPARK_UPLINK_DECODER = "converter/thingpark_uplink_decoder.raw";
    private static final String TPE_UPLINK_DECODER = "converter/tpe_uplink_decoder.raw";
    private static final String TTI_UPLINK_DECODER = "converter/tti_uplink_decoder.raw";
    private static final String TTN_UPLINK_DECODER = "converter/ttn_uplink_decoder.raw";
    private static final String TTN_UPLINK_JSON_DECODER = "converter/ttn_uplink_json_decoder.raw";
    private static final String LORIOT_UPLINK_DECODER_RETURN_ARRAY = "converter/loriot_uplink_decoder_array.raw";
    private static final String TTN_DECODED_PAYLOAD = "payload/ttn_payload_with_decoded.json";
    private static final String TTN_CUSTOM_DECODER_PAYLOAD = "payload/ttn_custom_decoder_payload.json";
    private static final String TTI_CUSTOM_DECODER_PAYLOAD = "payload/tti_custom_decoder_payload.json";
    private static final String DEFAULT_V2_CONFIGURATION = "ui-ngx/src/assets/converters/default-converters-v2-configuration.json";
    private static final String THINGS_STACK_REPOSITORY_DEVICE_PAYLOAD = "payload/things_stack_repository_device_payload.json";
    private static final String THINGS_STACK_MANUAL_DEVICE_PAYLOAD = "payload/things_stack_manual_device_payload.json";
    private static final String THINGS_STACK_FORMATTER_DEVICE_PAYLOAD = "payload/things_stack_formatter_device_payload.json";

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
                IntegrationType.LORIOT, getDefaultPayload(IntegrationType.LORIOT),
                IntegrationType.CHIRPSTACK, getDefaultPayload(IntegrationType.CHIRPSTACK),
                IntegrationType.TTN, getDefaultPayload(IntegrationType.TTN),
                IntegrationType.TTI, getDefaultPayload(IntegrationType.TTI));

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
    public void testDefaultDebugIn() throws Exception {
        String urlTemplate = "/api/converter/{converterId}/debugIn?integrationType={integrationType}&converterType={converterType}";

        SoftAssertions softly = new SoftAssertions();
        for (IntegrationType integrationType : IntegrationType.values()) {
            String dir = integrationType.getDirectory();
            if (dir == null) {
                continue;
            }
            String integrationName = readResource("converters/" + dir + "/integration.json")
                    .map(content -> JacksonUtil.toJsonNode(content).get("name").asText())
                    .orElse("Test " + dir);

            ObjectNode uplink = doGet(urlTemplate, ObjectNode.class, EntityId.NULL_UUID, integrationType.name(), ConverterType.UPLINK.name());
            ObjectNode uplinkMetadata = JacksonUtil.fromString(uplink.get("inMetadata").asText(), ObjectNode.class);
            softly.assertThat(uplinkMetadata.get("integrationName").asText()).as(dir + " uplink integrationName").isEqualTo(integrationName);
            readResource("converters/" + dir + "/uplink/metadata.json")
                    .map(content -> JacksonUtil.fromString(content, ObjectNode.class))
                    .ifPresent(expected -> expected.properties().forEach(entry ->
                            softly.assertThat(uplinkMetadata.get(entry.getKey())).as(dir + " uplink metadata " + entry.getKey()).isEqualTo(entry.getValue())));
            Optional<String> payload = readResource("converters/" + dir + "/uplink/payload.json");
            if (payload.isPresent()) {
                softly.assertThat(uplink.get("inContent").asText()).as(dir + " uplink payload").isEqualTo(payload.get());
            } else {
                softly.assertThat(uplink.get("inContent").isNull()).as(dir + " uplink payload").isTrue();
            }

            String downlink = doGet(urlTemplate, EntityId.NULL_UUID, integrationType.name(), ConverterType.DOWNLINK.name())
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            Optional<String> downlinkMetadata = readResource("converters/" + dir + "/downlink/metadata.json");
            Optional<String> downlinkPayload = readResource("converters/" + dir + "/downlink/payload.json");
            Optional<String> downlinkMsgType = readResource("converters/" + dir + "/downlink/msgType.txt").map(String::trim);
            if (downlinkMetadata.isPresent() || downlinkPayload.isPresent() || downlinkMsgType.isPresent()) {
                JsonNode downlinkNode = JacksonUtil.toJsonNode(downlink);
                softly.assertThat(Optional.ofNullable(downlinkNode.get("inMetadata")).map(JsonNode::asText).map(JacksonUtil::toJsonNode))
                        .as(dir + " downlink metadata").isEqualTo(downlinkMetadata.map(JacksonUtil::toJsonNode));
                softly.assertThat(Optional.ofNullable(downlinkNode.get("inContent")).map(JsonNode::asText))
                        .as(dir + " downlink payload").isEqualTo(downlinkPayload);
                softly.assertThat(Optional.ofNullable(downlinkNode.get("inMsgType")).map(JsonNode::asText))
                        .as(dir + " downlink msgType").isEqualTo(downlinkMsgType);
                softly.assertThat(JacksonUtil.toJsonNode(downlinkNode.get("inIntegrationMetadata").asText()).get("integrationName").asText())
                        .as(dir + " downlink integrationName").isEqualTo(integrationName);
            } else {
                softly.assertThat(downlink).as(dir + " downlink").isEmpty();
            }
        }
        softly.assertAll();
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
    public void testAwsIotDefaultDecoderTopic() throws Exception {
        String uplinkDir = "converters/AWS_IOT/uplink/";
        String payload = readResource(uplinkDir + "payload.json").orElseThrow();
        JsonNode expected = JacksonUtil.toJsonNode(readResource(uplinkDir + "test/default/expected.json").orElseThrow());

        // topic -> expected error
        Map<String, String> errorCases = new LinkedHashMap<>();
        errorCases.put(null, "MQTT topic is missing or empty");
        errorCases.put("", "MQTT topic is missing or empty");
        errorCases.put("  ", "MQTT topic is missing or empty");
        errorCases.put("thermostat", "MQTT topic must contain device type and device name: thermostat");
        errorCases.put("tb/aws/iot/telemetry/thermostat/", "MQTT topic must contain device type and device name: tb/aws/iot/telemetry/thermostat/");
        errorCases.put("tb/aws/iot/telemetry//Thermostat-01", "MQTT topic must contain device type and device name: tb/aws/iot/telemetry//Thermostat-01");

        SoftAssertions softly = new SoftAssertions();
        for (ScriptLanguage scriptLang : ScriptLanguage.values()) {
            String decoder = readResource(uplinkDir + scriptLang.name().toLowerCase() + ".raw").orElseThrow();

            // Spaces around the device type and name segments are not part of the device type and name.
            JsonNode response = decodeAwsIotUplink(decoder, scriptLang, payload, "tb/aws/iot/telemetry/ thermostat / Thermostat-01 ");
            softly.assertThat(response.get("error").asText()).as(scriptLang + ": topic with spaces").isEmpty();
            softly.assertThat(JacksonUtil.toJsonNode(response.get("output").asText())).as(scriptLang + ": topic with spaces").isEqualTo(expected);

            for (Map.Entry<String, String> errorCase : errorCases.entrySet()) {
                String description = scriptLang + ": topic " + errorCase.getKey();
                response = decodeAwsIotUplink(decoder, scriptLang, payload, errorCase.getKey());
                softly.assertThat(response.get("output").asText()).as(description).isEmpty();
                softly.assertThat(response.get("error").asText()).as(description).contains(errorCase.getValue());
            }
        }
        softly.assertAll();
    }

    private JsonNode decodeAwsIotUplink(String decoder, ScriptLanguage scriptLang, String payload, String topic) {
        ObjectNode metadata = JacksonUtil.newObjectNode();
        if (topic != null) {
            metadata.put("topic", topic);
        }
        ObjectNode inputParams = JacksonUtil.newObjectNode();
        inputParams.put("decoder", decoder);
        inputParams.set("metadata", metadata);
        inputParams.put("payload", Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));
        return doPost("/api/converter/testUpLink?scriptLang=" + scriptLang, inputParams, JsonNode.class);
    }

    @Test
    public void testDefaultUplinkConverters() throws Exception {
        Resource[] expectedFiles = new PathMatchingResourcePatternResolver().getResources("classpath*:converters/*/uplink/test/*/expected.json");
        assertThat(expectedFiles).isNotEmpty();

        SoftAssertions softly = new SoftAssertions();
        for (Resource expectedFile : expectedFiles) {
            String url = expectedFile.getURL().getPath();
            String casePath = url.substring(url.lastIndexOf("/converters/") + 1);
            String[] parts = casePath.split("/");
            IntegrationType integrationType = IntegrationType.valueOf(parts[1]);
            String uplinkDir = "converters/" + parts[1] + "/uplink/";
            String caseDir = uplinkDir + "test/" + parts[4] + "/";

            JsonNode expected = JacksonUtil.toJsonNode(readResource(casePath).orElseThrow());
            String payload = readResource(caseDir + "payload.json").orElse(getDefaultPayload(integrationType));
            Converter converter = readResource(caseDir + "configuration.json")
                    .map(configuration -> createConverter(JacksonUtil.toJsonNode(configuration), integrationType, 2))
                    .orElse(null);
            String metadata = readResource(uplinkDir + "metadata.json").orElse("{}");

            int decoders = 0;
            for (ScriptLanguage scriptLang : ScriptLanguage.values()) {
                Optional<String> decoder = readResource(uplinkDir + scriptLang.name().toLowerCase() + ".raw");
                if (decoder.isPresent()) {
                    decoders++;
                    String output = decodeUplink(decoder.get(), scriptLang, payload, metadata, converter);
                    softly.assertThat(JacksonUtil.toJsonNode(output)).as(casePath + " [" + scriptLang + "]").isEqualTo(expected);
                }
            }
            softly.assertThat(decoders).as(casePath + ": no decoder found").isPositive();
        }
        softly.assertAll();
    }

    @Test
    public void testDefaultDownlinkConverters() throws Exception {
        Resource[] expectedFiles = new PathMatchingResourcePatternResolver().getResources("classpath*:converters/*/downlink/test/*/expected.json");
        assertThat(expectedFiles).isNotEmpty();

        SoftAssertions softly = new SoftAssertions();
        for (Resource expectedFile : expectedFiles) {
            String url = expectedFile.getURL().getPath();
            String casePath = url.substring(url.lastIndexOf("/converters/") + 1);
            String[] parts = casePath.split("/");
            String downlinkDir = "converters/" + parts[1] + "/downlink/";
            String caseDir = downlinkDir + "test/" + parts[4] + "/";

            JsonNode expected = JacksonUtil.toJsonNode(readResource(casePath).orElseThrow());
            Optional<String> casePayload = readResource(caseDir + "payload.json");
            String msg = casePayload.isPresent() ? casePayload.get() : readResource(downlinkDir + "payload.json").orElseThrow();
            String msgType = readResource(downlinkDir + "msgType.txt").map(String::trim).orElse("ATTRIBUTES_UPDATED");
            String metadata = readResource(downlinkDir + "metadata.json").orElse("{}");

            int encoders = 0;
            for (ScriptLanguage scriptLang : ScriptLanguage.values()) {
                Optional<String> encoder = readResource(downlinkDir + scriptLang.name().toLowerCase() + ".raw");
                if (encoder.isPresent()) {
                    encoders++;
                    JsonNode response = encodeDownlink(encoder.get(), scriptLang, msg, msgType, metadata);
                    softly.assertThat(response.get("error").asText()).as(casePath + " [" + scriptLang + "] error").isEmpty();
                    softly.assertThat(JacksonUtil.toJsonNode(response.get("output").asText())).as(casePath + " [" + scriptLang + "]").isEqualTo(expected);
                }
            }
            softly.assertThat(encoders).as(casePath + ": no encoder found").isPositive();
        }
        softly.assertAll();
    }

    @Test
    public void testTTINotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"eui-1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684398325906,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "eui-$eui", "$applicationId", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TTI, 2);

        testDecoder(TTI_UPLINK_DECODER, readResource(TTI_CUSTOM_DECODER_PAYLOAD).orElseThrow(), expectedDecodedMessage, converter);
    }

    private static final String THINGS_STACK_REPOSITORY_DEVICE_RESULT = "{\"entityType\":\"DEVICE\",\"name\":\"am308-office\",\"profile\":\"milesight-iot-am308\",\"label\":\"am308-office\",\"group\":\"my-app\"," +
                "\"telemetry\":[{\"ts\":1793170000000,\"values\":{\"temperature\":24.1,\"co2\":550}},{\"ts\":1793181600100,\"values\":{\"battery\":95,\"temperature\":24.5,\"co2\":560," +
                "\"fCnt\":12,\"data\":\"AXVk\",\"rssi\":-60,\"snr\":9.5,\"channel\":2,\"consumedAirtime\":\"0.061696s\"}}]," +
                "\"attributes\":{\"eui\":\"24E124707C481005\",\"joinEui\":\"24E124C0002A0001\",\"devAddr\":\"260B1234\",\"deviceId\":\"am308-office\",\"applicationId\":\"my-app\"," +
                "\"fPort\":85,\"bandwidth\":125000,\"spreadingFactor\":7,\"codeRate\":\"4/5\",\"frequency\":868100000,\"brandId\":\"milesight-iot\",\"modelId\":\"am308\"," +
                "\"hardwareVersion\":\"1.0\",\"firmwareVersion\":\"1.0\",\"bandId\":\"EU_863_870\",\"netId\":\"000013\",\"tenantId\":\"ttn\",\"clusterId\":\"eu1\"," +
                "\"clusterAddress\":\"eu1.cloud.thethings.network\"}}";

    @Test
    public void testThingsStackDefaultConvertersGiveRepositoryDeviceItsModelProfile() throws IOException {
        testThingsStackDefaultConverters(THINGS_STACK_REPOSITORY_DEVICE_PAYLOAD, Map.of(), THINGS_STACK_REPOSITORY_DEVICE_RESULT);
    }

    @Test
    public void testThingsStackDefaultConvertersTakeProfileNameFromIntegrationMetadata() throws IOException {
        String expectedDecodedMessage = THINGS_STACK_REPOSITORY_DEVICE_RESULT.replace("\"profile\":\"milesight-iot-am308\"", "\"profile\":\"AM308\"");
        Map<String, String> integrationMetadata = Map.of("profile.milesight-iot-am308", "AM308", "profile.milesight-iot-ws202", "WS202");

        testThingsStackDefaultConverters(THINGS_STACK_REPOSITORY_DEVICE_PAYLOAD, integrationMetadata, expectedDecodedMessage);
    }

    @Test
    public void testThingsStackDefaultConvertersGiveManualDeviceRawData() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"manual-node\",\"profile\":\"LoRaWAN Device\",\"label\":\"manual-node\",\"group\":\"my-app\"," +
                "\"telemetry\":{\"ts\":1793181600100,\"values\":{\"fCnt\":3,\"data\":\"Ae0DM18OTGM=\",\"rssi\":-70,\"snr\":7.0}}," +
                "\"attributes\":{\"eui\":\"70B3D57ED006F370\",\"deviceId\":\"manual-node\",\"applicationId\":\"my-app\",\"fPort\":1,\"bandwidth\":125000," +
                "\"spreadingFactor\":9,\"frequency\":868300000}}";

        testThingsStackDefaultConverters(THINGS_STACK_MANUAL_DEVICE_PAYLOAD, Map.of("profile.milesight-iot-am308", "AM308"), expectedDecodedMessage);
    }

    // temperatureUnit is not ASCII: decoded_payload reaches the decoders as UTF-8, and both must give the same text
    private static final String THINGS_STACK_FORMATTER_DEVICE_RESULT = "{\"entityType\":\"DEVICE\",\"name\":\"macro-tracker-1\",\"profile\":\"lansitec-macro-tracker\",\"label\":\"macro-tracker-1\",\"group\":\"my-app\"," +
                "\"telemetry\":{\"ts\":1794304800100,\"values\":{\"batteryLevel\":100,\"chargeState\":\"Charging\",\"temperatureUnit\":\"°C\",\"fCnt\":5,\"data\":\"IWQ+Az4AbFA=\",\"rssi\":-70,\"snr\":7.0}}," +
                "\"attributes\":{\"brandId\":\"lansitec\",\"modelId\":\"macro-tracker\",\"eui\":\"70B3D57ED006F371\",\"deviceId\":\"macro-tracker-1\",\"applicationId\":\"my-app\",\"fPort\":1,\"bandwidth\":125000," +
                "\"spreadingFactor\":9,\"frequency\":868300000}}";

    // A device added without the Repository, whose payload formatter names its model in tbVersionIds: the profile of the
    // model, and the brandId and modelId attributes, as a Repository device gets them from version_ids
    @Test
    public void testThingsStackDefaultConvertersGiveFormatterDeviceTheModelProfileOfItsDecodedPayload() throws IOException {
        testThingsStackDefaultConverters(THINGS_STACK_FORMATTER_DEVICE_PAYLOAD, Map.of(), THINGS_STACK_FORMATTER_DEVICE_RESULT);
    }

    @Test
    public void testThingsStackDefaultConvertersTakeProfileNameOfFormatterDeviceFromIntegrationMetadata() throws IOException {
        String expectedDecodedMessage = THINGS_STACK_FORMATTER_DEVICE_RESULT.replace("\"profile\":\"lansitec-macro-tracker\"", "\"profile\":\"Macro tracker\"");

        testThingsStackDefaultConverters(THINGS_STACK_FORMATTER_DEVICE_PAYLOAD, Map.of("profile.lansitec-macro-tracker", "Macro tracker"), expectedDecodedMessage);
    }

    // version_ids come first: tbVersionIds of a Repository device does not change its profile, and is not telemetry either
    @Test
    public void testThingsStackDefaultConvertersPreferRepositoryModelToTbVersionIds() throws IOException {
        ObjectNode payload = (ObjectNode) JacksonUtil.toJsonNode(readResource(THINGS_STACK_REPOSITORY_DEVICE_PAYLOAD).orElseThrow());
        ((ObjectNode) payload.get("uplink_message").get("decoded_payload"))
                .set("tbVersionIds", JacksonUtil.toJsonNode("{\"brand_id\":\"lansitec\",\"model_id\":\"macro-tracker\"}"));

        testThingsStackDefaultConvertersOnPayload(payload.toString(), Map.of(), THINGS_STACK_REPOSITORY_DEVICE_RESULT);
    }

    // tbVersionIds that is not an object (a text, null, a list) names no model, and is not telemetry either
    @Test
    public void testThingsStackDefaultConvertersIgnoreTbVersionIdsThatIsNotAnObject() throws IOException {
        String expectedDecodedMessage = THINGS_STACK_FORMATTER_DEVICE_RESULT.replace("\"profile\":\"lansitec-macro-tracker\"", "\"profile\":\"LoRaWAN Device\"")
                .replace("\"brandId\":\"lansitec\",\"modelId\":\"macro-tracker\",", "");
        for (String tbVersionIds : List.of("\"lansitec-macro-tracker\"", "null", "[\"lansitec\",\"macro-tracker\"]")) {
            ObjectNode payload = (ObjectNode) JacksonUtil.toJsonNode(readResource(THINGS_STACK_FORMATTER_DEVICE_PAYLOAD).orElseThrow());
            ((ObjectNode) payload.get("uplink_message").get("decoded_payload")).set("tbVersionIds", JacksonUtil.toJsonNode(tbVersionIds));

            testThingsStackDefaultConvertersOnPayload(payload.toString(), Map.of(), expectedDecodedMessage);
        }
    }

    // The keys the integration metadata lists for the model are attributes, not telemetry; spaces around them are ignored,
    // and a listed key the uplink doesn't have is skipped. The model may come from tbVersionIds
    @Test
    public void testThingsStackDefaultConvertersSaveTheModelsListedKeysAsAttributes() throws IOException {
        String expectedDecodedMessage = THINGS_STACK_FORMATTER_DEVICE_RESULT
                .replace("\"chargeState\":\"Charging\",", "")
                .replace("\"attributes\":{", "\"attributes\":{\"chargeState\":\"Charging\",");

        testThingsStackDefaultConverters(THINGS_STACK_FORMATTER_DEVICE_PAYLOAD,
                Map.of("attributes.lansitec-macro-tracker", " chargeState , firmwareVersionOfTheTracker"), expectedDecodedMessage);
    }

    // The list is per brand and model, not per profile name: a renamed profile keeps it. History records stay telemetry:
    // temperature is in the history record (24.1) and in the latest values (24.5), and only the latest one is an attribute
    @Test
    public void testThingsStackDefaultConvertersTakeTheListOfARepositoryDeviceByItsModel() throws IOException {
        String latestTemperature = "\"temperature\":24.5,";
        assertThat(THINGS_STACK_REPOSITORY_DEVICE_RESULT.split(Pattern.quote(latestTemperature), -1)).hasSize(2);
        assertThat(THINGS_STACK_REPOSITORY_DEVICE_RESULT).contains("{\"temperature\":24.1,\"co2\":550}");
        String expectedDecodedMessage = THINGS_STACK_REPOSITORY_DEVICE_RESULT
                .replace("\"profile\":\"milesight-iot-am308\"", "\"profile\":\"AM308\"")
                .replace(latestTemperature, "")
                .replace("\"attributes\":{", "\"attributes\":{\"temperature\":24.5,");

        testThingsStackDefaultConverters(THINGS_STACK_REPOSITORY_DEVICE_PAYLOAD,
                Map.of("profile.milesight-iot-am308", "AM308", "attributes.milesight-iot-am308", "temperature"), expectedDecodedMessage);
    }

    // The list of another model changes nothing
    @Test
    public void testThingsStackDefaultConvertersIgnoreTheListOfAnotherModel() throws IOException {
        testThingsStackDefaultConverters(THINGS_STACK_FORMATTER_DEVICE_PAYLOAD,
                Map.of("attributes.lansitec-solar-tracker", "chargeState"), THINGS_STACK_FORMATTER_DEVICE_RESULT);
    }

    // The TBEL and JS defaults are two copies of one script, so each of them must give this result for TTN and TTI
    private void testThingsStackDefaultConverters(String payloadFile, Map<String, String> integrationMetadata, String expectedResult) throws IOException {
        testThingsStackDefaultConvertersOnPayload(readResource(payloadFile).orElseThrow(), integrationMetadata, expectedResult);
    }

    private void testThingsStackDefaultConvertersOnPayload(String payload, Map<String, String> integrationMetadata, String expectedResult) throws IOException {
        JsonNode defaultConfigurations = JacksonUtil.toJsonNode(FrontEndSource.read(DEFAULT_V2_CONFIGURATION));
        for (IntegrationType integrationType : List.of(IntegrationType.TTN, IntegrationType.TTI)) {
            Converter converter = createConverter(defaultConfigurations.get(integrationType.name()), integrationType, 2);
            for (ScriptLanguage scriptLang : ScriptLanguage.values()) {
                String decoder = readResource("converters/" + integrationType.getDirectory() + "/uplink/" + scriptLang.name().toLowerCase() + ".raw").orElseThrow();
                assertThat(JacksonUtil.toJsonNode(decodeUplink(decoder, scriptLang, payload, "{}", converter, integrationMetadata)))
                        .as("%s %s", integrationType, scriptLang)
                        .isEqualTo(JacksonUtil.toJsonNode(expectedResult));
            }
        }
    }

    @Test
    public void testTTNNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"eui-1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684474415641,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "eui-$eui", "$applicationId", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TTN, 2);

        testDecoder(TTN_UPLINK_DECODER, readResource(TTN_CUSTOM_DECODER_PAYLOAD).orElseThrow(), expectedDecodedMessage, converter);
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
    public void testThingParkNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1732828102138,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"mType\":2,\"eui\":\"1000000000000001\",\"fCntDn\":2}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device $eui", "", Set.of("eui", "mType", "fCntDn"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.THINGPARK, 2);

        testDecoder(THINGPARK_UPLINK_DECODER, getDefaultPayload(IntegrationType.THINGPARK), expectedDecodedMessage, converter);
    }

    @Test
    public void testTPENotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1732828102138,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"mType\":2,\"eui\":\"1000000000000001\",\"fCntDn\":2}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device $eui", "", Set.of("eui", "mType", "fCntDn"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.TPE, 2);

        testDecoder(TPE_UPLINK_DECODER, getDefaultPayload(IntegrationType.TPE), expectedDecodedMessage, converter);
    }

    @Test
    public void testChirpStackNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"ASSET\",\"name\":\"Asset 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684741625404,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"devAddr\":\"20000001\",\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.ASSET, "Asset $eui", "$deviceProfileName", Set.of("eui", "fPort", "devAddr"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.CHIRPSTACK, 2);

        testDecoder(CHIRPSTACK_UPLINK_DECODER, getDefaultPayload(IntegrationType.CHIRPSTACK), expectedDecodedMessage, converter);
    }

    @Test
    public void testLoriotNotDefaultDecoder() throws IOException {
        String expectedDecodedMessage = "{\"entityType\":\"DEVICE\",\"name\":\"Device name 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684478801936,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"fPort\":85,\"eui\":\"1000000000000001\"}}";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device name $eui", "Device type", Set.of("eui", "fPort"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.LORIOT, 2);

        testDecoder(LORIOT_UPLINK_DECODER, getDefaultPayload(IntegrationType.LORIOT), expectedDecodedMessage, converter);
    }

    @Test
    public void testLoriotDecoderThatReturnArray() throws IOException {
        String expectedDecodedMessage = "[{\"entityType\":\"DEVICE\",\"name\":\"Device name 1000000000000001\",\"profile\":\"IndustrialSensorProfile\",\"customer\":\"MyCustomer\",\"group\":\"SensorsGroup\"," +
                "\"telemetry\":{\"ts\":1684478801936,\"values\":{\"battery\":95,\"temperature\":36.6}},\"attributes\":{\"fPort\":85,\"eui\":\"1000000000000001\"}}]";

        DedicatedConverterConfig config = createDedicatedConverterConfig(EntityType.DEVICE, "Device name $eui", "Device type", Set.of("eui", "fPort"));
        Converter converter = createConverter(JacksonUtil.valueToTree(config), IntegrationType.LORIOT, 2);

        testDecoder(LORIOT_UPLINK_DECODER_RETURN_ARRAY, getDefaultPayload(IntegrationType.LORIOT), expectedDecodedMessage, converter);
    }

    private String getDefaultPayload(IntegrationType integrationType) throws IOException {
        return readResource("converters/" + integrationType.getDirectory() + "/uplink/payload.json").orElseThrow();
    }

    private JsonNode encodeDownlink(String encoder, ScriptLanguage scriptLang, String msg, String msgType, String metadata) {
        ObjectNode inputParams = JacksonUtil.newObjectNode();
        inputParams.put("encoder", encoder);
        inputParams.put("msg", msg);
        inputParams.put("msgType", msgType);
        inputParams.set("metadata", JacksonUtil.toJsonNode(metadata));
        inputParams.set("integrationMetadata", JacksonUtil.newObjectNode());

        return doPost("/api/converter/testDownLink?scriptLang=" + scriptLang, inputParams, JsonNode.class);
    }

    private Optional<String> readResource(String path) throws IOException {
        try (InputStream in = ConverterControllerTest.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? Optional.empty() : Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public void testDecoder(String decoderFileName, String payloadExample, String expectedResult) throws IOException {
        testDecoder(decoderFileName, payloadExample, expectedResult, null);
    }

    public void testDecoder(String decoderFileName, String payloadExample, String expectedResult, Converter converter) throws IOException {
        assertThat(decodeUplink(readResource(decoderFileName).orElseThrow(), ScriptLanguage.TBEL, payloadExample, "{}", converter)).isEqualTo(expectedResult);
    }

    private String decodeUplink(String decoder, ScriptLanguage scriptLang, String payloadExample, String metadata, Converter converter) {
        return decodeUplink(decoder, scriptLang, payloadExample, metadata, converter, Map.of());
    }

    // integrationMetadata: what the integration adds to the metadata of every uplink, from its configuration
    private String decodeUplink(String decoder, ScriptLanguage scriptLang, String payloadExample, String metadata, Converter converter,
                                Map<String, String> integrationMetadata) {
        String base64Payload = Base64.getEncoder().encodeToString(payloadExample.getBytes(StandardCharsets.UTF_8));

        ObjectNode inputParams = JacksonUtil.newObjectNode();
        inputParams.set("decoder", new TextNode(decoder));
        inputParams.set("metadata", JacksonUtil.toJsonNode(metadata));

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
            integrationMetadata.forEach(((ObjectNode) inputParams.get("metadata"))::put);
        } else {
            inputParams.set("payload", new TextNode(base64Payload));
        }

        JsonNode response = doPost("/api/converter/testUpLink?scriptLang=" + scriptLang, inputParams, JsonNode.class);
        if (converter != null && converter.getConverterVersion() == 2) {
            return response.get("outputMsg").toString();
        }
        return response.get("output").asText();
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
