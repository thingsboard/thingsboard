package org.thingsboard.server.actors.shared.application;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.TenantId;

public class TenantApplicationManager extends ApplicationManager{

    private final TenantId tenantId;

    @Override
    TenantId getTenantId() {
        return tenantId;
    }

    public TenantApplicationManager(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
    }
}
