// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.lang.Nullable;
import org.thingsboard.server.common.data.id.EntityId;

@Schema
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
