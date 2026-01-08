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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.service.DaoSqlTest;

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
        assertThat(savedModel.getExternalId()).isNull();
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
