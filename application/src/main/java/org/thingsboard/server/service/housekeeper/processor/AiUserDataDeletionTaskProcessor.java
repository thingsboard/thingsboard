// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiTokenProvider;

import java.util.Optional;

@Component
@TbCoreComponent
public class AiUserDataDeletionTaskProcessor extends AiDataDeletionTaskProcessor {

    public AiUserDataDeletionTaskProcessor(TbAiClient tbAiClient, Optional<TbAiTokenProvider> tokenProvider) {
        super(tbAiClient, tokenProvider);
    }

    @Override
    protected TbAiResponse deleteData(TbAiClient.TokenProvider tokenProvider) {
        return tbAiClient.deleteUserData(tokenProvider);
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_AI_USER_DATA;
    }

}
