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
package org.thingsboard.server.actors;

import org.thingsboard.server.common.data.id.EntityId;

import java.util.Objects;

public class TbActorId {

    private final EntityId entityId;

    public TbActorId(EntityId entityId) {
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return entityId.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TbActorId actorId = (TbActorId) o;
        return entityId.equals(actorId.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId);
    }
}
