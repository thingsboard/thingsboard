/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class EntityDataSortOrder {

    private EntityKey key;
    private Direction direction;

    public EntityDataSortOrder() {}

    public EntityDataSortOrder(EntityKey key) {
        this(key, Direction.ASC);
    }

    public EntityDataSortOrder(EntityKey key, Direction direction) {
        this.key = key;
        this.direction = direction;
    }

    public enum Direction {
        ASC, DESC
    }

}
