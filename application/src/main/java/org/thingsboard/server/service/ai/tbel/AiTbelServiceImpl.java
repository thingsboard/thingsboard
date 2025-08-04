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
package org.thingsboard.server.service.ai.tbel;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.dto.AiTbelValidationStatus;
import org.thingsboard.server.common.data.ai.dto.TbChatRequest;
import org.thingsboard.server.common.data.ai.dto.TbContent;
import org.thingsboard.server.common.data.ai.dto.TbUserMessage;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.service.ai.AiChatModelService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.ai.dto.AiTbelValidationStatus.ERROR;
import static org.thingsboard.server.common.data.ai.dto.AiTbelValidationStatus.UNCHANGED;
import static org.thingsboard.server.common.data.ai.dto.TbChatResponse.Success;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_CHANGED;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_COMMIT;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_CREATED;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_EXAMPLES_COMMIT;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_EXAMPLES_CREATED;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_FORBIDDEN_CONSTRUCTS;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_MAIN_DIRECTIVE_VALIDATE;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_RULES;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_VALIDATE_PROCESS;
import static org.thingsboard.server.service.ai.tbel.AiTbelHelper.SYSTEM_PROMPT_TBEL_VALIDATE_STATE;

@Service
@RequiredArgsConstructor
public class AiTbelServiceImpl implements AiTbelService {

    private final AiChatModelService aiChatModelService;
    private final DefaultOpenAiTbelChatModelConfigFactory configFactory;
    private final AiTbelUserCommentCompressor compressor = new AiTbelUserCommentCompressor();

    @Override
    public JsonNode verifyScript(String script, String scriptType, String[] argNames,
                                 String msgType, String msgJson, String metadataJson) {

        try {
            AiChatModelConfig<?> config = configFactory.create();
            if (config == null) {
                return JacksonUtil.newObjectNode()
                        .put("status", AiTbelValidationStatus.ERROR.name())
                        .put("error", "Missing OpenAI API key. Please set OPENAI_API_TBEL_KEY");
            }

            String prettyMsg = JacksonUtil.toPrettyString(JacksonUtil.toJsonNode(msgJson));
            String prettyMetadata = JacksonUtil.toPrettyString(JacksonUtil.toJsonNode(metadataJson));



            String userPrompt = String.format("""
                For validation - checking for corrections on stylistic edits, parameters for input and metadata - not vikorystvoe.
                To verify your TBEL script, you need to use the following:
            
                Arguments:
                - scriptType: %s
                - argNames: %s
            
                Example input:
                msgType: %s
                msg (parsed):
                %s
            
                metadata:
                %s
                """, scriptType, List.of(argNames), msgType, prettyMsg, prettyMetadata);


            String scriptWithoutComments = compressor.compressScript(script);
            TbContent scriptContent = TbContent.ofScript(scriptWithoutComments);
            TbUserMessage userMessage = new TbUserMessage(List.of(
                    scriptContent
            ));

            TbChatRequest tbChatRequest = new TbChatRequest(
                    SYSTEM_PROMPT + "\n\n" +
                            SYSTEM_PROMPT_TBEL_RULES + "\n\n" +
                            SYSTEM_PROMPT_TBEL_COMMIT + "\n\n" +
                            SYSTEM_PROMPT_TBEL_EXAMPLES_COMMIT + "\n\n" +
                            SYSTEM_PROMPT_TBEL_VALIDATE_STATE + "\n\n" +
                            SYSTEM_PROMPT_TBEL_MAIN_DIRECTIVE_VALIDATE + "\n\n" +
                            SYSTEM_PROMPT_TBEL_VALIDATE_PROCESS + "\n\n" +
                            SYSTEM_PROMPT_TBEL_CREATED + "\n\n" +
                            SYSTEM_PROMPT_TBEL_EXAMPLES_CREATED + "\n\n" +
                            SYSTEM_PROMPT_TBEL_CHANGED + "\n\n" +
                            SYSTEM_PROMPT_TBEL_FORBIDDEN_CONSTRUCTS + "\n\n" +
                            userPrompt,
                    userMessage,
                    config
            );

            var chatRequest = tbChatRequest.toLangChainChatTbelRequest();

            var rawResponse = aiChatModelService
                    .sendChatRequestAsync(config, chatRequest)
                    .get(config.timeoutSeconds(), TimeUnit.SECONDS);

            var chatResponse = new Success(rawResponse.aiMessage().text());

            String responseBody = chatResponse.generatedContent();
            if (ObjectUtils.isEmpty(responseBody)) {
                return JacksonUtil.newObjectNode()
                        .put("status", AiTbelValidationStatus.ERROR.name())
                        .put("error", "Empty response from AI model");
            }

            JsonNode parsed;
            try {
                parsed = JacksonUtil.toJsonNode(responseBody);
            } catch (Exception e) {
                return JacksonUtil.newObjectNode()
                        .put("status", AiTbelValidationStatus.ERROR.name())
                        .put("error", "Invalid JSON returned by AI: " + e.getMessage());
            }

            String internalStatus = parsed.has("status") ? parsed.get("status").asText() : "error";
            String script_ai_new = parsed.has("script_ai") ? parsed.get("script_ai").toPrettyString() : "";
            String script_ai = compressor.decompressScript(script_ai_new);
            String report_ai = parsed.has("report_ai") ? parsed.get("report_ai").toPrettyString() : "";
            String script_original = parsed.has("script_original") ? parsed.get("script_original").asText() : "";

            if (UNCHANGED.name().equals(internalStatus.toUpperCase()) && !scriptWithoutComments.equals(script_original)) {
                internalStatus = ERROR.name();
                report_ai = "Failed parse script in Ai";
                script_ai = script;
            }

            try {
                internalStatus = AiTbelValidationStatus.valueOf(internalStatus.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                internalStatus = ERROR.name();
                report_ai =  "Unknown status in AI response: ";
            }
            return JacksonUtil.newObjectNode()
                    .put("status", internalStatus.toUpperCase())
                    .put("script", script_ai)
                    .put("report_ai", report_ai);
        } catch (Exception ex) {
            String message = extractUserFriendlyError(ex);
            return JacksonUtil.newObjectNode()
                    .put("status", AiTbelValidationStatus.ERROR.name())
                    .put("script", script)
                    .put("report_ai", message);
        }
    }

    private boolean scriptsEqual(String s1, String s2) {
        return normalize(s1).equals(normalize(s2));
    }

    private String normalize(String input) {
        return input.replaceAll("\\s+", "").trim();
    }

    private String extractUserFriendlyError(Throwable ex) {
        Throwable root = ExceptionUtils.getRootCause(ex);
        String msg = root != null ? root.getMessage() : ex.getMessage();

        if (ex instanceof dev.langchain4j.exception.UnresolvedModelServerException) {
            return "The OpenAI service is unreachable. Please check your internet connection.";
        }

        if (msg == null) {
            return "Unexpected error occurred.";
        }

        msg = msg.toLowerCase();

        if (msg.contains("insufficient_quota") || msg.contains("you exceeded your current quota")) {
            return "You have exceeded your OpenAI usage quota. Please check your plan and billing.";
        } else if (msg.contains("invalid_api_key") || msg.contains("incorrect api key")) {
            return "Invalid OpenAI API key. Please check OPENAI_API_TBEL_KEY.";
        } else if (msg.contains("401") || msg.contains("unauthorized")) {
            return "Unauthorized: Please verify your OpenAI credentials.";
        } else if (msg.contains("connect timed out") || msg.contains("read timed out") || msg.contains("timeout")) {
            return "Connection to OpenAI timed out. Please try again later.";
        } else if (msg.contains("connection refused") || msg.contains("network is unreachable") || msg.contains("failed to connect")) {
            return "OpenAI service is unreachable. Please check your internet connection.";
        } else if (msg.contains("rate limit reached")) {
            return "OpenAI service is unreachable. Rate limit reached for Ai.";
        } else if (msg.contains("unknownhostexception") || msg.contains("name or service not known") || msg.contains("temporary failure in name resolution")) {
            return "Unable to resolve OpenAI API host. Please verify your internet and DNS settings.";
        } else if (msg.contains("404")) {
            return "OpenAI API endpoint not found (404). Check baseUrl or endpoint.";
        }
        return "Unexpected error: " + msg;
    }
}
