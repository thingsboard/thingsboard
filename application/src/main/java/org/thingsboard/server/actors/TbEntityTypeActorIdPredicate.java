// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.function.Predicate;

@RequiredArgsConstructor
public class TbEntityTypeActorIdPredicate implements Predicate<TbActorId> {

    private final EntityType entityType;

    @Override
    public boolean test(TbActorId actorId) {
        return actorId instanceof TbEntityActorId && testEntityId(((TbEntityActorId) actorId).getEntityId());
    }

    protected boolean testEntityId(EntityId entityId) {
        return entityId.getEntityType().equals(entityType);
    }
}
