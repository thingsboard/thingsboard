// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.load;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@Schema(
        description = "Request for loading a version",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SINGLE_ENTITY", schema = SingleEntityVersionLoadRequest.class),
                @DiscriminatorMapping(value = "ENTITY_TYPE", schema = EntityTypeVersionLoadRequest.class)
        }
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "SINGLE_ENTITY", value = SingleEntityVersionLoadRequest.class),
        @Type(name = "ENTITY_TYPE", value = EntityTypeVersionLoadRequest.class)
})
@Data
public abstract class VersionLoadRequest {

    private String versionId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of the version to load")
    public abstract VersionLoadRequestType getType();

}
