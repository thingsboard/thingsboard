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
package org.thingsboard.server.service.cf;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldStateRestoreMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.exception.CalculatedFieldStateException;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;

import static org.thingsboard.server.utils.CalculatedFieldUtils.fromProto;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

public abstract class AbstractCalculatedFieldStateService implements CalculatedFieldStateService {

    @Autowired
    private ActorSystemContext actorSystemContext;

    @Override
    public final void persistState(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) {
        if (state.isStateTooLarge()) {
            throw new CalculatedFieldStateException("State size exceeds the maximum allowed limit. The state will not be persisted to RocksDB.");
        }
        doPersist(stateId, toProto(stateId, state), callback);
    }

    protected abstract void doPersist(CalculatedFieldEntityCtxId stateId, CalculatedFieldStateProto stateMsgProto, TbCallback callback);

    @Override
    public final void removeState(CalculatedFieldEntityCtxId stateId, TbCallback callback) {
        doRemove(stateId, callback);
    }

    protected abstract void doRemove(CalculatedFieldEntityCtxId stateId, TbCallback callback);

    protected void processRestoredState(CalculatedFieldStateProto stateMsg) {
        var id = fromProto(stateMsg.getId());
        var state = fromProto(stateMsg);
        processRestoredState(id, state);
    }

    protected void processRestoredState(CalculatedFieldEntityCtxId id, CalculatedFieldState state) {
        actorSystemContext.tell(new CalculatedFieldStateRestoreMsg(id, state));
    }

}
