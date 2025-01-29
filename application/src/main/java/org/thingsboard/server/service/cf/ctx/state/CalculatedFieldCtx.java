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

import lombok.Data;
import net.objecthunter.exp4j.Expression;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class CalculatedFieldCtx {

    private CalculatedFieldId cfId;
    private TenantId tenantId;
    private EntityId entityId;
    private CalculatedFieldType cfType;
    private final Map<String, Argument> arguments;
    private final Map<ReferencedEntityKey, String> mainEntityArguments;
    private final Map<EntityId, Map<ReferencedEntityKey, String>> linkedEntityArguments;

    private final Map<TbPair<EntityId, ReferencedEntityKey>, String> referencedEntityKeys;
    private final List<String> argNames;
    private Output output;
    private String expression;
    private TbelInvokeService tbelInvokeService;
    private CalculatedFieldScriptEngine calculatedFieldScriptEngine;
    private ThreadLocal<Expression> customExpression;

    public CalculatedFieldCtx(CalculatedField calculatedField, TbelInvokeService tbelInvokeService) {
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
            if (refId == null) {
                mainEntityArguments.put(refKey, entry.getKey());
            } else {
                linkedEntityArguments.computeIfAbsent(refId, key -> new HashMap<>()).put(refKey, entry.getKey());
            }
        }
        this.referencedEntityKeys = arguments.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new TbPair<>(entry.getValue().getRefEntityId() == null ? entityId : entry.getValue().getRefEntityId(), entry.getValue().getRefEntityKey()),
                        Map.Entry::getKey
                ));
        this.argNames = new ArrayList<>(arguments.keySet());
        this.output = configuration.getOutput();
        this.expression = configuration.getExpression();
        this.tbelInvokeService = tbelInvokeService;
        if (CalculatedFieldType.SCRIPT.equals(calculatedField.getType())) {
            this.calculatedFieldScriptEngine = initEngine(tenantId, expression, tbelInvokeService);
        } else {
            this.customExpression = new ThreadLocal<>();
        }
    }

    private CalculatedFieldScriptEngine initEngine(TenantId tenantId, String expression, TbelInvokeService tbelInvokeService) {
        if (tbelInvokeService == null) {
            throw new IllegalArgumentException("TBEL script engine is disabled!");
        }

        return new CalculatedFieldTbelScriptEngine(
                tenantId,
                tbelInvokeService,
                expression,
                argNames.toArray(String[]::new)
        );
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

    private static boolean matchesAttributes(Map<ReferencedEntityKey, String> argMap, List<AttributeKvEntry> values, AttributeScope scope) {
        for (AttributeKvEntry attrKv : values) {
            ReferencedEntityKey attrKey = new ReferencedEntityKey(attrKv.getKey(), ArgumentType.ATTRIBUTE, scope);
            if (argMap.containsKey(attrKey)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesTimeSeries(Map<ReferencedEntityKey, String> argMap, List<TsKvEntry> values) {
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
}
