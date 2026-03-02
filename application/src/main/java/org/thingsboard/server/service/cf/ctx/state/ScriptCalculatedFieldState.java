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
package org.thingsboard.server.service.cf.ctx.state;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;

import java.util.Map;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ScriptCalculatedFieldState extends BaseCalculatedFieldState {

    protected CalculatedFieldScriptEngine tbelExpression;

    public ScriptCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        this.tbelExpression = ctx.getTbelExpressions().get(ctx.getExpression());
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        ListenableFuture<Object> resultFuture = ctx.evaluateTbelExpression(tbelExpression, this);
        Output output = ctx.getOutput();
        return Futures.transform(resultFuture,
                result -> TelemetryCalculatedFieldResult.builder()
                        .outputStrategy(output.getStrategy())
                        .type(output.getType())
                        .scope(output.getScope())
                        .result(JacksonUtil.valueToTree(result))
                        .build(),
                MoreExecutors.directExecutor()
        );
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }

}
