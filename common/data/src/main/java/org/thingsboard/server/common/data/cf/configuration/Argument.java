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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.lang.Nullable;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Argument {

    @Nullable
    private EntityId refEntityId;
    private CfArgumentDynamicSourceConfiguration refDynamicSourceConfiguration;
    private ReferencedEntityKey refEntityKey;
    private String defaultValue;

    private Integer limit;
    private Long timeWindow;

    public boolean hasDynamicSource() {
        return refDynamicSourceConfiguration != null;
    }

    public boolean hasRelationQuerySource() {
        return hasDynamicSource() && refDynamicSourceConfiguration.getType() == CFArgumentDynamicSourceType.RELATION_PATH_QUERY;
    }

    public boolean hasOwnerSource() {
        return hasDynamicSource() && refDynamicSourceConfiguration.getType() == CFArgumentDynamicSourceType.CURRENT_OWNER;
    }

    public boolean hasTsRollingArgument() {
        return ArgumentType.TS_ROLLING.equals(refEntityKey.getType());
    }

}
