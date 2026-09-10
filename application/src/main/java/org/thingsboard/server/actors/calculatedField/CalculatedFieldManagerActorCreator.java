// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.TbStringActorId;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public class CalculatedFieldManagerActorCreator extends ContextBasedCreator {

    private final TenantId tenantId;

    public CalculatedFieldManagerActorCreator(ActorSystemContext context, TenantId tenantId) {
        super(context);
        this.tenantId = tenantId;
    }

    @Override
    public TbActorId createActorId() {
        return new TbStringActorId("CFM|" + tenantId);
    }

    @Override
    public TbActor createActor() {
        return new CalculatedFieldManagerActor(context, tenantId);
    }

}
