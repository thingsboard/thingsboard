// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.create;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(
        description = "Request for creating a version",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SINGLE_ENTITY", schema = SingleEntityVersionCreateRequest.class),
                @DiscriminatorMapping(value = "COMPLEX", schema = ComplexVersionCreateRequest.class)
        }
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "SINGLE_ENTITY", value = SingleEntityVersionCreateRequest.class),
        @Type(name = "COMPLEX", value = ComplexVersionCreateRequest.class)
})
@Data
public abstract class VersionCreateRequest {

    private String versionName;
    private String branch;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of the version to create")
    public abstract VersionCreateRequestType getType();

}
