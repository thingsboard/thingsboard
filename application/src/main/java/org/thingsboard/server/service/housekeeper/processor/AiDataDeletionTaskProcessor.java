// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.ai.TbAiTokenProvider;
import org.thingsboard.server.service.ai.TbAiTokenProviderFactory;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AiDataDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    protected final TbAiClient tbAiClient;
    private final Optional<TbAiTokenProvider> tokenProvider;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        TbAiTokenProvider tokenProvider = this.tokenProvider.orElseThrow(() ->
                new IllegalStateException("AI token provider is not configured"));
        if (!tokenProvider.isTokenAvailable()) {
            log.debug("Skipping {}: AI token is not available", task.getDescription());
            return;
        }
        UserId userId = new UserId(task.getEntityId().getId());
        TbAiResponse response = deleteData(TbAiTokenProviderFactory.idScoped(tokenProvider, task.getTenantId(), userId));
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getError());
        }
    }

    protected abstract TbAiResponse deleteData(TbAiClient.TokenProvider tokenProvider);

}
