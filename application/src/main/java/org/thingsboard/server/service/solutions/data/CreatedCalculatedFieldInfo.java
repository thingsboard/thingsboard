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
package org.thingsboard.server.service.solutions.data;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;

public record CreatedCalculatedFieldInfo(EntityId entityId, String entityName, String type,
                                         String name) implements HasAppliedToEntity {

    public static CreatedCalculatedFieldInfo from(EntityId entityId, String entityName, CalculatedField calculatedField) {
        return new CreatedCalculatedFieldInfo(entityId, entityName, calculatedField.getType().getDisplayName(), calculatedField.getName());
    }

    @Override
    public String getCfPageLink(UUID cfId) {
        return "/calculatedFields/" + cfId;
    }
}
