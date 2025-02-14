/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.ctx.state;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldStateRestoreMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.exception.CalculatedFieldStateException;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.cf.CalculatedFieldStateService;
import org.thingsboard.server.service.cf.RocksDBService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;

import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.CalculatedFieldUtils.fromProto;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true) // Queue type in mem or Kafka;
public class RocksDBCalculatedFieldStateService implements CalculatedFieldStateService {

    private final ActorSystemContext actorSystemContext;
    private final RocksDBService rocksDBService;

    public Map<CalculatedFieldEntityCtxId, CalculatedFieldState> restoreStates() {
        return rocksDBService.getAll().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> fromProto(entry.getKey()),
                        entry -> fromProto(entry.getValue())
                ));
    }

    @AfterStartUp(order = AfterStartUp.CF_STATE_RESTORE_SERVICE)
    public void initCalculatedFieldStates() {
        restoreStates().forEach((k, v) -> actorSystemContext.tell(new CalculatedFieldStateRestoreMsg(k, v)));
    }

    @Override
    public void persistState(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) throws CalculatedFieldStateException {
        if (state.isStateTooLarge()) {
            throw new CalculatedFieldStateException("State size exceeds the maximum allowed limit. The state will not be persisted to RocksDB.");
        }
        rocksDBService.put(toProto(stateId), toProto(stateId, state));
        callback.onSuccess();
    }

    @Override
    public void removeState(CalculatedFieldEntityCtxId ctxId, TbCallback callback) {
        rocksDBService.delete(toProto(ctxId));
        callback.onSuccess();
    }

}
