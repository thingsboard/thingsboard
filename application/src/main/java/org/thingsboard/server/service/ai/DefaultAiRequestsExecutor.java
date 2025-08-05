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
package org.thingsboard.server.service.ai;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.time.Duration;
import java.util.concurrent.Executors;

@Lazy
@Component
@RequiredArgsConstructor
class DefaultAiRequestsExecutor implements AiRequestsExecutor {

    private final AiRequestsExecutorProperties properties;

    @Data
    @Validated
    @Configuration
    @ConfigurationProperties(prefix = "actors.rule.ai-requests-thread-pool")
    private static class AiRequestsExecutorProperties {

        @NotBlank(message = "Pool name must be not blank")
        private String poolName = "ai-requests";

        @Min(value = 1, message = "Pool size must be at least 1")
        private int poolSize = 50;

        @Min(value = 1, message = "Termination timeout must be at least 1 second")
        private int terminationTimeoutSeconds = 60;

    }

    private ListeningExecutorService executorService;

    @PostConstruct
    private void init() {
        executorService = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(properties.getPoolSize(), ThingsBoardThreadFactory.forName(properties.getPoolName()))
        );
    }

    @Override
    public FluentFuture<ChatResponse> sendChatRequestAsync(ChatModel chatModel, ChatRequest chatRequest) {
        return FluentFuture.from(executorService.submit(() -> chatModel.chat(chatRequest)));
    }

    @PreDestroy
    private void destroy() {
        if (executorService != null) {
            MoreExecutors.shutdownAndAwaitTermination(executorService, Duration.ofSeconds(properties.getTerminationTimeoutSeconds()));
            executorService = null;
        }
    }

}
