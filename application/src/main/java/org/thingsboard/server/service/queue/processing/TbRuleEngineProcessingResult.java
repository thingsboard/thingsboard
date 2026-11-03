// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue.processing;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class TbRuleEngineProcessingResult {

    @Getter
    private final String queueName;
    @Getter
    private final boolean success;
    @Getter
    private final boolean timeout;
    @Getter
    private final TbMsgPackProcessingContext ctx;

    public TbRuleEngineProcessingResult(String queueName, boolean timeout, TbMsgPackProcessingContext ctx) {
        this.queueName = queueName;
        this.timeout = timeout;
        this.ctx = ctx;
        this.success = !timeout && ctx.getPendingMap().isEmpty() && ctx.getFailedMap().isEmpty();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getPendingMap() {
        return ctx.getPendingMap();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getSuccessMap() {
        return ctx.getSuccessMap();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getFailedMap() {
        return ctx.getFailedMap();
    }

    public ConcurrentMap<TenantId, RuleEngineException> getExceptionsMap() {
        return ctx.getExceptionsMap();
    }
}
