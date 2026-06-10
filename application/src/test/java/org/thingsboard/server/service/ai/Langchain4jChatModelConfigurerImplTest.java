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

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OllamaChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AmazonBedrockProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AzureOpenAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GitHubModelsProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.MistralAiProviderConfig;
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

    @AfterEach
    void resetSsrfProtection() {
        SsrfProtectionValidator.setEnabled(false);
    }

    // ============================== Configuration correctness (one per provider) ==============================
    // For each provider we feed a fully populated config and assert that the returned ChatModel carries the same
    // values, using only the public ChatModel surface (provider() and defaultRequestParameters()) — no reflection.

    @Test
    void shouldConfigureOpenAiModel_whenGivenOpenAiConfig() {
        // GIVEN
        var config = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl("https://api.openai.com/v1")
                        .apiKey("test-key")
                        .build())
                .modelId("gpt-4o")
                .temperature(0.7)
                .topP(0.9)
                .frequencyPenalty(0.5)
                .presencePenalty(0.25)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.OPEN_AI);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("gpt-4o");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.frequencyPenalty()).isEqualTo(0.5);
        assertThat(params.presencePenalty()).isEqualTo(0.25);
        assertThat(params.maxOutputTokens()).isEqualTo(500);
    }

    @Test
    void shouldConfigureAzureOpenAiModel_whenGivenAzureOpenAiConfig() {
        // GIVEN
        var config = AzureOpenAiChatModelConfig.builder()
                .providerConfig(new AzureOpenAiProviderConfig(
                        "https://my-resource.openai.azure.com/", "2024-05-01-preview", "test-key"))
                .modelId("gpt-4o")
                .temperature(0.7)
                .topP(0.9)
                .frequencyPenalty(0.5)
                .presencePenalty(0.25)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.AZURE_OPEN_AI);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("gpt-4o"); // deployment name maps to modelName
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.frequencyPenalty()).isEqualTo(0.5);
        assertThat(params.presencePenalty()).isEqualTo(0.25);
        assertThat(params.maxOutputTokens()).isEqualTo(500);
    }

    @Test
    void shouldConfigureGoogleAiGeminiModel_whenGivenGoogleAiGeminiConfig() {
        // GIVEN
        var config = GoogleAiGeminiChatModelConfig.builder()
                .providerConfig(new GoogleAiGeminiProviderConfig("test-key"))
                .modelId("gemini-2.5-flash")
                .temperature(0.7)
                .topP(0.9)
                .topK(40)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.GOOGLE_GENAI);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("gemini-2.5-flash");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.topK()).isEqualTo(40);
        assertThat(params.maxOutputTokens()).isEqualTo(500);
    }

    @Test
    void shouldConfigureGoogleVertexAiGeminiModel_whenGivenGoogleVertexAiGeminiConfig() {
        // GIVEN
        var config = GoogleVertexAiGeminiChatModelConfig.builder()
                .providerConfig(new GoogleVertexAiGeminiProviderConfig(
                        "key.json", "test-project", "us-central1", TEST_SERVICE_ACCOUNT_KEY))
                .modelId("gemini-2.5-flash")
                .temperature(0.7)
                .topP(0.9)
                .topK(40)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.GOOGLE_GENAI);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("gemini-2.5-flash");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.topK()).isEqualTo(40);
        assertThat(params.maxOutputTokens()).isEqualTo(500);
    }

    @Test
    void shouldConfigureMistralAiModel_whenGivenMistralAiConfig() {
        // GIVEN
        var config = MistralAiChatModelConfig.builder()
                .providerConfig(new MistralAiProviderConfig("test-key"))
                .modelId("mistral-large-latest")
                .temperature(0.7)
                .topP(0.9)
                .frequencyPenalty(0.5)
                .presencePenalty(0.25)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.MISTRAL_AI);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("mistral-large-latest");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.frequencyPenalty()).isEqualTo(0.5);
        assertThat(params.presencePenalty()).isEqualTo(0.25);
        assertThat(params.maxOutputTokens()).isEqualTo(500);
    }

    @Test
    void shouldConfigureAnthropicModel_whenGivenAnthropicConfig() {
        // GIVEN
        var config = AnthropicChatModelConfig.builder()
                .providerConfig(new AnthropicProviderConfig("test-key"))
                .modelId("claude-opus-4-8")
                .temperature(0.7)
                .topP(0.9)
                .topK(40)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.ANTHROPIC);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("claude-opus-4-8");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.topK()).isEqualTo(40);
        assertThat(params.maxOutputTokens()).isEqualTo(500);
    }

    @Test
    void shouldConfigureAmazonBedrockModel_whenGivenAmazonBedrockConfig() {
        // GIVEN
        var config = AmazonBedrockChatModelConfig.builder()
                .providerConfig(new AmazonBedrockProviderConfig(
                        "us-east-1", "test-access-key-id", "test-secret-access-key"))
                .modelId("anthropic.claude-3-5-sonnet-20240620-v1:0")
                .temperature(0.7)
                .topP(0.9)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.AMAZON_BEDROCK);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("anthropic.claude-3-5-sonnet-20240620-v1:0");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.maxOutputTokens()).isEqualTo(500);
    }

    @Test
    void shouldConfigureGitHubModelsModel_whenGivenGitHubModelsConfig() {
        // GIVEN
        var config = GitHubModelsChatModelConfig.builder()
                .providerConfig(new GitHubModelsProviderConfig("ghp-test-token"))
                .modelId("gpt-4o")
                .temperature(0.7)
                .topP(0.9)
                .frequencyPenalty(0.5)
                .presencePenalty(0.25)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.GITHUB_MODELS);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("gpt-4o");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.frequencyPenalty()).isEqualTo(0.5);
        assertThat(params.presencePenalty()).isEqualTo(0.25);
        assertThat(params.maxOutputTokens()).isEqualTo(500); // maxCompletionTokens maps to maxOutputTokens
    }

    @Test
    void shouldConfigureOllamaModel_whenGivenOllamaConfig() {
        // GIVEN
        var config = OllamaChatModelConfig.builder()
                .providerConfig(new OllamaProviderConfig(
                        "http://localhost:11434", new OllamaProviderConfig.OllamaAuth.None()))
                .modelId("llama3")
                .temperature(0.7)
                .topP(0.9)
                .topK(40)
                .contextLength(4096)
                .maxOutputTokens(500)
                .timeoutSeconds(60)
                .maxRetries(3)
                .build();

        // WHEN
        ChatModel chatModel = configurer.configureChatModel(config);

        // THEN
        assertThat(chatModel.provider()).isEqualTo(ModelProvider.OLLAMA);
        ChatRequestParameters params = chatModel.defaultRequestParameters();
        assertThat(params.modelName()).isEqualTo("llama3");
        assertThat(params.temperature()).isEqualTo(0.7);
        assertThat(params.topP()).isEqualTo(0.9);
        assertThat(params.topK()).isEqualTo(40);
        assertThat(params.maxOutputTokens()).isEqualTo(500); // numPredict maps to maxOutputTokens
    }

    // ============================== Base URL SSRF validation ==============================
    // Providers that accept a user-supplied base URL must reject hosts that resolve to private/loopback addresses
    // when SSRF protection is enabled.

    @Test
    void shouldThrow_whenOpenAiBaseUrlIsPrivateIp() {
        // GIVEN
        SsrfProtectionValidator.setEnabled(true);
        var config = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl("http://172.17.0.1:8080/")
                        .apiKey("test")
                        .build())
                .modelId("gpt-4o")
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void shouldThrow_whenOpenAiBaseUrlIsLocalhost() {
        // GIVEN
        SsrfProtectionValidator.setEnabled(true);
        var config = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl("http://localhost:22/")
                        .apiKey("test")
                        .build())
                .modelId("gpt-4o")
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void shouldThrow_whenAzureOpenAiEndpointIsPrivateIp() {
        // GIVEN
        SsrfProtectionValidator.setEnabled(true);
        var config = AzureOpenAiChatModelConfig.builder()
                .providerConfig(new AzureOpenAiProviderConfig(
                        "http://10.0.0.1:8080/", null, "test-key"))
                .modelId("gpt-4o")
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void shouldThrow_whenOllamaBaseUrlIsPrivateIp() {
        // GIVEN
        SsrfProtectionValidator.setEnabled(true);
        var config = OllamaChatModelConfig.builder()
                .providerConfig(new OllamaProviderConfig(
                        "http://192.168.1.100:11434/", new OllamaProviderConfig.OllamaAuth.None()))
                .modelId("llama3")
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> configurer.configureChatModel(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

}
