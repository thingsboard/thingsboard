// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
