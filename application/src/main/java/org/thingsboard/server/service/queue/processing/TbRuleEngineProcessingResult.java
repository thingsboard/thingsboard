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
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class TbRuleEngineProcessingResult {

    @Getter
    private boolean success;
    @Getter
    private boolean timeout;
    @Getter
    private ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pendingMap;
    @Getter
    private ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> successMap;
    @Getter
    private ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failureMap;

    public TbRuleEngineProcessingResult(boolean timeout,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pendingMap,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> successMap,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failureMap) {
        this.timeout = timeout;
        this.pendingMap = pendingMap;
        this.successMap = successMap;
        this.failureMap = failureMap;
        this.success = !timeout && pendingMap.isEmpty() && failureMap.isEmpty();
    }
}
