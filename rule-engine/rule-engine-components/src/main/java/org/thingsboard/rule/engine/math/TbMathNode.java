/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.math;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "math formula",
        configClazz = TbMathFormulaConfiguration.class,
        nodeDescription = "Calculates the mathematics formula based on message and/or database values",
        nodeDetails = "Transform incoming Message with configured JS function to String and log final value into Thingsboard log file. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        icon = "functions"
)
public class TbMathNode implements TbNode {

    private static ConcurrentMap<EntityId, Semaphore> semaphores = new ConcurrentReferenceHashMap<>();

    private TbMathFormulaConfiguration config;
    private boolean msgBodyToJsonConversionRequired;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMathFormulaConfiguration.class);
        var operation = config.getOperation();
        var argsCount = config.getArguments().size();
        if (argsCount < operation.getMinArgs() || argsCount > operation.getMaxArgs()) {
            throw new RuntimeException("Args count: " + argsCount + " does not match operation: " + operation.name());
        }
        msgBodyToJsonConversionRequired = config.getArguments().stream().anyMatch(arg -> TbMathArgumentType.MESSAGE_BODY.equals(arg.getType()));
        msgBodyToJsonConversionRequired = msgBodyToJsonConversionRequired || TbMathArgumentType.MESSAGE_BODY.equals(config.getResult().getType());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var originator = msg.getOriginator();
        var originatorSemaphore = semaphores.computeIfAbsent(originator, tmp -> new Semaphore(1, true));

        var arguments = config.getArguments();
        Optional<ObjectNode> msgBodyOpt = convertMsgBodyIfRequired(msg);
        var argumentValues = Futures.allAsList(arguments.stream()
                .map(arg -> resolveArguments(ctx, msg, msgBodyOpt, arg)).collect(Collectors.toList()));
        ListenableFuture<TbMsg> resultMsgFuture = Futures.transformAsync(argumentValues, args ->
                updateMsgAndDb(ctx, msg, msgBodyOpt, calculateResult(ctx, msg, args)), ctx.getDbCallbackExecutor());
        DonAsynchron.withCallback(resultMsgFuture, ctx::tellSuccess, t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<TbMsg> updateMsgAndDb(TbContext ctx, TbMsg msg, Optional<ObjectNode> msgBodyOpt, double result) {
        TbMathResult mathResultDef = config.getResult();
        switch (mathResultDef.getType()) {
            case MESSAGE_BODY:
                return Futures.immediateFuture(addToBody(msg, mathResultDef, msgBodyOpt, result));
            case MESSAGE_METADATA:
                return Futures.immediateFuture(addToMeta(msg, mathResultDef, result));
            case ATTRIBUTE:
                ListenableFuture<Void> attrSave = ctx.getTelemetryService().saveAttrAndNotify(
                        ctx.getTenantId(), msg.getOriginator(), getAttributeScope(mathResultDef.getAttributeScope()), mathResultDef.getValue(), result);
                return Futures.transform(attrSave, attr -> addToBodyAndMeta(msg, msgBodyOpt, result, mathResultDef), ctx.getDbCallbackExecutor());
            case TIME_SERIES:
                ListenableFuture<Void> tsSave = ctx.getTelemetryService().saveAndNotify(ctx.getTenantId(), msg.getOriginator(),
                        new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry(mathResultDef.getValue(), result)));
                return Futures.transform(tsSave, ts -> addToBodyAndMeta(msg, msgBodyOpt, result, mathResultDef), ctx.getDbCallbackExecutor());
            default:
                throw new RuntimeException("Result type is not supported: " + mathResultDef.getType() + "!");
        }
    }

    private Optional<ObjectNode> convertMsgBodyIfRequired(TbMsg msg) {
        Optional<ObjectNode> msgBodyOpt;
        if (msgBodyToJsonConversionRequired) {
            var jsonNode = JacksonUtil.toJsonNode(msg.getData());
            if (jsonNode.isObject()) {
                msgBodyOpt = Optional.of((ObjectNode) jsonNode);
            } else {
                throw new RuntimeException("Message body is not a JSON object!");
            }
        } else {
            msgBodyOpt = Optional.empty();
        }
        return msgBodyOpt;
    }

    private TbMsg addToBodyAndMeta(TbMsg msg, Optional<ObjectNode> msgBodyOpt, double result, TbMathResult mathResultDef) {
        TbMsg tmpMsg = msg;
        if (mathResultDef.isAddToBody()) {
            tmpMsg = addToBody(msg, mathResultDef, msgBodyOpt, result);
        }
        if (mathResultDef.isAddToMetadata()) {
            tmpMsg = addToMeta(msg, mathResultDef, result);
        }
        return tmpMsg;
    }

    private TbMsg addToBody(TbMsg msg, TbMathResult mathResultDef, Optional<ObjectNode> msgBodyOpt, double result) {
        ObjectNode body = msgBodyOpt.get();
        body.put(mathResultDef.getValue(), result);
        return TbMsg.transformMsgData(msg, JacksonUtil.toString(body));
    }

    private TbMsg addToMeta(TbMsg msg, TbMathResult mathResultDef, double result) {
        var md = msg.getMetaData();
        md.putValue(mathResultDef.getValue(), Double.toString(result));
        return TbMsg.transformMsg(msg, md);
    }

    private double calculateResult(TbContext ctx, TbMsg msg, List<TbMathArgumentValue> args) {
        switch (config.getOperation()) {
            case ADD:
                return apply(args.get(0), args.get(1), Double::sum);
            case SUB:
                return apply(args.get(0), args.get(1), (a, b) -> a - b);
            case MULT:
                return apply(args.get(0), args.get(1), (a, b) -> a * b);
            case DIV:
                return apply(args.get(0), args.get(1), (a, b) -> a / b);
            case SIN:
                return apply(args.get(0), Math::sin);
            case COS:
                return apply(args.get(0), Math::cos);
            case SQRT:
                return apply(args.get(0), Math::sqrt);
            case ABS:
                return apply(args.get(0), Math::abs);
            default:
                throw new RuntimeException("Not supported operation: " + config.getOperation());
        }
    }

    private double apply(TbMathArgumentValue arg, Function<Double, Double> function) {
        return function.apply(arg.getValue());
    }

    private double apply(TbMathArgumentValue arg1, TbMathArgumentValue arg2, BiFunction<Double, Double, Double> function) {
        return function.apply(arg1.getValue(), arg2.getValue());
    }

    private ListenableFuture<TbMathArgumentValue> resolveArguments(TbContext ctx, TbMsg msg, Optional<ObjectNode> msgBodyOpt, TbMathArgument arg) {
        switch (arg.getType()) {
            case CONSTANT:
                return Futures.immediateFuture(TbMathArgumentValue.constant(arg));
            case MESSAGE_BODY:
                return Futures.immediateFuture(TbMathArgumentValue.fromMessageBody(arg.getValue(), msgBodyOpt));
            case MESSAGE_METADATA:
                return Futures.immediateFuture(TbMathArgumentValue.fromMessageMetadata(arg.getValue(), msg.getMetaData()));
            case ATTRIBUTE:
                String scope = getAttributeScope(arg.getAttributeScope());
                return Futures.transform(ctx.getAttributesService().find(ctx.getTenantId(), msg.getOriginator(), scope, arg.getValue()),
                        opt -> getTbMathArgumentValue(opt.orElseThrow(() ->
                                new RuntimeException("Attribute: " + arg.getValue() + " with scope: " + scope + " not found for entity: " + msg.getOriginator())))
                        , MoreExecutors.directExecutor());
            case TIME_SERIES:
                return Futures.transform(ctx.getTimeseriesService().findLatest(ctx.getTenantId(), msg.getOriginator(), arg.getValue()),
                        opt -> getTbMathArgumentValue(opt.orElseThrow(() ->
                                new RuntimeException("Time-series: " + arg.getValue() + " not found for entity: " + msg.getOriginator())))
                        , MoreExecutors.directExecutor());
            default:
                throw new RuntimeException("Unsupported argument type: " + arg.getType() + "!");
        }

    }

    private String getAttributeScope(String attrScope) {
        return StringUtils.isEmpty(attrScope) ? DataConstants.SERVER_SCOPE : attrScope;
    }

    private TbMathArgumentValue getTbMathArgumentValue(KvEntry kv) {
        switch (kv.getDataType()) {
            case LONG:
                return TbMathArgumentValue.fromLong(kv.getLongValue().get());
            case DOUBLE:
                return TbMathArgumentValue.fromDouble(kv.getDoubleValue().get());
            default:
                return TbMathArgumentValue.fromString(kv.getValueAsString());
        }
    }

    @Override
    public void destroy() {
    }
}
