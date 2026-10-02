// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiTokenProvider;
import org.thingsboard.server.service.ai.transport.TbAiClientRequestFactory;
import org.thingsboard.server.service.ai.transport.TbAiOperations;

import java.util.Optional;

@Component
@TbCoreComponent
public class AiUserDataDeletionTaskProcessor extends AiDataDeletionTaskProcessor {

    public AiUserDataDeletionTaskProcessor(Optional<TbAiTokenProvider> tokenProvider, TbAiOperations operations,
                                           TbAiClientRequestFactory clientRequestFactory) {
        super(tokenProvider, operations, clientRequestFactory);
    }

    @Override
    protected String operationType() {
        return ChannelProtocol.USER_DATA_DELETE;
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_AI_USER_DATA;
    }

}
