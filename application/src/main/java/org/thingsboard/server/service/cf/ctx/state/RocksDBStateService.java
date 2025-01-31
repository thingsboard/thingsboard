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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.service.cf.RocksDBService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldStateService;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${service.type:null}'=='monolith'")
public class RocksDBStateService implements CalculatedFieldStateService {

    private final RocksDBService rocksDBService;

    @Override
    public Map<CalculatedFieldEntityCtxId, CalculatedFieldState> restoreStates() {
        return rocksDBService.getAll().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> JacksonUtil.fromString(entry.getKey(), CalculatedFieldEntityCtxId.class),
                        entry -> JacksonUtil.fromString(entry.getValue(), CalculatedFieldState.class)
                ));
    }

    @Override
    public CalculatedFieldState restoreState(CalculatedFieldEntityCtxId ctxId) {
        return Optional.ofNullable(rocksDBService.get(JacksonUtil.writeValueAsString(ctxId)))
                .map(storedState -> JacksonUtil.fromString(storedState, CalculatedFieldState.class))
                .orElse(null);
    }

    @Override
    public void persistState(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback){
        rocksDBService.put(JacksonUtil.writeValueAsString(stateId), JacksonUtil.writeValueAsString(state));
        callback.onSuccess();
    }

    @Override
    public void removeState(CalculatedFieldEntityCtxId ctxId, TbCallback callback) {
        rocksDBService.delete(JacksonUtil.writeValueAsString(ctxId));
        callback.onSuccess();
    }

}
