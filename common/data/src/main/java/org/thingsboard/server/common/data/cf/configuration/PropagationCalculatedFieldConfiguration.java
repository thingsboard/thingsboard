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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PropagationCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration {

    public static final String PROPAGATION_CONFIG_ARGUMENT = "propagationCtx";

    private EntitySearchDirection direction;
    private String relationType;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.PROPAGATION;
    }

    @Override
    public void validate() {
        baseCalculatedFieldRestriction();
        propagationRestriction();
        if (direction == null) {
            throw new IllegalArgumentException("Propagation calculated field direction must be specified!");
        }
        if (StringUtils.isBlank(relationType)) {
            throw new IllegalArgumentException("Propagation calculated field relation type must be specified!");
        }
    }

    public Argument toPropagationArgument() {
        var refDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        refDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(direction, relationType)));
        var propagationArgument = new Argument();
        propagationArgument.setRefDynamicSourceConfiguration(refDynamicSourceConfiguration);
        return propagationArgument;
    }

    private void propagationRestriction() {
        if (arguments.entrySet().stream().anyMatch(entry -> entry.getKey().equals(PROPAGATION_CONFIG_ARGUMENT))) {
            throw new IllegalArgumentException("Argument name '" + PROPAGATION_CONFIG_ARGUMENT + "' is reserved and cannot be used.");
        }
    }
}
