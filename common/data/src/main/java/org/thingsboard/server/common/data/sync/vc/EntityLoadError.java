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
package org.thingsboard.server.common.data.sync.vc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityLoadError implements Serializable {

    private static final long serialVersionUID = 7538450180582109391L;

    private String type;
    private EntityId source;
    private EntityId target;
    private String message;

    public static EntityLoadError credentialsError(EntityId sourceId) {
        return EntityLoadError.builder().type("DEVICE_CREDENTIALS_CONFLICT").source(sourceId).build();
    }

    public static EntityLoadError referenceEntityError(EntityId sourceId, EntityId targetId) {
        return EntityLoadError.builder().type("MISSING_REFERENCED_ENTITY").source(sourceId).target(targetId).build();
    }

    public static EntityLoadError runtimeError(String msg) {
        return EntityLoadError.builder().type("RUNTIME").message(msg).build();
    }

}
