// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.AiModelInfo;
import org.thingsboard.server.common.data.ai.dto.TbChatRequest;
import org.thingsboard.server.common.data.ai.dto.TbChatResponse;
import org.thingsboard.server.common.data.ai.dto.TbContent;
import org.thingsboard.server.common.data.ai.dto.TbUserMessage;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiModelControllerTest extends AbstractControllerTest {

    /* --- Save API tests --- */

    @Test
    public void saveAiModel_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginSysAdmin();

        AiModel model = constructValidOpenAiModel("Test model");

        // WHEN
        ResultActions result = doPost("/api/ai/model", model);

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void saveAiModel_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginCustomerUser();

        AiModel model = constructValidOpenAiModel("Test model");

        // WHEN
        ResultActions result = doPost("/api/ai/model", model);

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void saveAiModel_whenCreatingValidModelAsTenantAdmin_shouldSucceed() throws Exception {
        // GIVEN
        loginTenantAdmin();

        AiModel model = constructValidOpenAiModel("Test model");

        // WHEN
        var savedModel = doPost("/api/ai/model", model, AiModel.class);

        // THEN

        // verify returned object
        assertThat(savedModel.getId()).isNotNull();
        assertThat(savedModel.getUuidId()).isNotNull().isNotEqualTo(EntityId.NULL_UUID);
        assertThat(savedModel.getId().getEntityType()).isEqualTo(EntityType.AI_MODEL);
        assertThat(savedModel.getCreatedTime()).isPositive();
        assertThat(savedModel.getVersion()).isEqualTo(1);
        assertThat(savedModel.getTenantId()).isEqualTo(tenantId);
        assertThat(savedModel.getName()).isEqualTo("Test model");
        assertThat(savedModel.getConfiguration()).isEqualTo(model.getConfiguration());
        assertThat(savedModel.isAvailableViaApi()).isFalse();
        assertThat(savedModel.getExternalId()).isNull();
    }

    @Test
    public void saveAiModel_whenAvailableViaApiIsSet_shouldPersistIt() throws Exception {
        // GIVEN
        loginTenantAdmin();

        AiModel model = constructValidOpenAiModel("Test model");
        model.setAvailableViaApi(true);

        // WHEN
        var savedModel = doPost("/api/ai/model", model, AiModel.class);

        // THEN
        assertThat(savedModel.isAvailableViaApi()).isTrue();
        assertThat(doGet("/api/ai/model/" + savedModel.getId(), AiModel.class).isAvailableViaApi()).isTrue();
    }

    @Test
    public void saveAiModel_whenUpdatingExistingModelAsTenantAdmin_shouldSucceed() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var model = doPost("/api/ai/model", constructValidOpenAiModel("Test model"), AiModel.class);

        var newModelConfig = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl(OpenAiProviderConfig.OPENAI_OFFICIAL_BASE_URL)
                        .apiKey("test-api-key-updated")
                        .build())
                .modelId("o4-mini")
                .temperature(0.2)
                .topP(0.4)
                .frequencyPenalty(0.2)
                .presencePenalty(0.5)
                .maxOutputTokens(2000)
                .timeoutSeconds(20)
                .maxRetries(0)
                .build();

        model.setName("Test model updated");
        model.setConfiguration(newModelConfig);

        // WHEN
        var updatedModel = doPost("/api/ai/model", model, AiModel.class);

        // THEN

        // verify returned object
        assertThat(updatedModel.getId()).isEqualTo(model.getId());
        assertThat(updatedModel.getCreatedTime()).isEqualTo(model.getCreatedTime());
        assertThat(updatedModel.getVersion()).isEqualTo(2);
        assertThat(updatedModel.getTenantId()).isEqualTo(tenantId);
        assertThat(updatedModel.getName()).isEqualTo("Test model updated");
        assertThat(updatedModel.getConfiguration()).isEqualTo(newModelConfig);
        assertThat(updatedModel.getExternalId()).isNull();
    }

    // checks that fileName is nullable in PE since it can be null if secrets are used
    @Test
    public void saveAiModel_whenCreatingValidVertexModelWithSecretsUsedForKeyAsTenantAdmin_shouldSucceed() throws Exception {
        // GIVEN
        loginTenantAdmin();

        // if secrets are used, fileName is null and serviceAccountKey contains a reference to a secret instead of actual contents of key file
        var providerConfig = GoogleVertexAiGeminiProviderConfig.builder()
                .fileName(null)
                .projectId("test-project-123")
                .location("us-south1")
                .serviceAccountKey("${secret:test-key;type:TEXT_FILE}")
                .build();

        var modelConfig = GoogleVertexAiGeminiChatModelConfig.builder()
                .providerConfig(providerConfig)
                .modelId("gemini-2.5-pro")
                .temperature(0.5)
                .topP(0.3)
                .frequencyPenalty(0.1)
                .presencePenalty(0.2)
                .maxOutputTokens(1000)
                .timeoutSeconds(60)
                .maxRetries(2)
                .build();

        var model = AiModel.builder()
                .tenantId(tenantId)
                .name("test-vertex-ai")
                .configuration(modelConfig)
                .build();

        // WHEN
        var savedModel = doPost("/api/ai/model", model, AiModel.class);

        // THEN

        // verify returned object
        assertThat(savedModel.getId()).isNotNull();
        assertThat(savedModel.getUuidId()).isNotNull().isNotEqualTo(EntityId.NULL_UUID);
        assertThat(savedModel.getId().getEntityType()).isEqualTo(EntityType.AI_MODEL);
        assertThat(savedModel.getCreatedTime()).isPositive();
        assertThat(savedModel.getVersion()).isEqualTo(1);
        assertThat(savedModel.getTenantId()).isEqualTo(tenantId);
        assertThat(savedModel.getName()).isEqualTo("test-vertex-ai");
        assertThat(savedModel.getConfiguration()).isEqualTo(model.getConfiguration());
        assertThat(savedModel.getExternalId()).isNull();
    }

    @Test
    public void saveAiModel_whenBaseUrlIsPrivateIp_shouldReturnBadRequest() throws Exception {
        // GIVEN
        loginTenantAdmin();
        SsrfProtectionValidator.setEnabled(true);

        try {
            var modelConfig = OpenAiChatModelConfig.builder()
                    .providerConfig(OpenAiProviderConfig.builder()
                            .baseUrl("http://172.17.0.1:22/")
                            .apiKey("test-api-key")
                            .build())
                    .modelId("gpt-4o")
                    .build();

            AiModel model = AiModel.builder()
                    .tenantId(tenantId)
                    .name("SSRF test model")
                    .configuration(modelConfig)
                    .build();

            // WHEN
            ResultActions result = doPost("/api/ai/model", model);

            // THEN
            result.andExpect(status().isBadRequest());
        } finally {
            SsrfProtectionValidator.setEnabled(false);
        }
    }

    @Test
    public void sendChatRequest_whenBaseUrlBlockedAtRuntime_shouldReturnFailureEnvelope() throws Exception {
        // GIVEN
        loginTenantAdmin();
        SsrfProtectionValidator.setEnabled(true);

        try {
            var modelConfig = OpenAiChatModelConfig.builder()
                    .providerConfig(OpenAiProviderConfig.builder()
                            .baseUrl("http://10.0.0.1:8080/")
                            .apiKey("test-api-key")
                            .build())
                    .modelId("gpt-4o")
                    .build();

            var chatRequest = new TbChatRequest(
                    null,
                    new TbUserMessage(List.of(new TbContent.TbTextContent("hi"))),
                    modelConfig);

            // WHEN
            TbChatResponse response = doPostAsync("/api/ai/model/chat", chatRequest, TbChatResponse.class, status().isOk());

            // THEN
            assertThat(response).isInstanceOf(TbChatResponse.Failure.class);
            assertThat(((TbChatResponse.Failure) response).errorDetails()).contains("URI is invalid");
        } finally {
            SsrfProtectionValidator.setEnabled(false);
        }
    }

    @Test
    public void sendChatRequest_whenChatModelConfigIsMissing_shouldReturnBadRequest() throws Exception {
        // GIVEN
        loginTenantAdmin();

        // WHEN
        ResultActions result = doPost("/api/ai/model/chat", constructChatRequest());

        // THEN
        result.andExpect(status().isBadRequest())
                .andExpect(statusReason(is("Validation error: chatModelConfig must not be null")));
    }

    /* --- Chat with saved model API tests --- */

    @Test
    public void sendChatRequestToAiModel_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        ResultActions result = doPost("/api/ai/model/" + Uuids.timeBased() + "/chat", constructChatRequest());

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void sendChatRequestToAiModel_whenModelDoesNotExist_shouldReturnNotFound() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var nonexistentModelId = new AiModelId(Uuids.timeBased());

        // WHEN
        ResultActions result = doPost("/api/ai/model/" + nonexistentModelId + "/chat", constructChatRequest());

        // THEN
        result.andExpect(status().isNotFound())
                .andExpect(statusReason(is("AI model with id [" + nonexistentModelId + "] is not found")));
    }

    @Test
    public void sendChatRequestToAiModel_whenModelIsNotAvailableViaApi_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var model = doPost("/api/ai/model", constructValidOpenAiModel("Test model"), AiModel.class);

        // WHEN
        ResultActions result = doPost("/api/ai/model/" + model.getId() + "/chat", constructChatRequest());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(is("AI model with id [" + model.getId() + "] is not available via API")));
    }

    @Test
    public void sendChatRequestToAiModel_whenTenantAdmin_shouldUseStoredConfigurationAndReturnProviderError() throws Exception {
        // GIVEN
        loginTenantAdmin();
        var model = saveModelAvailableViaApiWithBlockedBaseUrl();
        SsrfProtectionValidator.setEnabled(true);

        try {
            // WHEN
            TbChatResponse response = doPostAsync("/api/ai/model/" + model.getId() + "/chat", constructChatRequest(), TbChatResponse.class, status().isOk());

            // THEN
            assertThat(response).isInstanceOf(TbChatResponse.Failure.class);
            assertThat(((TbChatResponse.Failure) response).errorDetails()).contains("URI is invalid");
        } finally {
            SsrfProtectionValidator.setEnabled(false);
        }
    }

    @Test
    public void sendChatRequestToAiModel_whenCustomerAdmin_shouldBeAllowedAndHideProviderError() throws Exception {
        // GIVEN
        loginTenantAdmin();
        var model = saveModelAvailableViaApiWithBlockedBaseUrl();
        loginCustomerAdminUser();
        SsrfProtectionValidator.setEnabled(true);

        try {
            // WHEN
            TbChatResponse response = doPostAsync("/api/ai/model/" + model.getId() + "/chat", constructChatRequest(), TbChatResponse.class, status().isOk());

            // THEN
            assertThat(response).isInstanceOf(TbChatResponse.Failure.class);
            assertThat(((TbChatResponse.Failure) response).errorDetails()).isEqualTo("Failed to get a response from the AI model");
        } finally {
            SsrfProtectionValidator.setEnabled(false);
        }
    }

    @Test
    public void sendChatRequestToAiModel_whenCustomerUserWithoutInvokePermission_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginTenantAdmin();
        var model = saveModelAvailableViaApiWithBlockedBaseUrl();
        loginCustomerUser();

        // WHEN
        ResultActions result = doPost("/api/ai/model/" + model.getId() + "/chat", constructChatRequest());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo("You don't have permission to perform 'INVOKE' operation with AI_MODEL 'Test model'!")));
    }

    /* --- Customer permission tests --- */

    @Test
    public void aiModelPermission_whenCustomerAdminGrantedAll_shouldAllowInvokeOnly() throws Exception {
        // GIVEN
        loginTenantAdmin();
        var model = doPost("/api/ai/model", constructValidOpenAiModel("Test model"), AiModel.class);
        loginCustomerAdminUser();

        // WHEN / THEN
        assertThat(hasAiModelPermission(model, "INVOKE")).isTrue();
        assertThat(hasAiModelPermission(model, "READ")).isFalse();
        assertThat(hasAiModelPermission(model, "WRITE")).isFalse();
        assertThat(hasAiModelPermission(model, "DELETE")).isFalse();
    }

    @Test
    public void aiModelPermission_whenCustomerUserGrantedReadOnAllResources_shouldDenyReadAndInvoke() throws Exception {
        // GIVEN
        loginTenantAdmin();
        var model = doPost("/api/ai/model", constructValidOpenAiModel("Test model"), AiModel.class);
        loginCustomerUser();

        // WHEN / THEN
        assertThat(hasAiModelPermission(model, "READ")).isFalse();
        assertThat(hasAiModelPermission(model, "INVOKE")).isFalse();
    }

    /* --- Get by ID API tests --- */

    @Test
    public void getAiModelById_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        ResultActions result = doGet("/api/ai/model/" + Uuids.timeBased());

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void getAiModelById_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        ResultActions result = doGet("/api/ai/model/" + Uuids.timeBased());

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void getAiModelById_whenGettingExistingModelAsTenantAdmin_shouldReturnModel() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var saved = doPost("/api/ai/model", constructValidOpenAiModel("Test model"), AiModel.class);

        // WHEN
        AiModel actual = doGet("/api/ai/model/" + saved.getId(), AiModel.class);

        // THEN
        assertThat(actual).isEqualTo(saved);
    }

    @Test
    public void getAiModelById_whenGettingNonexistentModelAsTenantAdmin_shouldReturnNotFound() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var nonexistentModelId = new AiModelId(Uuids.timeBased());

        // WHEN
        ResultActions result = doGet("/api/ai/model/" + nonexistentModelId);

        // THEN
        result.andExpect(status().isNotFound())
                .andExpect(statusReason(is("AI model with id [" + nonexistentModelId + "] is not found")));
    }

    /* --- Get paged API tests --- */

    @Test
    public void getAiModels_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        ResultActions result = doGet("/api/ai/model?pageSize=10&page=0");

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void getAiModels_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        ResultActions result = doGet("/api/ai/model?pageSize=10&page=0");

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void getAiModels_testPagination() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var model1 = doPost("/api/ai/model", constructValidOpenAiModel("Test model 1"), AiModel.class);
        var model2 = doPost("/api/ai/model", constructValidOpenAiModel("Test model 2"), AiModel.class);
        var model3 = doPost("/api/ai/model", constructValidOpenAiModel("Test model 3"), AiModel.class);
        var model4 = doPost("/api/ai/model", constructValidOpenAiModel("Test model 4"), AiModel.class);
        var model5 = doPost("/api/ai/model", constructValidOpenAiModel("Test model 5"), AiModel.class);

        // WHEN
        PageData<AiModel> result = doGetTypedWithPageLink("/api/ai/model?", new TypeReference<>() {}, new PageLink(2, 1));

        // THEN
        assertThat(result.getData()).containsExactly(model3, model4);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    public void getAiModels_testSearchAndSortAppliedBeforePagination() throws Exception {
        // GIVEN
        loginTenantAdmin();

        // Create 5 models: 3 with "Alpha" in name, 2 with "Beta" in name
        var alpha1 = doPost("/api/ai/model", constructValidOpenAiModel("Alpha Model 1"), AiModel.class);
        var beta1 = doPost("/api/ai/model", constructValidOpenAiModel("Beta Model 1"), AiModel.class);
        var alpha2 = doPost("/api/ai/model", constructValidOpenAiModel("Alpha Model 2"), AiModel.class);
        var beta2 = doPost("/api/ai/model", constructValidOpenAiModel("Beta Model 2"), AiModel.class);
        var alpha3 = doPost("/api/ai/model", constructValidOpenAiModel("Alpha Model 3"), AiModel.class);

        // WHEN
        // Search for "Alpha", sort by name DESC, get the first page with size 2
        PageData<AiModel> result = doGetTypedWithPageLink("/api/ai/model?",
                new TypeReference<>() {},
                new PageLink(2, 0, "Alpha", SortOrder.of("name", SortOrder.Direction.DESC)));

        // THEN
        // Should find only 3 "Alpha" models, sort them DESC (3, 2, 1), then return first 2
        assertThat(result.getData()).containsExactly(alpha3, alpha2);
        assertThat(result.getTotalPages()).isEqualTo(2); // One more "Alpha" model on the next page
        assertThat(result.getTotalElements()).isEqualTo(3); // Only 3 models match "Alpha", not 5
        assertThat(result.hasNext()).isTrue(); // One more "Alpha" model on the next page
    }

    @Test
    public void getAiModels_testTextSearch() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var model1 = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 1")
                .configuration(OpenAiChatModelConfig.builder()
                        .providerConfig(OpenAiProviderConfig.builder().apiKey("test-api-key").build())
                        .modelId("o3-pro")
                        .build())
                .build(), AiModel.class);
        var model2 = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 2")
                .configuration(GoogleAiGeminiChatModelConfig.builder()
                        .providerConfig(new GoogleAiGeminiProviderConfig("test-api-key"))
                        .modelId("gemini-2.5-flash")
                        .build())
                .build(), AiModel.class);
        var model3 = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 3")
                .configuration(GoogleAiGeminiChatModelConfig.builder()
                        .providerConfig(new GoogleAiGeminiProviderConfig("test-api-key"))
                        .modelId("gemini-2.5-pro")
                        .build())
                .build(), AiModel.class);

        // WHEN
        int pageSize = 10;
        int page = 0;
        SortOrder sortOrder = null;

        PageData<AiModel> result1 = doGetTypedWithPageLink("/api/ai/model?", new TypeReference<>() {}, new PageLink(pageSize, page, "google ai", sortOrder));

        PageData<AiModel> result2 = doGetTypedWithPageLink("/api/ai/model?", new TypeReference<>() {}, new PageLink(pageSize, page, "pro", sortOrder));

        PageData<AiModel> result3 = doGetTypedWithPageLink("/api/ai/model?", new TypeReference<>() {}, new PageLink(pageSize, page, "test", sortOrder));

        PageData<AiModel> result4 = doGetTypedWithPageLink("/api/ai/model?", new TypeReference<>() {}, new PageLink(pageSize, page, "anthropic", sortOrder));

        // THEN

        // should find google models
        assertThat(result1.getData()).containsExactly(model2, model3);
        assertThat(result1.getTotalPages()).isEqualTo(1);
        assertThat(result1.getTotalElements()).isEqualTo(2);
        assertThat(result1.hasNext()).isFalse();

        // should find "o3-pro" and "gemini-2.5-pro" models
        assertThat(result2.getData()).containsExactly(model1, model3);
        assertThat(result2.getTotalPages()).isEqualTo(1);
        assertThat(result2.getTotalElements()).isEqualTo(2);
        assertThat(result2.hasNext()).isFalse();

        // should find all models (all contain "Test" in their names)
        assertThat(result3.getData()).containsExactly(model1, model2, model3);
        assertThat(result3.getTotalPages()).isEqualTo(1);
        assertThat(result3.getTotalElements()).isEqualTo(3);
        assertThat(result3.hasNext()).isFalse();

        // should find no models (nothing matches "anthropic")
        assertThat(result4.getData()).isEmpty();
        assertThat(result4.getTotalPages()).isEqualTo(0);
        assertThat(result4.getTotalElements()).isEqualTo(0);
        assertThat(result4.hasNext()).isFalse();
    }

    @Test
    public void getAiModels_testFilterByAvailableViaApi() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var available = constructValidOpenAiModel("Available model");
        available.setAvailableViaApi(true);
        available = doPost("/api/ai/model", available, AiModel.class);
        var unavailable = doPost("/api/ai/model", constructValidOpenAiModel("Unavailable model"), AiModel.class);

        // WHEN
        PageData<AiModel> onlyAvailable = doGetTypedWithPageLink("/api/ai/model?availableViaApi=true&", new TypeReference<>() {}, new PageLink(10));
        PageData<AiModel> onlyUnavailable = doGetTypedWithPageLink("/api/ai/model?availableViaApi=false&", new TypeReference<>() {}, new PageLink(10));
        PageData<AiModel> all = doGetTypedWithPageLink("/api/ai/model?", new TypeReference<>() {}, new PageLink(10));

        // THEN
        assertThat(onlyAvailable.getData()).containsExactly(available);
        assertThat(onlyUnavailable.getData()).containsExactly(unavailable);
        assertThat(all.getData()).containsExactlyInAnyOrder(available, unavailable);
    }

    /* --- Get infos API tests --- */

    @Test
    public void getAiModelInfos_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        ResultActions result = doGet("/api/ai/model/infos?pageSize=10&page=0");

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void getAiModelInfos_whenCustomerUserWithoutInvokePermission_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        ResultActions result = doGet("/api/ai/model/infos?pageSize=10&page=0");

        // THEN
        result.andExpect(status().isForbidden());
    }

    @Test
    public void getAiModelInfos_whenTenantAdmin_shouldReturnInfosFilteredByAvailableViaApi() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var available = constructValidOpenAiModel("Available model");
        available.setAvailableViaApi(true);
        available = doPost("/api/ai/model", available, AiModel.class);
        var unavailable = doPost("/api/ai/model", constructValidOpenAiModel("Unavailable model"), AiModel.class);

        // WHEN
        PageData<AiModelInfo> onlyAvailable = doGetTypedWithPageLink("/api/ai/model/infos?availableViaApi=true&", new TypeReference<>() {}, new PageLink(10));
        PageData<AiModelInfo> all = doGetTypedWithPageLink("/api/ai/model/infos?", new TypeReference<>() {}, new PageLink(10));

        // THEN
        assertThat(onlyAvailable.getData()).containsExactly(new AiModelInfo(available));
        assertThat(all.getData()).containsExactlyInAnyOrder(new AiModelInfo(available), new AiModelInfo(unavailable));
        assertThat(onlyAvailable.getData().get(0).getProvider()).isEqualTo(AiProvider.OPENAI);
        assertThat(onlyAvailable.getData().get(0).getModelId()).isEqualTo("gpt-4o");
    }

    @Test
    public void getAiModelInfos_testSearchAndSortByProviderAndModelId() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var anthropicModel = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 1")
                .configuration(AnthropicChatModelConfig.builder()
                        .providerConfig(new AnthropicProviderConfig("test-api-key"))
                        .modelId("claude-sonnet-4-0")
                        .build())
                .build(), AiModel.class);
        var geminiModel = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 2")
                .configuration(GoogleAiGeminiChatModelConfig.builder()
                        .providerConfig(new GoogleAiGeminiProviderConfig("test-api-key"))
                        .modelId("gemini-2.5-pro")
                        .build())
                .build(), AiModel.class);

        // WHEN
        PageData<AiModelInfo> byProviderDesc = doGetTypedWithPageLink("/api/ai/model/infos?", new TypeReference<>() {},
                new PageLink(10, 0, null, SortOrder.of("provider", SortOrder.Direction.DESC)));
        PageData<AiModelInfo> byModelIdAsc = doGetTypedWithPageLink("/api/ai/model/infos?", new TypeReference<>() {},
                new PageLink(10, 0, null, SortOrder.of("modelId", SortOrder.Direction.ASC)));
        PageData<AiModelInfo> searchByProvider = doGetTypedWithPageLink("/api/ai/model/infos?", new TypeReference<>() {},
                new PageLink(10, 0, "google ai"));
        PageData<AiModelInfo> searchByModelId = doGetTypedWithPageLink("/api/ai/model/infos?", new TypeReference<>() {},
                new PageLink(10, 0, "sonnet"));

        // THEN
        assertThat(byProviderDesc.getData()).containsExactly(new AiModelInfo(geminiModel), new AiModelInfo(anthropicModel));
        assertThat(byModelIdAsc.getData()).containsExactly(new AiModelInfo(anthropicModel), new AiModelInfo(geminiModel));
        assertThat(searchByProvider.getData()).containsExactly(new AiModelInfo(geminiModel));
        assertThat(searchByModelId.getData()).containsExactly(new AiModelInfo(anthropicModel));
    }

    @Test
    public void getAiModelInfos_whenCustomerAdmin_shouldReturnInfosWithoutConfiguration() throws Exception {
        // GIVEN
        loginTenantAdmin();
        var model = saveModelAvailableViaApiWithBlockedBaseUrl();
        loginCustomerAdminUser();

        // WHEN
        String response = doGet("/api/ai/model/infos?pageSize=10&page=0").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(response).contains(model.getId().getId().toString()).doesNotContain("test-api-key");
    }

    @Test
    public void getAiModels_testSortingByCreatedTime() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var model1 = doPost("/api/ai/model", constructValidOpenAiModel("Test model 1"), AiModel.class);
        var model2 = doPost("/api/ai/model", constructValidOpenAiModel("Test model 2"), AiModel.class);

        // WHEN
        int pageSize = 2;
        int page = 0;
        String textSearch = null;

        PageData<AiModel> resultAsc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("createdTime", SortOrder.Direction.ASC))
        );
        PageData<AiModel> resultDesc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("createdTime", SortOrder.Direction.DESC))
        );

        // THEN
        assertThat(resultAsc.getData()).containsExactly(model1, model2);
        assertThat(resultAsc.getTotalPages()).isEqualTo(1);
        assertThat(resultAsc.getTotalElements()).isEqualTo(2);
        assertThat(resultAsc.hasNext()).isFalse();

        assertThat(resultDesc.getData()).containsExactly(model2, model1);
        assertThat(resultDesc.getTotalPages()).isEqualTo(1);
        assertThat(resultDesc.getTotalElements()).isEqualTo(2);
        assertThat(resultDesc.hasNext()).isFalse();
    }

    @Test
    public void getAiModels_testSortingByName() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var modelA = doPost("/api/ai/model", constructValidOpenAiModel("Test model A"), AiModel.class);
        var modelB = doPost("/api/ai/model", constructValidOpenAiModel("Test model B"), AiModel.class);

        // WHEN
        int pageSize = 2;
        int page = 0;
        String textSearch = null;

        PageData<AiModel> resultAsc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("name", SortOrder.Direction.ASC))
        );
        PageData<AiModel> resultDesc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("name", SortOrder.Direction.DESC))
        );

        // THEN
        assertThat(resultAsc.getData()).containsExactly(modelA, modelB);
        assertThat(resultAsc.getTotalPages()).isEqualTo(1);
        assertThat(resultAsc.getTotalElements()).isEqualTo(2);
        assertThat(resultAsc.hasNext()).isFalse();

        assertThat(resultDesc.getData()).containsExactly(modelB, modelA);
        assertThat(resultDesc.getTotalPages()).isEqualTo(1);
        assertThat(resultDesc.getTotalElements()).isEqualTo(2);
        assertThat(resultDesc.hasNext()).isFalse();
    }

    @Test
    public void getAiModels_testSortingByProvider() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var anthropicModel = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 1")
                .configuration(AnthropicChatModelConfig.builder()
                        .providerConfig(new AnthropicProviderConfig("test-api-key"))
                        .modelId("claude-sonnet-4-0")
                        .build())
                .build(), AiModel.class);
        var geminiModel = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 2")
                .configuration(GoogleAiGeminiChatModelConfig.builder()
                        .providerConfig(new GoogleAiGeminiProviderConfig("test-api-key"))
                        .modelId("gemini-2.5-pro")
                        .build())
                .build(), AiModel.class);

        // WHEN
        int pageSize = 2;
        int page = 0;
        String textSearch = null;

        PageData<AiModel> resultAsc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("provider", SortOrder.Direction.ASC))
        );
        PageData<AiModel> resultDesc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("provider", SortOrder.Direction.DESC))
        );

        // THEN
        assertThat(resultAsc.getData()).containsExactly(anthropicModel, geminiModel);
        assertThat(resultAsc.getTotalPages()).isEqualTo(1);
        assertThat(resultAsc.getTotalElements()).isEqualTo(2);
        assertThat(resultAsc.hasNext()).isFalse();

        assertThat(resultDesc.getData()).containsExactly(geminiModel, anthropicModel);
        assertThat(resultDesc.getTotalPages()).isEqualTo(1);
        assertThat(resultDesc.getTotalElements()).isEqualTo(2);
        assertThat(resultDesc.hasNext()).isFalse();
    }

    @Test
    public void getAiModels_testSortingByModelId() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var modelA = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 1")
                .configuration(AnthropicChatModelConfig.builder()
                        .providerConfig(new AnthropicProviderConfig("test-api-key"))
                        .modelId("model-a")
                        .build())
                .build(), AiModel.class);

        var modelB = doPost("/api/ai/model", AiModel.builder()
                .tenantId(tenantId)
                .name("Test model 2")
                .configuration(GoogleAiGeminiChatModelConfig.builder()
                        .providerConfig(new GoogleAiGeminiProviderConfig("test-api-key"))
                        .modelId("model-b")
                        .build())
                .build(), AiModel.class);

        // WHEN
        int pageSize = 2;
        int page = 0;
        String textSearch = null;

        PageData<AiModel> resultAsc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("modelId", SortOrder.Direction.ASC))
        );
        PageData<AiModel> resultDesc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("modelId", SortOrder.Direction.DESC))
        );

        // THEN
        assertThat(resultAsc.getData()).containsExactly(modelA, modelB);
        assertThat(resultAsc.getTotalPages()).isEqualTo(1);
        assertThat(resultAsc.getTotalElements()).isEqualTo(2);
        assertThat(resultAsc.hasNext()).isFalse();

        assertThat(resultDesc.getData()).containsExactly(modelB, modelA);
        assertThat(resultDesc.getTotalPages()).isEqualTo(1);
        assertThat(resultDesc.getTotalElements()).isEqualTo(2);
        assertThat(resultDesc.hasNext()).isFalse();
    }

    @Test
    public void getAiModels_testSortingByIdTieBreaker() throws Exception {
        // GIVEN
        loginTenantAdmin();

        // Both models are from OpenAI and sorting will be done on provider
        var modelA = doPost("/api/ai/model", constructValidOpenAiModel("Test model A"), AiModel.class);
        var modelB = doPost("/api/ai/model", constructValidOpenAiModel("Test model B"), AiModel.class);

        // WHEN
        int pageSize = 2;
        int page = 0;
        String textSearch = null;

        PageData<AiModel> resultAsc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("provider", SortOrder.Direction.ASC))
        );
        PageData<AiModel> resultDesc = doGetTypedWithPageLink(
                "/api/ai/model?", new TypeReference<>() {},
                new PageLink(pageSize, page, textSearch, SortOrder.of("provider", SortOrder.Direction.DESC))
        );

        // THEN

        // in both cases result should be the same since in case of ties (both models have OpenAI as provider, sorting by ID ascending is used)
        assertThat(resultAsc.getData()).containsExactly(modelA, modelB);
        assertThat(resultAsc.getTotalPages()).isEqualTo(1);
        assertThat(resultAsc.getTotalElements()).isEqualTo(2);
        assertThat(resultAsc.hasNext()).isFalse();

        assertThat(resultDesc.getData()).containsExactly(modelA, modelB);
        assertThat(resultDesc.getTotalPages()).isEqualTo(1);
        assertThat(resultDesc.getTotalElements()).isEqualTo(2);
        assertThat(resultDesc.hasNext()).isFalse();
    }

    /* --- Delete API tests --- */

    @Test
    public void deleteAiModelById_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        ResultActions result = doDelete("/api/ai/model/" + Uuids.timeBased());

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void deleteAiModelById_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        ResultActions result = doDelete("/api/ai/model/" + Uuids.timeBased());

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void deleteAiModelById_whenDeletingExistingModelAsTenantAdmin_shouldSucceedAndReturnTrue() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var model = doPost("/api/ai/model", constructValidOpenAiModel("Test model"), AiModel.class);

        // WHEN
        boolean deleted = doDelete("/api/ai/model/" + model.getId(), Boolean.class);

        // THEN
        assertThat(deleted).isTrue();

        // verify model cannot be found anymore
        doGet("/api/ai/model/" + model.getId())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(is("AI model with id [" + model.getId() + "] is not found")));
    }

    @Test
    public void deleteAiModelById_whenDeletingNonexistentModelAsTenantAdmin_shouldSucceedAndReturnFalse() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var nonexistentModelId = new AiModelId(Uuids.timeBased());

        // WHEN
        boolean deleted = doDelete("/api/ai/model/" + nonexistentModelId, Boolean.class);

        // THEN
        assertThat(deleted).isFalse();
    }

    private AiModel saveModelAvailableViaApiWithBlockedBaseUrl() throws Exception {
        var modelConfig = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl("http://10.0.0.1:8080/")
                        .apiKey("test-api-key")
                        .build())
                .modelId("gpt-4o")
                .build();

        var model = AiModel.builder()
                .tenantId(tenantId)
                .name("Test model")
                .configuration(modelConfig)
                .availableViaApi(true)
                .build();

        return doPost("/api/ai/model", model, AiModel.class);
    }

    private TbChatRequest constructChatRequest() {
        return new TbChatRequest(null, new TbUserMessage(List.of(new TbContent.TbTextContent("hi"))), null);
    }

    private boolean hasAiModelPermission(AiModel model, String operation) throws Exception {
        return doGet("/api/permission/AI_MODEL/" + model.getId() + "/" + operation, Boolean.class);
    }

    private AiModel constructValidOpenAiModel(String name) {
        var modelConfig = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl(OpenAiProviderConfig.OPENAI_OFFICIAL_BASE_URL)
                        .apiKey("test-api-key")
                        .build())
                .modelId("gpt-4o")
                .temperature(0.5)
                .topP(0.3)
                .frequencyPenalty(0.1)
                .presencePenalty(0.2)
                .maxOutputTokens(1000)
                .timeoutSeconds(60)
                .maxRetries(2)
                .build();

        return AiModel.builder()
                .tenantId(tenantId)
                .name(name)
                .configuration(modelConfig)
                .build();
    }

}
