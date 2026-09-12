// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue;

import org.springframework.stereotype.Component;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

public interface TbMsgPackProcessingContextFactory {

    TbMsgPackProcessingContext create(String queueName, TbRuleEngineSubmitStrategy submitStrategy, boolean skipTimeouts);

    @Component
    class DefaultTbMsgPackProcessingContextFactory implements TbMsgPackProcessingContextFactory {

        @Override
        public TbMsgPackProcessingContext create(String queueName, TbRuleEngineSubmitStrategy submitStrategy, boolean skipTimeouts) {
            return new TbMsgPackProcessingContext(queueName, submitStrategy, skipTimeouts);
        }

    }

}
