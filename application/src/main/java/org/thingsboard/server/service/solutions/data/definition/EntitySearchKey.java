// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@AllArgsConstructor
public class EntitySearchKey {

    private final EntityId ownerId;
    private final EntityType entityType;
    private final String entityName;

}
