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
package org.thingsboard.server.common.data.cf.configuration;

import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public abstract class BaseCalculatedFieldConfiguration implements ExpressionBasedCalculatedFieldConfiguration {

    protected Map<String, Argument> arguments;
    protected String expression;
    protected Output output;

    @Override
    public List<EntityId> getReferencedEntities() {
        return arguments.values().stream()
                .map(Argument::getRefEntityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void validate() {
        if (arguments.containsKey("ctx")) {
            throw new IllegalArgumentException("Argument name 'ctx' is reserved and cannot be used.");
        }
        if (arguments.values().stream().anyMatch(Argument::hasDynamicSource)) {
            throw new IllegalArgumentException("Calculated field with type: '" + getType() + "' doesn't support dynamic source configuration!");
        }
    }

}
