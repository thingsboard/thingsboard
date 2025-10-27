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
package org.thingsboard.server.service.cf.ctx.state;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import org.mvel2.MVEL;
import org.thingsboard.common.util.ExpressionUtils;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfCtx;
import org.thingsboard.script.api.tbel.TbelCfSingleValueArg;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldReevaluateMsg;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ExpressionBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class CalculatedFieldCtx implements Closeable {

    private CalculatedField calculatedField;

    private CalculatedFieldId cfId;
    private TenantId tenantId;
    private EntityId entityId;
    private CalculatedFieldType cfType;
    private final Map<String, Argument> arguments;
    private final Map<ReferencedEntityKey, String> mainEntityArguments;
    private final Map<EntityId, Map<ReferencedEntityKey, String>> linkedEntityArguments;
    private final Map<ReferencedEntityKey, String> dynamicEntityArguments;
    private final List<String> argNames;
    private Output output;
    private String expression;
    private boolean useLatestTs;
    private boolean requiresScheduledReevaluation;

    private ActorSystemContext systemContext;
    private TbelInvokeService tbelInvokeService;
    private RelationService relationService;
    private AlarmSubscriptionService alarmService;

    private Map<String, CalculatedFieldScriptEngine> tbelExpressions;
    private Map<String, ThreadLocal<Expression>> simpleExpressions;

    private boolean initialized;

    private long maxDataPointsPerRollingArg;
    private long maxStateSize;
    private long maxSingleValueArgumentSize;

    private boolean relationQueryDynamicArguments;
    private List<String> mainEntityGeofencingArgumentNames;
    private List<String> linkedEntityAndCurrentOwnerGeofencingArgumentNames;

    private long scheduledUpdateIntervalMillis;

    private Argument propagationArgument;
    private boolean applyExpressionForResolvedArguments;

    public CalculatedFieldCtx(CalculatedField calculatedField,
                              ActorSystemContext systemContext) {
        this.calculatedField = calculatedField;

        this.cfId = calculatedField.getId();
        this.tenantId = calculatedField.getTenantId();
        this.entityId = calculatedField.getEntityId();
        this.cfType = calculatedField.getType();
        this.arguments = new HashMap<>();
        this.mainEntityArguments = new HashMap<>();
        this.linkedEntityArguments = new HashMap<>();
        this.dynamicEntityArguments = new HashMap<>();
        this.argNames = new ArrayList<>();
        this.mainEntityGeofencingArgumentNames = new ArrayList<>();
        this.linkedEntityAndCurrentOwnerGeofencingArgumentNames = new ArrayList<>();
        this.output = calculatedField.getConfiguration().getOutput();
        if (calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration argBasedConfig) {
            this.arguments.putAll(argBasedConfig.getArguments());
            for (Map.Entry<String, Argument> entry : arguments.entrySet()) {
                var refId = entry.getValue().getRefEntityId();
                var refKey = entry.getValue().getRefEntityKey();
                if (refId == null) {
                    if (entry.getValue().hasRelationQuerySource()) {
                        relationQueryDynamicArguments = true;
                        continue;
                    }
                    if (entry.getValue().hasOwnerSource()) {
                        dynamicEntityArguments.put(refKey, entry.getKey());
                    } else {
                        mainEntityArguments.put(refKey, entry.getKey());
                    }
                } else if (refId.equals(calculatedField.getEntityId())) {
                    mainEntityArguments.put(refKey, entry.getKey());
                } else {
                    linkedEntityArguments.computeIfAbsent(refId, key -> new HashMap<>()).put(refKey, entry.getKey());
                }
            }
            this.argNames.addAll(arguments.keySet());
            if (argBasedConfig instanceof ExpressionBasedCalculatedFieldConfiguration expressionBasedConfig) {
                this.expression = expressionBasedConfig.getExpression();
                this.useLatestTs = CalculatedFieldType.SIMPLE.equals(calculatedField.getType()) && ((SimpleCalculatedFieldConfiguration) argBasedConfig).isUseLatestTs();
            }
            if (calculatedField.getConfiguration() instanceof GeofencingCalculatedFieldConfiguration geofencingConfig) {
                geofencingConfig.getZoneGroups().forEach((zoneGroupName, config) -> {
                    if (config.isCfEntitySource(entityId)) {
                        mainEntityGeofencingArgumentNames.add(zoneGroupName);
                        return;
                    }
                    if (config.isLinkedCfEntitySource(entityId) || config.hasCurrentOwnerSource()) {
                        linkedEntityAndCurrentOwnerGeofencingArgumentNames.add(zoneGroupName);
                    }
                });
            }
            if (calculatedField.getConfiguration() instanceof PropagationCalculatedFieldConfiguration propagationConfig) {
                propagationArgument = propagationConfig.toPropagationArgument();
                applyExpressionForResolvedArguments = propagationConfig.isApplyExpressionToResolvedArguments();
                relationQueryDynamicArguments = true;
            }
        }
        if (calculatedField.getConfiguration() instanceof ScheduledUpdateSupportedCalculatedFieldConfiguration scheduledConfig) {
            this.scheduledUpdateIntervalMillis = scheduledConfig.isScheduledUpdateEnabled() ? TimeUnit.SECONDS.toMillis(scheduledConfig.getScheduledUpdateInterval()) : -1L;
        }
        this.requiresScheduledReevaluation = calculatedField.getConfiguration().requiresScheduledReevaluation();
        this.systemContext = systemContext;
        this.tbelInvokeService = systemContext.getTbelInvokeService();
        this.relationService = systemContext.getRelationService();
        this.alarmService = systemContext.getAlarmService();

        this.maxDataPointsPerRollingArg = systemContext.getApiLimitService().getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg); // fixme why tenant profile update is not handled??
        this.maxStateSize = systemContext.getApiLimitService().getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxStateSizeInKBytes) * 1024;
        this.maxSingleValueArgumentSize = systemContext.getApiLimitService().getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxSingleValueArgumentSizeInKBytes) * 1024;
    }

    public void init() {
        switch (cfType) {
            case SCRIPT -> {
                initTbelExpression(expression);
                initialized = true;
            }
            case GEOFENCING -> initialized = true;
            case SIMPLE -> {
                initSimpleExpression(expression);
                initialized = true;
            }
            case ALARM -> {
                AlarmCalculatedFieldConfiguration configuration = (AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration();
                configuration.getAllRules().map(rule -> rule.getValue().getCondition().getExpression())
                        .forEach(expression -> {
                            if (expression instanceof TbelAlarmConditionExpression tbelExpression) {
                                initTbelExpression(tbelExpression.getExpression());
                            }
                        });
                initialized = true;
            }
            case PROPAGATION -> {
                if (applyExpressionForResolvedArguments) {
                    initTbelExpression(expression);
                }
                initialized = true;
            }
        }
    }

    public double evaluateSimpleExpression(Expression expression, CalculatedFieldState state) {
        for (Map.Entry<String, ArgumentEntry> entry : state.getArguments().entrySet()) {
            try {
                BasicKvEntry kvEntry = ((SingleValueArgumentEntry) entry.getValue()).getKvEntryValue();
                double value = switch (kvEntry.getDataType()) {
                    case LONG -> kvEntry.getLongValue().map(Long::doubleValue).orElseThrow();
                    case DOUBLE -> kvEntry.getDoubleValue().orElseThrow();
                    case BOOLEAN -> kvEntry.getBooleanValue().map(b -> b ? 1.0 : 0.0).orElseThrow();
                    case STRING, JSON -> Double.parseDouble(kvEntry.getValueAsString());
                };
                expression.setVariable(entry.getKey(), value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Argument '" + entry.getKey() + "' is not a number.");
            }
        }
        return expression.evaluate();
    }

    public ListenableFuture<Object> evaluateTbelExpression(String expression, CalculatedFieldState state) {
        return evaluateTbelExpression(tbelExpressions.get(expression), state);
    }

    public ListenableFuture<Object> evaluateTbelExpression(CalculatedFieldScriptEngine expression, CalculatedFieldState state) {
        Map<String, TbelCfArg> arguments = new LinkedHashMap<>();
        List<Object> args = new ArrayList<>(argNames.size() + 1);
        args.add(new Object()); // first element is a ctx, but we will set it later;
        for (String argName : argNames) {
            var arg = toTbelArgument(argName, state);
            arguments.put(argName, arg);
            if (arg instanceof TbelCfSingleValueArg svArg) {
                args.add(svArg.getValue());
            } else {
                args.add(arg);
            }
        }
        args.set(0, new TbelCfCtx(arguments, state.getLatestTimestamp()));

        return expression.executeScriptAsync(args.toArray());
    }

    public ScheduledFuture<?> scheduleReevaluation(long delayMs, TbActorRef actorCtx) {
        log.debug("[{}] Scheduling CF reevaluation in {} ms", cfId, delayMs);
        return systemContext.scheduleMsgWithDelay(actorCtx, new CalculatedFieldReevaluateMsg(tenantId, this), delayMs);
    }

    private TbelCfArg toTbelArgument(String key, CalculatedFieldState state) {
        return state.getArguments().get(key).toTbelCfArg();
    }

    private void initTbelExpression(String expression) {
        if (tbelExpressions == null) {
            tbelExpressions = new HashMap<>();
        } else if (tbelExpressions.containsKey(expression)) {
            return;
        }
        try {
            CalculatedFieldScriptEngine engine = initEngine(tenantId, expression, tbelInvokeService);
            tbelExpressions.put(expression, engine);
        } catch (Exception e) {
            initialized = false;
            throw new RuntimeException("Failed to init calculated field ctx. Invalid expression syntax.", e);
        }
    }

    private void initSimpleExpression(String expression) {
        if (simpleExpressions == null) {
            simpleExpressions = new HashMap<>();
        } else if (simpleExpressions.containsKey(expression)) {
            return;
        }
        if (isValidExpression(expression)) {
            ThreadLocal<Expression> compiledExpression = ThreadLocal.withInitial(() ->
                    ExpressionUtils.createExpression(expression, this.arguments.keySet())
            );
            simpleExpressions.put(expression, compiledExpression);
        } else {
            initialized = false;
            throw new RuntimeException("Failed to init calculated field ctx. Invalid expression syntax.");
        }
    }

    private CalculatedFieldScriptEngine initEngine(TenantId tenantId, String expression, TbelInvokeService tbelInvokeService) {
        if (tbelInvokeService == null) {
            throw new IllegalArgumentException("TBEL script engine is disabled!");
        }

        List<String> ctxAndArgNames = new ArrayList<>(argNames.size() + 1);
        ctxAndArgNames.add("ctx");
        ctxAndArgNames.addAll(argNames);
        return new CalculatedFieldTbelScriptEngine(
                tenantId,
                tbelInvokeService,
                expression,
                ctxAndArgNames.toArray(String[]::new)
        );
    }

    private boolean isValidExpression(String expression) {
        try {
            MVEL.compileExpression(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean matches(List<AttributeKvEntry> values, AttributeScope scope) {
        return matchesAttributes(mainEntityArguments, values, scope);
    }

    public boolean linkMatches(EntityId entityId, List<AttributeKvEntry> values, AttributeScope scope) {
        var map = linkedEntityArguments.get(entityId);
        return map != null && matchesAttributes(map, values, scope);
    }

    public boolean matches(List<TsKvEntry> values) {
        return matchesTimeSeries(mainEntityArguments, values);
    }

    public boolean linkMatches(EntityId entityId, List<TsKvEntry> values) {
        var map = linkedEntityArguments.get(entityId);
        return map != null && matchesTimeSeries(map, values);
    }

    public boolean dynamicSourceMatches(List<TsKvEntry> values) {
        return matchesTimeSeries(dynamicEntityArguments, values);
    }

    public boolean dynamicSourceMatches(List<AttributeKvEntry> values, AttributeScope scope) {
        return matchesAttributes(dynamicEntityArguments, values, scope);
    }

    private boolean matchesAttributes(Map<ReferencedEntityKey, String> argMap, List<AttributeKvEntry> values, AttributeScope scope) {
        if (argMap.isEmpty() || values.isEmpty()) {
            return false;
        }

        for (AttributeKvEntry attrKv : values) {
            if (argMap.containsKey(new ReferencedEntityKey(attrKv.getKey(), ArgumentType.ATTRIBUTE, scope))) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesTimeSeries(Map<ReferencedEntityKey, String> argMap, List<TsKvEntry> values) {
        if (argMap.isEmpty() || values.isEmpty()) {
            return false;
        }

        for (TsKvEntry tsKv : values) {

            ReferencedEntityKey latestKey = new ReferencedEntityKey(tsKv.getKey(), ArgumentType.TS_LATEST, null);
            if (argMap.containsKey(latestKey)) {
                return true;
            }

            ReferencedEntityKey rollingKey = new ReferencedEntityKey(tsKv.getKey(), ArgumentType.TS_ROLLING, null);
            if (argMap.containsKey(rollingKey)) {
                return true;
            }
        }

        return false;
    }

    public boolean matchesKeys(List<String> keys, AttributeScope scope) {
        return matchesAttributesKeys(mainEntityArguments, keys, scope);
    }

    public boolean matchesKeys(List<String> keys) {
        return matchesTimeSeriesKeys(mainEntityArguments, keys);
    }

    public boolean matchesDynamicSourceKeys(List<String> keys, AttributeScope scope) {
        return matchesAttributesKeys(dynamicEntityArguments, keys, scope);
    }

    public boolean matchesDynamicSourceKeys(List<String> keys) {
        return matchesTimeSeriesKeys(dynamicEntityArguments, keys);
    }

    private boolean matchesAttributesKeys(Map<ReferencedEntityKey, String> argMap, List<String> keys, AttributeScope scope) {
        if (argMap.isEmpty() || keys.isEmpty()) {
            return false;
        }

        for (String key : keys) {
            ReferencedEntityKey attrKey = new ReferencedEntityKey(key, ArgumentType.ATTRIBUTE, scope);
            if (argMap.containsKey(attrKey)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesTimeSeriesKeys(Map<ReferencedEntityKey, String> argMap, List<String> keys) {
        if (argMap.isEmpty() || keys.isEmpty()) {
            return false;
        }

        for (String key : keys) {

            ReferencedEntityKey latestKey = new ReferencedEntityKey(key, ArgumentType.TS_LATEST, null);
            if (argMap.containsKey(latestKey)) {
                return true;
            }

            ReferencedEntityKey rollingKey = new ReferencedEntityKey(key, ArgumentType.TS_ROLLING, null);
            if (argMap.containsKey(rollingKey)) {
                return true;
            }
        }

        return false;
    }

    public boolean linkMatchesAttrKeys(EntityId entityId, List<String> keys, AttributeScope scope) {
        var map = linkedEntityArguments.get(entityId);
        return map != null && matchesAttributesKeys(map, keys, scope);
    }

    public boolean linkMatchesTsKeys(EntityId entityId, List<String> keys) {
        var map = linkedEntityArguments.get(entityId);
        return map != null && matchesTimeSeriesKeys(map, keys);
    }

    public boolean dynamicSourceMatches(CalculatedFieldTelemetryMsgProto proto) {
        if (!proto.getTsDataList().isEmpty()) {
            List<TsKvEntry> updatedTelemetry = proto.getTsDataList().stream()
                    .map(ProtoUtils::fromProto)
                    .toList();
            return dynamicSourceMatches(updatedTelemetry);
        } else if (!proto.getAttrDataList().isEmpty()) {
            AttributeScope scope = AttributeScope.valueOf(proto.getScope().name());
            List<AttributeKvEntry> updatedTelemetry = proto.getAttrDataList().stream()
                    .map(ProtoUtils::fromProto)
                    .toList();
            return dynamicSourceMatches(updatedTelemetry, scope);
        } else if (!proto.getRemovedTsKeysList().isEmpty()) {
            return matchesDynamicSourceKeys(proto.getRemovedTsKeysList());
        } else {
            return matchesDynamicSourceKeys(proto.getRemovedAttrKeysList(), AttributeScope.valueOf(proto.getScope().name()));
        }
    }

    public boolean linkMatches(EntityId entityId, CalculatedFieldTelemetryMsgProto proto) {
        if (!proto.getTsDataList().isEmpty()) {
            List<TsKvEntry> updatedTelemetry = proto.getTsDataList().stream()
                    .map(ProtoUtils::fromProto)
                    .toList();
            return linkMatches(entityId, updatedTelemetry);
        } else if (!proto.getAttrDataList().isEmpty()) {
            AttributeScope scope = AttributeScope.valueOf(proto.getScope().name());
            List<AttributeKvEntry> updatedTelemetry = proto.getAttrDataList().stream()
                    .map(ProtoUtils::fromProto)
                    .toList();
            return linkMatches(entityId, updatedTelemetry, scope);
        } else if (!proto.getRemovedTsKeysList().isEmpty()) {
            return linkMatchesTsKeys(entityId, proto.getRemovedTsKeysList());
        } else {
            return linkMatchesAttrKeys(entityId, proto.getRemovedAttrKeysList(), AttributeScope.valueOf(proto.getScope().name()));
        }
    }

    public Map<ReferencedEntityKey, String> getLinkedAndDynamicArgs(EntityId entityId) {
        var argNames = new HashMap<ReferencedEntityKey, String>();
        var linkedArgNames = linkedEntityArguments.get(entityId);
        if (linkedArgNames != null && !linkedArgNames.isEmpty()) {
            argNames.putAll(linkedArgNames);
        }
        if (dynamicEntityArguments != null && !dynamicEntityArguments.isEmpty()) {
            argNames.putAll(dynamicEntityArguments);
        }
        return argNames;
    }

    public CalculatedFieldEntityCtxId toCalculatedFieldEntityCtxId() {
        return new CalculatedFieldEntityCtxId(tenantId, cfId, entityId);
    }

    public boolean hasContextOnlyChanges(CalculatedFieldCtx other) { // has changes that do not require state reinit and will be picked up by the state on the fly
        if (calculatedField.getConfiguration() instanceof ExpressionBasedCalculatedFieldConfiguration && !Objects.equals(expression, other.expression)) {
            return true;
        }
        if (!Objects.equals(output, other.output)) {
            return true;
        }
        if (cfType == CalculatedFieldType.ALARM) {
            if (!calculatedField.getName().equals(other.getCalculatedField().getName())) {
                return true;
            }

            var thisConfig = (AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration();
            var otherConfig = (AlarmCalculatedFieldConfiguration) other.getCalculatedField().getConfiguration();
            if (!thisConfig.rulesEqual(otherConfig, AlarmRule::equals)) {
                // if the rules have any changes not tracked by hasStateChanges
                return true;
            }
        }
        return scheduledUpdateIntervalMillis != other.scheduledUpdateIntervalMillis;
    }

    public boolean hasStateChanges(CalculatedFieldCtx other) {
        if (!arguments.equals(other.arguments)) {
            return true;
        }
        if (cfType == CalculatedFieldType.ALARM) {
            var thisConfig = (AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration();
            var otherConfig = (AlarmCalculatedFieldConfiguration) other.getCalculatedField().getConfiguration();
            if (!thisConfig.rulesEqual(otherConfig, (thisRule, otherRule) -> {
                return thisRule.getCondition().getType() == otherRule.getCondition().getType();
            })) {
                // reinitializing only if the rule list changed, or if a condition type changed for any rule
                return true;
            }
        }
        return hasGeofencingZoneGroupConfigurationChanges(other);
    }

    private boolean hasGeofencingZoneGroupConfigurationChanges(CalculatedFieldCtx other) {
        if (calculatedField.getConfiguration() instanceof GeofencingCalculatedFieldConfiguration thisConfig
            && other.calculatedField.getConfiguration() instanceof GeofencingCalculatedFieldConfiguration otherConfig) {
            return !thisConfig.getZoneGroups().equals(otherConfig.getZoneGroups());
        }
        return false;
    }

    private boolean isScheduledUpdateEnabled() {
        return scheduledUpdateIntervalMillis != -1;
    }

    public boolean shouldFetchRelationQueryDynamicArgumentsFromDb(CalculatedFieldState state) {
        if (!relationQueryDynamicArguments) {
            return false;
        }
        return switch (cfType) {
            case PROPAGATION -> true;
            case GEOFENCING -> {
                if (!isScheduledUpdateEnabled()) {
                    yield false;
                }
                var geofencingState = (GeofencingCalculatedFieldState) state;
                if (geofencingState.getLastDynamicArgumentsRefreshTs() == -1L) {
                    yield true;
                }
                yield geofencingState.getLastDynamicArgumentsRefreshTs() <
                      System.currentTimeMillis() - scheduledUpdateIntervalMillis;
            }
            default -> false;
        };
    }

    @Override
    public void close() {
        try {
            if (tbelExpressions != null) {
                tbelExpressions.values().forEach(CalculatedFieldScriptEngine::destroy);
            }
            if (simpleExpressions != null) {
                simpleExpressions.values().forEach(ThreadLocal::remove);
            }
        } catch (Exception e) {
            log.warn("Failed to stop {}", this, e);
        }
    }

    public String getSizeExceedsLimitMessage() {
        return "Failed to init CF state. State size exceeds limit of " + (maxStateSize / 1024) + "Kb!";
    }

    public boolean hasCurrentOwnerSourceArguments() {
        return !dynamicEntityArguments.isEmpty();
    }

    @Override
    public String toString() {
        return "CalculatedFieldCtx{" +
               "cfId=" + cfId +
               ", cfType=" + cfType +
               ", entityId=" + entityId +
               '}';
    }

}
