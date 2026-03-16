/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
