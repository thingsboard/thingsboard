/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "mock")
public class MockJsInvokeService implements JsInvokeService {

    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, ScriptType scriptType, String scriptBody, String... argNames) {
        log.warn("eval {} {} {} {}", tenantId, scriptType, scriptBody, argNames);
        return Futures.immediateFuture(UUID.randomUUID());
    }

    @Override
    public ListenableFuture<Object> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        log.warn("invokeFunction {} {} {} {}", tenantId, customerId, scriptId, args);
        return Futures.immediateFuture("{}");
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        log.warn("release {}", scriptId);
        return Futures.immediateFuture(null);
    }
}
