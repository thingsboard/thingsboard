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

import lombok.Data;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.mvel2.MVEL;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.common.util.ExpressionFunctionsUtil.userDefinedFunctions;

@Data
public class CalculatedFieldCtx {

    private CalculatedField calculatedField;

    private CalculatedFieldId cfId;
    private TenantId tenantId;
    private EntityId entityId;
    private CalculatedFieldType cfType;
    private final Map<String, Argument> arguments;
    private final Map<ReferencedEntityKey, Set<String>> mainEntityArguments;
    private final Map<EntityId, Map<ReferencedEntityKey, Set<String>>> linkedEntityArguments;
    private final List<String> argNames;
    private Output output;
    private String expression;
    private boolean useLatestTs;
    private TbelInvokeService tbelInvokeService;
    private CalculatedFieldScriptEngine calculatedFieldScriptEngine;
    private ThreadLocal<Expression> customExpression;

    private boolean initialized;

    private long maxDataPointsPerRollingArg;
    private long maxStateSize;
    private long maxSingleValueArgumentSize;

    public CalculatedFieldCtx(CalculatedField calculatedField, TbelInvokeService tbelInvokeService, ApiLimitService apiLimitService) {
        this.calculatedField = calculatedField;

        this.cfId = calculatedField.getId();
        this.tenantId = calculatedField.getTenantId();
        this.entityId = calculatedField.getEntityId();
        this.cfType = calculatedField.getType();
        CalculatedFieldConfiguration configuration = calculatedField.getConfiguration();
        this.arguments = configuration.getArguments();
        this.mainEntityArguments = new HashMap<>();
        this.linkedEntityArguments = new HashMap<>();
        for (Map.Entry<String, Argument> entry : arguments.entrySet()) {
            var refId = entry.getValue().getRefEntityId();
            var refKey = entry.getValue().getRefEntityKey();
            if (refId == null || refId.equals(calculatedField.getEntityId())) {
                mainEntityArguments.compute(refKey, (key, existingNames) -> CollectionsUtil.addToSet(existingNames, entry.getKey()));
            } else {
                linkedEntityArguments.computeIfAbsent(refId, key -> new HashMap<>())
                        .compute(refKey, (key, existingNames) -> CollectionsUtil.addToSet(existingNames, entry.getKey()));
            }
        }
        this.argNames = new ArrayList<>(arguments.keySet());
        this.output = configuration.getOutput();
        this.expression = configuration.getExpression();
        this.useLatestTs = CalculatedFieldType.SIMPLE.equals(calculatedField.getType()) && ((SimpleCalculatedFieldConfiguration) configuration).isUseLatestTs();
        this.tbelInvokeService = tbelInvokeService;

        this.maxDataPointsPerRollingArg = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        this.maxStateSize = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxStateSizeInKBytes) * 1024;
        this.maxSingleValueArgumentSize = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxSingleValueArgumentSizeInKBytes) * 1024;
    }

    public void init() {
        if (CalculatedFieldType.SCRIPT.equals(cfType)) {
            try {
                this.calculatedFieldScriptEngine = initEngine(tenantId, expression, tbelInvokeService);
                initialized = true;
            } catch (Exception e) {
                initialized = false;
                throw new RuntimeException("Failed to init calculated field ctx. Invalid expression syntax.", e);
            }
        } else {
            if (isValidExpression(expression)) {
                this.customExpression = ThreadLocal.withInitial(() ->
                        new ExpressionBuilder(expression)
                                .functions(userDefinedFunctions)
                                .implicitMultiplication(true)
                                .variables(this.arguments.keySet())
                                .build()
                );
                initialized = true;
            } else {
                initialized = false;
                throw new RuntimeException("Failed to init calculated field ctx. Invalid expression syntax.");
            }
        }
    }

    public void stop() {
        if (calculatedFieldScriptEngine != null) {
            calculatedFieldScriptEngine.destroy();
        }
        if (customExpression != null) {
            customExpression.remove();
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

    private boolean matchesAttributes(Map<ReferencedEntityKey, Set<String>> argMap, List<AttributeKvEntry> values, AttributeScope scope) {
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

    private boolean matchesTimeSeries(Map<ReferencedEntityKey, Set<String>> argMap, List<TsKvEntry> values) {
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

    private boolean matchesAttributesKeys(Map<ReferencedEntityKey, Set<String>> argMap, List<String> keys, AttributeScope scope) {
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

    private boolean matchesTimeSeriesKeys(Map<ReferencedEntityKey, Set<String>> argMap, List<String> keys) {
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

    public CalculatedFieldEntityCtxId toCalculatedFieldEntityCtxId() {
        return new CalculatedFieldEntityCtxId(tenantId, cfId, entityId);
    }

    public boolean hasOtherSignificantChanges(CalculatedFieldCtx other) {
        boolean expressionChanged = !expression.equals(other.expression);
        boolean outputChanged = !output.equals(other.output);
        return expressionChanged || outputChanged;
    }

    public boolean hasStateChanges(CalculatedFieldCtx other) {
        boolean typeChanged = !cfType.equals(other.cfType);
        boolean argumentsChanged = !arguments.equals(other.arguments);
        return typeChanged || argumentsChanged;
    }

    public String getSizeExceedsLimitMessage() {
        return "Failed to init CF state. State size exceeds limit of " + (maxStateSize / 1024) + "Kb!";
    }

}
