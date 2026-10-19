// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;

public interface GroupEntity<I extends EntityId> extends HasId<I>, HasName, TenantEntity, HasCustomerId, HasOwnerId {
}
