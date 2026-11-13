// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.mqtt.TbMqttNode;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.dto.TbChatRequest;
import org.thingsboard.server.common.data.ai.dto.TbChatResponse;
import org.thingsboard.server.common.data.ai.dto.TbContent;
import org.thingsboard.server.common.data.ai.dto.TbUserMessage;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;
import org.thingsboard.server.common.data.ai.provider.GoogleAiGeminiProviderConfig;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ai.AiRequestsExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
        "integrations.converters.library.enabled=true"
})
@Slf4j
@DaoSqlTest
public class SecretControllerTest extends AbstractControllerTest {

    private static final TypeReference<PageData<SecretInfo>> PAGE_DATA_SECRET_TYPE_REF = new TypeReference<>() {};

    @MockitoBean
    private AiRequestsExecutor aiRequestsExecutor;

    @MockitoSpyBean
    private Langchain4jChatModelConfigurer chatModelConfigurer;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Create Secret", "CreatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(new SecretInfo(savedSecret));

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecret() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Update Secret", "UpdatePassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // update secret value
        secret = new Secret(savedSecret);
        secret.setValue("UpdatedPassword");

        SecretInfo updatedSecret = doPost("/api/secret", secret, SecretInfo.class);
        retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, updatedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(updatedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSecret_whenUsedInRuleNodeWithHasSecrets_thenReceiveDeleteResultError() throws Exception {
        String secretName = "MqttNodeSecret";
        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create rule node with secret usage that uses 'hasSecrets' annotation:
        var ruleChain = createRuleChain("Rule Chain test mqtt", TbMqttNode.class.getName(), secretName);

        // delete secret
        String responseBody = doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        JsonNode node = JacksonUtil.toJsonNode(responseBody);
        assertFalse(node.get("success").asBoolean());

        JsonNode references = node.get("references");
        assertNotNull(references.get("RULE_CHAIN"));
        assertFalse(references.get("RULE_CHAIN").isEmpty());

        doDelete("/api/ruleChain/" + ruleChain.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSecret_whenUsedInRuleNodeWithoutHasSecrets_thenReceiveDeleteResultSuccess() throws Exception {
        String secretName = "GetAttrNode";
        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        // create rule node with secret usage that uses 'hasSecrets' annotation:
        var ruleChain = createRuleChain("Rule Chain test attr", TbGetAttributesNode.class.getName(), secretName);

        // delete secret
        doDelete("/api/ruleChain/" + ruleChain.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSecretUsedInIntegration_thenReceiveDeleteResultError() throws Exception {
        String secretName = "MqttSecret";
        String placeholder = toSecretPlaceholder(secretName, SecretType.TEXT);

        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        ConverterId converterId = createConverter();
        IntegrationId integrationId = createIntegration(converterId, placeholder);

        String responseBody = doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        JsonNode node = JacksonUtil.toJsonNode(responseBody);
        assertFalse(node.get("success").asBoolean());

        JsonNode references = node.get("references");
        assertNotNull(references.get("INTEGRATION"));
        assertFalse(references.get("INTEGRATION").isEmpty());

        doDelete("/api/integration/" + integrationId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/converter/" + converterId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSecretUsedInAiModel() throws Exception {
        String secretName = "Gemini API key";
        String secretPlaceholder = toSecretPlaceholder(secretName, SecretType.TEXT);
        String apiToken = "wdfwefwefwef";
        SecretInfo secret = doPost("/api/secret", constructSecret(secretName, apiToken), SecretInfo.class);

        AiModel aiModel = new AiModel();
        aiModel.setName("Gemini v2.0");
        GoogleAiGeminiChatModelConfig config = new GoogleAiGeminiChatModelConfig(
                new GoogleAiGeminiProviderConfig(secretPlaceholder),
                "gemini-2.0",
                0.2,
                0.8,
                40,
                null,
                null,
                1024,
                10,
                3
        );
        aiModel.setConfiguration(config);
        AiModel savedAiModel = doPost("/api/ai/model", aiModel, AiModel.class);

        doReturn(FluentFuture.from(Futures.immediateFuture(ChatResponse.builder()
                .aiMessage(new AiMessage("test"))
                .build()))).when(aiRequestsExecutor).sendChatRequestAsync(any(), any());

        doPostAsync("/api/ai/model/chat", new TbChatRequest("test", new TbUserMessage(List.of(new TbContent.TbTextContent("test"))), config), TbChatResponse.class,
                status().isOk());
        verify(chatModelConfigurer).configureChatModel(ArgumentMatchers.<GoogleAiGeminiChatModelConfig>argThat(modelConfig -> {
            return modelConfig.providerConfig().apiKey().equals(apiToken);
        }));

        TbSecretDeleteResult result = readResponse(doDelete("/api/secret/" + secret.getUuidId())
                .andExpect(status().isBadRequest()), TbSecretDeleteResult.class);
        assertThat(result.getReferences().get(EntityType.AI_MODEL)).singleElement().satisfies(reference -> {
            assertThat(reference.getName()).isEqualTo(savedAiModel.getName());
            assertThat(reference.getId()).isEqualTo(savedAiModel.getId());

        });
    }

    @Test
    public void testUpdateSecretUsedInIntegration_thenReceiveLifecycleEvent() throws Exception {
        String secretName = "MqttSecret";
        String placeholder = toSecretPlaceholder(secretName, SecretType.TEXT);

        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret(secretName, "Password");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        ConverterId converterId = createConverter();
        IntegrationId integrationId = createIntegration(converterId, placeholder);

        secret = new Secret(savedSecret);
        secret.setValue("UpdatedPassword");

        // broadcast update event for integration with secret on secret update
        testBroadcastEntityStateChangeEventTime(integrationId, tenantId, 1);

        SecretInfo updatedSecret = doPost("/api/secret", secret, SecretInfo.class);
        retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, updatedSecret.getId().getId());
        assertThat(retrievedSecret).isEqualTo(updatedSecret);

        doDelete("/api/integration/" + integrationId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/converter/" + converterId.getId().toString()).andExpect(status().isOk());
        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecretNameProhibited() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Secret secret = constructSecret("Test Secret", "Prohibited");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        secret = new Secret(savedSecret);
        secret.setName("Updated Name");

        doPost("/api/secret", secret)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Can't update secret name!")));

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testFindSecretInfos() throws Exception {
        PageData<SecretInfo> pageData = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        int expectedSize = 10;
        String namePrefix = "Test Create Secret_";
        for (int i = 0; i < expectedSize; i++) {
            doPost("/api/secret", constructSecret(namePrefix + i, "CreatePassword"), SecretInfo.class);
        }

        PageData<SecretInfo> pageData2 = doGetTypedWithPageLink("/api/secrets?", PAGE_DATA_SECRET_TYPE_REF, new PageLink(expectedSize, 0));
        assertThat(pageData2.getData()).hasSize(expectedSize);

        List<UUID> toDelete = new ArrayList<>();

        for (int i = 0; i < expectedSize; i++) {
            SecretInfo secretInfo = pageData2.getData().get(i);
            assertThat(secretInfo.getName()).isEqualTo(namePrefix + i);
            toDelete.add(secretInfo.getUuidId());
        }

        toDelete.forEach(secret -> {
            try {
                doDelete("/api/secret/" + secret).andExpect(status().isOk());
                doGet("/api/secret/{id}/info", secret).andExpect(status().isNotFound());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testGetSecretInfoByName() throws Exception {
        String secretName = "T!^%|]/.@x()";
        Secret secret = constructSecret(secretName, "TestPassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());

        SecretInfo retrievedSecret = doGet("/api/secret?name=" + secretName, SecretInfo.class);
        assertThat(retrievedSecret).isEqualTo(savedSecret);

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSecretDescription() throws Exception {
        String secretName = "TestUpdateDescriptionSecret";
        Secret secret = constructSecret(secretName, "TestPassword");
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);

        assertNotNull(savedSecret);
        assertNotNull(savedSecret.getId());
        assertThat(savedSecret.getDescription()).isNull();

        String newDescription = "New description for the secret";
        SecretInfo updatedSecret = doPut("/api/secret/" + savedSecret.getId().getId() + "/description", newDescription, SecretInfo.class);

        assertThat(updatedSecret.getDescription()).isEqualTo(newDescription);

        SecretInfo retrievedSecret = doGet("/api/secret/{id}/info", SecretInfo.class, savedSecret.getId().getId());
        assertThat(retrievedSecret.getDescription()).isEqualTo(newDescription);

        updatedSecret = doPut("/api/secret/" + savedSecret.getId().getId() + "/description", "", SecretInfo.class);

        assertThat(updatedSecret.getDescription()).isNull();

        doDelete("/api/secret/" + savedSecret.getId().getId()).andExpect(status().isOk());
        doGet("/api/secret/{id}/info", savedSecret.getId().getId()).andExpect(status().isNotFound());
    }

    private Secret constructSecret(String name, String value) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setValue(value);
        secret.setType(SecretType.TEXT);
        return secret;
    }

    private ConverterId createConverter() {
        JsonNode converterConfiguration = JacksonUtil.newObjectNode().put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(converterConfiguration);
        converter = doPost("/api/converter", converter, Converter.class);
        return converter.getId();
    }

    private IntegrationId createIntegration(ConverterId converterId, String value) {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(converterId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(JacksonUtil.newObjectNode().putObject("metadata").put("key1", value));
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        Assert.assertNotNull(savedIntegration);
        Assert.assertNotNull(savedIntegration.getId());
        assertTrue(savedIntegration.getCreatedTime() > 0);
        return savedIntegration.getId();
    }

    private RuleChainId createRuleChain(String name, String clazz, String secretName) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        var result = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(result.getId());
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("Test");
        ruleNode.setType(clazz);
        TbMqttNodeConfiguration config = new TbMqttNodeConfiguration();
        config.setHost(toSecretPlaceholder(secretName, SecretType.TEXT));
        ruleNode.setConfiguration(JacksonUtil.valueToTree(config));
        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
        return result.getId();
    }

    private String toSecretPlaceholder(String name, SecretType type) {
        return String.format("${secret:%s;type:%s}", name, type);
    }

}
