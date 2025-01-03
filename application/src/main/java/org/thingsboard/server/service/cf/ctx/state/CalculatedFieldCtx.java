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
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CalculatedFieldCtx {

    private CalculatedFieldId cfId;
    private TenantId tenantId;
    private EntityId entityId;
    private CalculatedFieldType cfType;
    private final Map<String, Argument> arguments;
    private final List<String> argKeys;
    private Output output;
    private String expression;
    private TbelInvokeService tbelInvokeService;
    private CalculatedFieldScriptEngine calculatedFieldScriptEngine;

    public CalculatedFieldCtx(CalculatedField calculatedField, TbelInvokeService tbelInvokeService) {
        this.cfId = calculatedField.getId();
        this.tenantId = calculatedField.getTenantId();
        this.entityId = calculatedField.getEntityId();
        this.cfType = calculatedField.getType();
        CalculatedFieldConfiguration configuration = calculatedField.getConfiguration();
        this.arguments = configuration.getArguments();
        this.argKeys = new ArrayList<>(arguments.keySet());
        this.output = configuration.getOutput();
        this.expression = configuration.getExpression();
        this.tbelInvokeService = tbelInvokeService;
        if (CalculatedFieldType.SCRIPT.equals(calculatedField.getType())) {
            this.calculatedFieldScriptEngine = initEngine(tenantId, expression, tbelInvokeService);
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
                argKeys.toArray(String[]::new)
        );
    }

}
