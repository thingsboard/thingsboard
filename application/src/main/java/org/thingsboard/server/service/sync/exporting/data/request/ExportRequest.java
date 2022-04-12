/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.exporting.data.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "SINGLE_ENTITY", value = SingleEntityExportRequest.class),
        @Type(name = "ENTITY_LIST", value = EntityListExportRequest.class),
        @Type(name = "ENTITY_TYPE", value = EntityTypeExportRequest.class),
        @Type(name = "CUSTOM_ENTITY_FILTER", value = CustomEntityFilterExportRequest.class),
        @Type(name = "CUSTOM_ENTITY_QUERY", value = CustomEntityQueryExportRequest.class)
})
@Data
public abstract class ExportRequest {

    private EntityExportSettings exportSettings;

    public abstract ExportRequestType getType();

}
