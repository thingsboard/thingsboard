// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "IMMEDIATE", schema = TimeSeriesImmediateOutputStrategy.class),
                @DiscriminatorMapping(value = "RULE_CHAIN", schema = TimeSeriesRuleChainOutputStrategy.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TimeSeriesImmediateOutputStrategy.class, name = "IMMEDIATE"),
        @JsonSubTypes.Type(value = TimeSeriesRuleChainOutputStrategy.class, name = "RULE_CHAIN")
})
public interface TimeSeriesOutputStrategy extends OutputStrategy {
}
