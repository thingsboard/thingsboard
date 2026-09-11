// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
