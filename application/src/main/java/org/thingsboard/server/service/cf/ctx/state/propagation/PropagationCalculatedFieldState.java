/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.ctx.state.propagation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.PropagationCalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;

import java.util.Map;

import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;

public class PropagationCalculatedFieldState extends ScriptCalculatedFieldState {

    public PropagationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.PROPAGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        ArgumentEntry argumentEntry = arguments.get(PROPAGATION_CONFIG_ARGUMENT);
        if (!(argumentEntry instanceof PropagationArgumentEntry propagationArgumentEntry) || propagationArgumentEntry.isEmpty()) {
            return Futures.immediateFuture(PropagationCalculatedFieldResult.builder().build());
        }
        return Futures.transform(super.performCalculation(updatedArgs, ctx), telemetryCfResult ->
                        PropagationCalculatedFieldResult.builder()
                                .propagationEntityIds(propagationArgumentEntry.getPropagationEntityIds())
                                .result((TelemetryCalculatedFieldResult) telemetryCfResult)
                                .build(),
                MoreExecutors.directExecutor());
    }

}
