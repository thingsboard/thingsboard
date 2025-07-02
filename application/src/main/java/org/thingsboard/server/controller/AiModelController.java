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

import com.google.common.util.concurrent.ListenableFuture;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.ai.dto.TbChatRequest;
import org.thingsboard.server.common.data.ai.dto.TbChatResponse;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModel;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.service.ai.AiModelService;

import java.time.Duration;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/model")
class AiModelController extends BaseController {

    private final AiModelService aiModelService;

    @ApiOperation(
            value = "Send request to AI chat model (sendChatRequest)",
            notes = "Submits a single prompt - made up of an optional system message and a required user message - to the specified AI chat model " +
                    "and returns either the generated answer or an error envelope." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/chat")
    public DeferredResult<TbChatResponse> sendChatRequest(@Valid @RequestBody TbChatRequest tbChatRequest) {
        ChatRequest langChainChatRequest = tbChatRequest.toLangChainChatRequest();
        AiChatModel<?> chatModel = tbChatRequest.chatModel();

        ListenableFuture<TbChatResponse> future = aiModelService.sendChatRequestAsync(chatModel, langChainChatRequest)
                .transform(chatResponse -> (TbChatResponse) new TbChatResponse.Success(chatResponse.aiMessage().text()), directExecutor())
                .catching(Throwable.class, ex -> new TbChatResponse.Failure(ex.getMessage()), directExecutor());

        Integer requestTimeoutSeconds = chatModel.modelConfig().timeoutSeconds();
        return requestTimeoutSeconds != null ? wrapFuture(future, Duration.ofSeconds(requestTimeoutSeconds).toMillis()) : wrapFuture(future);
    }

}
