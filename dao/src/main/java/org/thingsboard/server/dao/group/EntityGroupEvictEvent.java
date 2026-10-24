// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.group;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

public record EntityGroupEvictEvent(EntityId ownerId, EntityType entityType, String newGroupName, String oldGroupName) {
}
