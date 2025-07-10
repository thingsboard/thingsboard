/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiModelControllerTest extends AbstractControllerTest {

    @SpyBean
    private EntitiesVersionControlService versionControlService;

    @SpyBean
    private TbLogEntityActionService logEntityActionService;

    /* --- Save API tests --- */

    @Test
    public void saveAiModel_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginSysAdmin();

        AiModel model = constructValidModel();

        // WHEN
        ResultActions result = doPost("/api/ai/model", model);

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void saveAiModel_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        // GIVEN
        loginCustomerUser();

        AiModel model = constructValidModel();

        // WHEN
        ResultActions result = doPost("/api/ai/model", model);

        // THEN
        result.andExpect(status().isForbidden()).andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void saveAiModel_whenCreatingValidModelAsTenantAdmin_shouldSucceed() throws Exception {
        // GIVEN
        loginTenantAdmin();

        AiModel model = constructValidModel();

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

        // verify auto-commit
        then(versionControlService).should().autoCommit(
                argThat(actualUser -> Objects.equals(actualUser.getId(), tenantAdminUser.getId())), eq(savedModel.getId())
        );

        // verify a rule engine message was sent, and an audit log was created
        then(logEntityActionService).should().logEntityAction(
                eq(tenantId),
                eq(savedModel.getId()),
                eq(savedModel),
                eq(ActionType.ADDED),
                argThat(actualUser -> Objects.equals(actualUser.getId(), tenantAdminUser.getId()))
        );
    }

    @Test
    public void saveAiModel_whenUpdatingExistingModelAsTenantAdmin_shouldSucceed() throws Exception {
        // GIVEN
        loginTenantAdmin();

        var model = doPost("/api/ai/model", constructValidModel(), AiModel.class);

        var newModelConfig = OpenAiChatModelConfig.builder()
                .providerConfig(new OpenAiProviderConfig("test-api-key-updated"))
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

        // verify auto-commit
        then(versionControlService).should(times(2)).autoCommit(
                argThat(actualUser -> Objects.equals(actualUser.getId(), tenantAdminUser.getId())), eq(updatedModel.getId())
        );

        // verify a rule engine message was sent, and an audit log was created
        then(logEntityActionService).should().logEntityAction(
                eq(tenantId), eq(updatedModel.getId()), eq(updatedModel), eq(ActionType.UPDATED),
                argThat(actualUser -> Objects.equals(actualUser.getId(), tenantAdminUser.getId()))
        );
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

        var saved = doPost("/api/ai/model", constructValidModel(), AiModel.class);

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

        var model = doPost("/api/ai/model", constructValidModel(), AiModel.class);

        // WHEN
        boolean deleted = doDelete("/api/ai/model/" + model.getId(), Boolean.class);

        // THEN
        assertThat(deleted).isTrue();

        // verify a rule engine message was sent, and an audit log was created
        then(logEntityActionService).should().logEntityAction(
                eq(tenantId),
                eq(model.getId()),
                eq(model),
                eq(ActionType.DELETED),
                argThat(actualUser -> Objects.equals(actualUser.getId(), tenantAdminUser.getId())),
                eq(model.getId().toString())
        );

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

        // verify a rule engine message was not sent, and an audit log was not created
        then(logEntityActionService).should(never()).logEntityAction(
                eq(tenantId),
                eq(nonexistentModelId),
                any(AiModel.class),
                eq(ActionType.DELETED),
                argThat(actualUser -> Objects.equals(actualUser.getId(), tenantAdminUser.getId())),
                eq(nonexistentModelId.toString())
        );
    }

    private AiModel constructValidModel() {
        var modelConfig = OpenAiChatModelConfig.builder()
                .providerConfig(new OpenAiProviderConfig("test-api-key"))
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
                .name("Test model")
                .configuration(modelConfig)
                .build();
    }

}
