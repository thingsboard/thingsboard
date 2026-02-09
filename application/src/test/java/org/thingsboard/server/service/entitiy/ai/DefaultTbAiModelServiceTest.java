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
package org.thingsboard.server.service.entitiy.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DefaultTbAiModelServiceTest {

    @Mock
    EntitiesVersionControlService vcServiceMock;

    @Mock
    AiModelService aiModelServiceMock;

    @Mock
    TbLogEntityActionService logEntityActionServiceMock;

    @Spy
    @InjectMocks
    DefaultTbAiModelService service;

    TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setTenantId(tenantId);

        service = new DefaultTbAiModelService(aiModelServiceMock);
        ReflectionTestUtils.setField(service, "vcService", vcServiceMock);
        ReflectionTestUtils.setField(service, "logEntityActionService", logEntityActionServiceMock);
    }

    @Test
    void save_whenCreatingNewModel_shouldAutoCommitAndLogAddedActionAndUseTenantIdFromUser() {
        // GIVEN
        var modelToSave = AiModel.builder()
                .name("Model to save")
                .configuration(constructValidOpenAiModelConfig())
                .build();

        var savedModel = new AiModel(modelToSave);
        savedModel.setId(new AiModelId(UUID.randomUUID()));
        savedModel.setTenantId(user.getTenantId());
        savedModel.setVersion(1L);
        savedModel.setCreatedTime(System.currentTimeMillis());

        given(aiModelServiceMock.save(modelToSave)).willReturn(savedModel);

        // WHEN
        AiModel result = service.save(modelToSave, user);

        // THEN
        assertThat(result).isEqualTo(savedModel);

        then(aiModelServiceMock).should().save(modelToSave);
        then(vcServiceMock).should().autoCommit(user, savedModel.getId());
        then(logEntityActionServiceMock).should().logEntityAction(tenantId, savedModel.getId(), savedModel, ActionType.ADDED, user);
    }

    @Test
    void save_whenUpdatingExistingModel_shouldAutoCommitAndLogUpdatedAction() {
        // GIVEN
        var modelToUpdate = AiModel.builder()
                .tenantId(tenantId)
                .version(1L)
                .name("Model to update")
                .configuration(constructValidOpenAiModelConfig())
                .build();
        modelToUpdate.setId(new AiModelId(UUID.randomUUID()));
        modelToUpdate.setCreatedTime(System.currentTimeMillis());

        var updatedModel = new AiModel(modelToUpdate);
        updatedModel.setVersion(2L);
        updatedModel.setName("Updated model");

        given(aiModelServiceMock.save(modelToUpdate)).willReturn(updatedModel);

        // WHEN
        AiModel result = service.save(modelToUpdate, user);

        // THEN
        assertThat(result).isEqualTo(updatedModel);

        then(aiModelServiceMock).should().save(modelToUpdate);
        then(vcServiceMock).should().autoCommit(user, updatedModel.getId());
        then(logEntityActionServiceMock).should().logEntityAction(tenantId, updatedModel.getId(), updatedModel, ActionType.UPDATED, user);
    }

    @Test
    void save_whenCreatingNewModelThrowsException_shouldUseEmptyIdAndLogError() {
        // GIVEN
        var modelToSave = AiModel.builder()
                .tenantId(tenantId)
                .name("Model to save")
                .configuration(constructValidOpenAiModelConfig())
                .build();

        var exception = new RuntimeException("Failed to save");

        given(aiModelServiceMock.save(modelToSave)).willThrow(exception);

        // WHEN-THEN
        assertThatThrownBy(() -> service.save(modelToSave, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save");

        then(aiModelServiceMock).should().save(modelToSave);
        then(vcServiceMock).should(never()).autoCommit(any(), any());
        then(logEntityActionServiceMock).should().logEntityAction(tenantId, new AiModelId(EntityId.NULL_UUID), modelToSave, ActionType.ADDED, user, exception);
    }

    @Test
    void save_whenUpdatingExistingModelThrowsException_shouldUseExistingModelIdAndLogError() {
        // GIVEN
        var modelToUpdate = AiModel.builder()
                .tenantId(tenantId)
                .version(1L)
                .name("Model to update")
                .configuration(constructValidOpenAiModelConfig())
                .build();
        modelToUpdate.setId(new AiModelId(UUID.randomUUID()));
        modelToUpdate.setCreatedTime(System.currentTimeMillis());

        var exception = new RuntimeException("Failed to save");

        given(aiModelServiceMock.save(modelToUpdate)).willThrow(exception);

        // WHEN-THEN
        assertThatThrownBy(() -> service.save(modelToUpdate, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save");

        then(aiModelServiceMock).should().save(modelToUpdate);
        then(vcServiceMock).should(never()).autoCommit(any(), any());
        then(logEntityActionServiceMock).should().logEntityAction(tenantId, modelToUpdate.getId(), modelToUpdate, ActionType.UPDATED, user, exception);
    }

    @Test
    void delete_whenDeleteSuccessful_shouldLogDeletedAction() {
        // GIVEN
        var modelToDelete = AiModel.builder()
                .tenantId(tenantId)
                .version(1L)
                .name("Model to delete")
                .configuration(constructValidOpenAiModelConfig())
                .build();
        modelToDelete.setId(new AiModelId(UUID.randomUUID()));
        modelToDelete.setCreatedTime(System.currentTimeMillis());

        given(aiModelServiceMock.deleteByTenantIdAndId(tenantId, modelToDelete.getId())).willReturn(true);

        // WHEN
        boolean result = service.delete(modelToDelete, user);

        // THEN
        assertThat(result).isTrue();
        then(aiModelServiceMock).should().deleteByTenantIdAndId(tenantId, modelToDelete.getId());
        then(logEntityActionServiceMock).should().logEntityAction(tenantId, modelToDelete.getId(), modelToDelete, ActionType.DELETED, user, modelToDelete.getId().toString());
    }

    @Test
    void delete_whenDeleteReturnsFalse_shouldNotLogAction() {
        // GIVEN
        var modelToDelete = AiModel.builder()
                .tenantId(tenantId)
                .version(1L)
                .name("Model to delete")
                .configuration(constructValidOpenAiModelConfig())
                .build();
        modelToDelete.setId(new AiModelId(UUID.randomUUID()));
        modelToDelete.setCreatedTime(System.currentTimeMillis());

        given(aiModelServiceMock.deleteByTenantIdAndId(tenantId, modelToDelete.getId())).willReturn(false);

        // WHEN
        boolean result = service.delete(modelToDelete, user);

        // THEN
        assertThat(result).isFalse();
        then(aiModelServiceMock).should().deleteByTenantIdAndId(tenantId, modelToDelete.getId());
        then(logEntityActionServiceMock).should(never()).logEntityAction(tenantId, modelToDelete.getId(), modelToDelete, ActionType.DELETED, user, modelToDelete.getId().toString());
    }

    @Test
    void delete_whenDeleteThrowsException_shouldLogError() {
        // GIVEN
        var modelToDelete = AiModel.builder()
                .tenantId(tenantId)
                .version(1L)
                .name("Model to delete")
                .configuration(constructValidOpenAiModelConfig())
                .build();
        modelToDelete.setId(new AiModelId(UUID.randomUUID()));
        modelToDelete.setCreatedTime(System.currentTimeMillis());

        var exception = new RuntimeException("Failed to delete");

        given(aiModelServiceMock.deleteByTenantIdAndId(tenantId, modelToDelete.getId())).willThrow(exception);

        // WHEN-THEN
        assertThatThrownBy(() -> service.delete(modelToDelete, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete");

        then(aiModelServiceMock).should().deleteByTenantIdAndId(tenantId, modelToDelete.getId());
        then(logEntityActionServiceMock).should().logEntityAction(tenantId, modelToDelete.getId(), modelToDelete, ActionType.DELETED, user, exception, modelToDelete.getId().toString());
    }

    private static AiModelConfig constructValidOpenAiModelConfig() {
        return OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder().apiKey("test-api-key").build())
                .modelId("gpt-4o")
                .temperature(0.5)
                .topP(0.3)
                .frequencyPenalty(0.1)
                .presencePenalty(0.2)
                .maxOutputTokens(1000)
                .timeoutSeconds(60)
                .maxRetries(2)
                .build();
    }

}
