// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ruleengine;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.function.Consumer;

public interface RuleEngineCallService {

    void processRestApiCallToRuleEngine(TenantId tenantId, UUID requestId, TbMsg request, boolean useQueueFromTbMsg, Consumer<TbMsg> responseConsumer);

    void onQueueMsg(TransportProtos.RestApiCallResponseMsgProto restApiCallResponseMsg, TbCallback callback);
}
