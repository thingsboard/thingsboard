// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "RELATION_PATH_QUERY", schema = RelationPathQueryDynamicSourceConfiguration.class),
                @DiscriminatorMapping(value = "CURRENT_OWNER", schema = CurrentOwnerDynamicSourceConfiguration.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CurrentOwnerDynamicSourceConfiguration.class, name = "CURRENT_OWNER"),
        @JsonSubTypes.Type(value = RelationPathQueryDynamicSourceConfiguration.class, name = "RELATION_PATH_QUERY")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CfArgumentDynamicSourceConfiguration {

    @JsonIgnore
    CFArgumentDynamicSourceType getType();

    default void validate() {}

}
