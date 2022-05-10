package org.thingsboard.server.dao.tenant;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantProfileId;

@Data
public class TenantProfileEvictEvent {
    private final TenantProfileId tenantProfileId;
    private final boolean defaultProfile;
}
