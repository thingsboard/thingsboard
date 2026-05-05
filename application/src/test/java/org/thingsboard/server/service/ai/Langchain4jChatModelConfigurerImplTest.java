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
package org.thingsboard.server.service.ai;

import com.google.cloud.vertexai.api.GenerationConfig;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OllamaChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AzureOpenAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OllamaProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ResourceLock("SsrfProtectionValidator")
class Langchain4jChatModelConfigurerImplTest {

    private static final String TEST_SERVICE_ACCOUNT_KEY = """
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "key-id",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNrHph/y7zyxIg\\ncmYYeOD8mFg9KraK71n84ffTQyVrl4HzlQgRIz5m4vM2rV5zjVLFi0xlAPT/iq/5\\nbh2zA4iXI0dEsR901nVjcL182t/GRYbKen53ZiuScBxBoCZPXW16Md+Yk8nMdNUb\\n4LoIRGZq64bjsJ+vh3Aa2gdGUHpDyebIXlXbF8ehWmEhgUsL7XjL0PkJ4lt2UMG+\\nx1j2Or25rqJmfc5M9kbxvINtdvSRTPiMOIXX00fCDZjQdd18RBVHOxraGxDgQpmv\\nk4qjFEPqGr0YTsa5dI8fz+4DqJpEi3rancRiTKM/KUwYLGnPSD07XqGfiDA8npVm\\nj4N62LhnAgMBAAECggEADbFfH87DRk7YQO8XgKdCOf7oglX+0NwjjmmlkVvwjgEI\\nZqT0ObPcz9u/MSjfV2vAEs/LK773ELD1NfLQqQjiBfHpfkIZOTLynhwKOYRBjqvf\\n+p38ynUzucGbV/vSC8meuW/AQPe3Nn9MFYQ4znEYrSNLbTWRRA3idvSEtHfffqDA\\nDHRBI1eIlxh1OTIR3L+HhcNYuus1LuoKnSlmwLGhAZLt7fjuWK3PkOiFT15e0M9M\\nUhp3WwhHcRC0o6bxT+BWRYKMVX3Vjlro4sF1fq4+jePThX1bpJcPfUmsC95tXPfX\\njfNGAxHlZ+MS1V/cLlIqyz7drXBcwCDJtbPmvavmNQKBgQD1ZR/ePHcjXUGM57U8\\nbxPatNOrcicvaP2AtTA6Y/JjfbcydkVXsenDGk0h6hykpIiMwrAaJuUTjGeM6QTI\\nOhK0k1QbGitcM71d9TSdLzWUdb3yvsyaPZlPR/6u2FBb6Bf9rWOQRYYyv1Lvu0+1\\nYLnR4sHBxiAur1NGxuHfA4ZUOwKBgQDWj+fcS/x/ifbCqexr7teWU+tUeyG3eLGA\\nMmB9eCkY7djl0/LHu/IHgqrGRVgra3IB1uI7Wr3jZYvlS7qGL3KpjeIPYj7LTQC5\\nznm0875NvJELPjQK/A4EM3mC057QRvb7y52KBNKJi7/JwHU7VHmudB78e7uGlW2K\\n5Ccl0PJFxQKBgCWv5yoJXT64JsYOG95xLLptBQkSmgQE+tHWgdal3Ob8urLsSRAD\\nyePl2Sy5OLbscfA0Qjlx+cJ70LdqXgqmKJNFASi8ZyZc59tTOkZdprvrLUXnmaKi\\njTYI14tgu06yIWUbSOwyUT7f9UvOF5rChSc/zQQGepDQ6lg3WR8X+nxbAoGBAKiu\\nfAcqSfjuuuuxcWgtXpoVoaZKI2i9Xza85DTf+ddabjHJXk3+iTm0VZQIwldoYjnl\\n+PfW0ABtPf1net2xgcChBf84Ksvj3tU06WQEWDF/NLyVC48zN8W/viDHREzT7app\\nGpJ+VhLCpmXzg3bAY+Vt70pp8DTPV05hLhHB4iZNAoGBAJW+bYh7jE61+58VpjvF\\nP36BK09jEEPWVucJdghb2mb62iA6JDy3ApU+8FzckXHDewt0sqvsW4VqukgwVZx3\\npSC7mR4B+Fm6znm0Z5mBWiG5bOOgTJ0mRZv4cYgC+JRRF/E3yYR58RyAKFAFIAFH\\nng8XYP1wQp64Fzv4+rUSwM49\\n-----END PRIVATE KEY-----\\n",
              "client_email": "test@test-project.iam.gserviceaccount.com",
              "client_id": "123456789",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """;

    private final Langchain4jChatModelConfigurerImpl configurer = new Langchain4jChatModelConfigurerImpl();

    @BeforeEach
    void enableSsrfProtection() {
        SsrfProtectionValidator.setEnabled(true);
    }

    @AfterEach
    void disableSsrfProtection() {
        SsrfProtectionValidator.setEnabled(false);
    }

    @Test
    void configureChatModel_openAi_withPrivateIp_shouldThrow() {
        var config = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl("http://172.17.0.1:8080/")
                        .apiKey("test")
                        .build())
                .modelId("gpt-4o")
                .build();

        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void configureChatModel_openAi_withLocalhostUrl_shouldThrow() {
        var config = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl("http://localhost:22/")
                        .apiKey("test")
                        .build())
                .modelId("gpt-4o")
                .build();

        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void configureChatModel_azureOpenAi_withPrivateIp_shouldThrow() {
        var config = AzureOpenAiChatModelConfig.builder()
                .providerConfig(new AzureOpenAiProviderConfig(
                        "http://10.0.0.1:8080/", null, "test-key"))
                .modelId("gpt-4o")
                .build();

        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void configureChatModel_ollama_withPrivateIp_shouldThrow() {
        var config = OllamaChatModelConfig.builder()
                .providerConfig(new OllamaProviderConfig(
                        "http://192.168.1.100:11434/", new OllamaProviderConfig.OllamaAuth.None()))
                .modelId("llama3")
                .build();

        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void configureChatModel_vertexAi_setsFrequencyAndPresencePenaltyFromCorrectConfigFields() {
        // GIVEN
        var providerConfig = new GoogleVertexAiGeminiProviderConfig(
                "test.json", "test-project", "us-central1", TEST_SERVICE_ACCOUNT_KEY
        );
        var chatModelConfig = GoogleVertexAiGeminiChatModelConfig.builder()
                .providerConfig(providerConfig)
                .modelId("gemini-2.0-flash")
                .frequencyPenalty(0.3)
                .presencePenalty(0.7)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(chatModelConfig);

        // THEN
        var generationConfig = (GenerationConfig) ReflectionTestUtils.getField(chatModel, "generationConfig");
        assertThat(generationConfig.getFrequencyPenalty()).isEqualTo(0.3f);
        assertThat(generationConfig.getPresencePenalty()).isEqualTo(0.7f);
    }

}
