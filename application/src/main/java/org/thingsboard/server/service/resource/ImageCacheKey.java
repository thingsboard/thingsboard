package org.thingsboard.server.service.resource;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class ImageCacheKey {

    private final TenantId tenantId;
    private final String key;
    private final boolean preview;

}
