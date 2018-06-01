/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

/**
 * Created by ashvayka on 01.06.18.
 */
@Data
@AllArgsConstructor
public class EntityFieldsData {

    public static final String DEFAULT = "default";

    private final EntityId entityId;
    private final String name;
    private final String type;

    public EntityFieldsData(EntityId entityId, String name) {
        this(entityId, name, DEFAULT);
    }
}
