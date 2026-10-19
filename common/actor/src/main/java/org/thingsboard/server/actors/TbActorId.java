// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import org.thingsboard.server.common.data.EntityType;

public interface TbActorId {

    /**
     * Returns entity type of the actor.
     * May return null if the actor does not belong to any entity.
     * This method is added for performance optimization.
     *
     */
    EntityType getEntityType();

}
