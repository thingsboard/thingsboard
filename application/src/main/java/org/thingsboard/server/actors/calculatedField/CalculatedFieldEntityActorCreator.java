// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbCalculatedFieldEntityActorId;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.device.DeviceActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public class CalculatedFieldEntityActorCreator extends ContextBasedCreator {

    private final TenantId tenantId;
    private final EntityId entityId;

    public CalculatedFieldEntityActorCreator(ActorSystemContext context, TenantId tenantId, EntityId entityId) {
        super(context);
        this.tenantId = tenantId;
        this.entityId = entityId;
    }

    @Override
    public TbActorId createActorId() {
        return new TbCalculatedFieldEntityActorId(entityId);
    }

    @Override
    public TbActor createActor() {
        return new CalculatedFieldEntityActor(context, tenantId, entityId);
    }

}
