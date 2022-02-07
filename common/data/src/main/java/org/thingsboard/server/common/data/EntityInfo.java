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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;

import java.util.UUID;

@ApiModel
@Data
public class EntityInfo implements HasId<EntityId>, HasName {

    @ApiModelProperty(position = 1, value = "JSON object with the entity Id. ")
    private final EntityId id;
    @ApiModelProperty(position = 2, value = "Entity Name")
    private final String name;

    @JsonCreator
    public EntityInfo(@JsonProperty("id") EntityId id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    public EntityInfo(UUID uuid, String entityType, String name) {
        this.id = EntityIdFactory.getByTypeAndUuid(entityType, uuid);
        this.name = name;
    }

}
