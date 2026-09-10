// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
