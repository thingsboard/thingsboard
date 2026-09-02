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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.AiModel;
import org.thingsboard.client.model.OpenAiChatModelConfig;
import org.thingsboard.client.model.OpenAiProviderConfig;
import org.thingsboard.client.model.PageDataAiModel;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class AIModelApiClientTest extends AbstractApiClientTest {

    private static final String AI_PREFIX = "AiTest_";

    @Test
    public void testSaveAndGetAiModel() throws Exception {
        long ts = System.currentTimeMillis();
        String name = AI_PREFIX + "save_" + ts;

        AiModel model = buildAiModel(name, "gpt-4o", 0.7);
        AiModel saved = client.saveAiModel(model);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(name, saved.getName());
        assertNotNull(saved.getConfiguration());

        // get by id
        AiModel fetched = client.getAiModelById(saved.getId().getId());
        assertNotNull(fetched);
        assertEquals(name, fetched.getName());
        assertEquals(saved.getId().getId(), fetched.getId().getId());
    }

    @Test
    public void testGetAiModelById() throws Exception {
        long ts = System.currentTimeMillis();
        AiModel saved = createAiModel("getbyid_" + ts);

        AiModel fetched = client.getAiModelById(saved.getId().getId());
        assertNotNull(fetched);
        assertEquals(saved.getName(), fetched.getName());
        assertEquals(saved.getId().getId(), fetched.getId().getId());
    }

    @Test
    public void testUpdateAiModel() throws Exception {
        long ts = System.currentTimeMillis();
        AiModel saved = createAiModel("update_" + ts);

        saved.setName(AI_PREFIX + "updated_" + ts);
        OpenAiChatModelConfig updatedConfig = new OpenAiChatModelConfig();
        updatedConfig.setModelId("gpt-4o-mini");
        updatedConfig.setTemperature(0.3);
        updatedConfig.setMaxOutputTokens(2048);
        updatedConfig.setMaxRetries(50);
        OpenAiProviderConfig providerConfig = new OpenAiProviderConfig();
        providerConfig.setApiKey("test-api-key");
        providerConfig.setBaseUrl("https://api.openai.com/v1");
        updatedConfig.setProviderConfig(providerConfig);
        updatedConfig.setProvider("OPENAI");
        saved.setConfiguration(updatedConfig);

        AiModel updated = client.saveAiModel(saved);
        assertNotNull(updated);
        assertEquals(saved.getId().getId(), updated.getId().getId());
        assertEquals(AI_PREFIX + "updated_" + ts, updated.getName());
    }

    @Test
    public void testDeleteAiModel() throws Exception {
        long ts = System.currentTimeMillis();
        AiModel saved = createAiModel("delete_" + ts);

        UUID modelId = saved.getId().getId();
        client.getAiModelById(modelId);

        Boolean deleted = client.deleteAiModelById(modelId);
        assertTrue(deleted);

        assertReturns404(() -> client.getAiModelById(modelId));
    }

    @Test
    public void testGetAiModels() throws Exception {
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            createAiModel("list_" + ts + "_" + i);
        }

        PageDataAiModel page = client.getAiModels(100, 0, AI_PREFIX + "list_" + ts, null, null);
        assertNotNull(page);
        assertEquals(3, page.getTotalElements().intValue());
        for (AiModel m : page.getData()) {
            assertTrue(m.getName().startsWith(AI_PREFIX + "list_" + ts));
        }
    }

    @Test
    public void testGetAiModelById_notFound() {
        UUID nonExistentId = UUID.randomUUID();
        assertReturns404(() -> client.getAiModelById(nonExistentId));
    }

    @Test
    public void testGetAiModelsPagination() throws Exception {
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            createAiModel("paged_" + ts + "_" + i);
        }

        PageDataAiModel page1 = client.getAiModels(2, 0, AI_PREFIX + "paged_" + ts, null, null);
        assertNotNull(page1);
        assertEquals(5, page1.getTotalElements().intValue());
        assertEquals(3, page1.getTotalPages().intValue());
        assertEquals(2, page1.getData().size());
        assertTrue(page1.getHasNext());

        PageDataAiModel lastPage = client.getAiModels(2, 2, AI_PREFIX + "paged_" + ts, null, null);
        assertEquals(1, lastPage.getData().size());
        assertFalse(lastPage.getHasNext());
    }

    private AiModel buildAiModel(String name, String modelId, double temperature) {
        OpenAiChatModelConfig config = new OpenAiChatModelConfig();
        config.setModelId(modelId);
        config.setTemperature(temperature);
        config.setMaxRetries(50);
        OpenAiProviderConfig openAiProviderConfig = new OpenAiProviderConfig();
        openAiProviderConfig.setApiKey("test-api-key");
        openAiProviderConfig.setBaseUrl("https://api.openai.com/v1");
        config.setProviderConfig(openAiProviderConfig);
        config.setProvider("OPENAI");

        AiModel model = new AiModel();
        model.setName(name);
        model.setConfiguration(config);
        return model;
    }

    private AiModel createAiModel(String suffix) throws Exception {
        return client.saveAiModel(buildAiModel(AI_PREFIX + suffix, "gpt-4o", 0.7));
    }

}
