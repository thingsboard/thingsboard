/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.processing;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class TbRuleEngineProcessingResult {

    @Getter
    private final boolean success;
    @Getter
    private final boolean timeout;
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pendingMap;
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> successMap;
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failureMap;
    @Getter
    private final ConcurrentMap<TenantId, RuleEngineException> exceptionsMap;

    public TbRuleEngineProcessingResult(boolean timeout,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pendingMap,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> successMap,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failureMap,
                                        ConcurrentMap<TenantId, RuleEngineException> exceptionsMap) {
        this.timeout = timeout;
        this.pendingMap = pendingMap;
        this.successMap = successMap;
        this.failureMap = failureMap;
        this.exceptionsMap = exceptionsMap;
        this.success = !timeout && pendingMap.isEmpty() && failureMap.isEmpty();
    }
}
