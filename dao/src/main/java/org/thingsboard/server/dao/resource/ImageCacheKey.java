// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.resource;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos.ImageCacheKeyProto;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageCacheKey {

    private final TenantId tenantId;
    private final String resourceKey;
    @With
    private final boolean preview;

    private final String publicResourceKey;

    public static ImageCacheKey forImage(TenantId tenantId, String key, boolean preview) {
        return new ImageCacheKey(tenantId, key, preview, null);
    }

    public static ImageCacheKey forImage(TenantId tenantId, String key) {
        return forImage(tenantId, key, false);
    }

    public static ImageCacheKey forPublicImage(String publicKey) {
        return new ImageCacheKey(null, null, false, publicKey);
    }

    public ImageCacheKeyProto toProto() {
        var msg = ImageCacheKeyProto.newBuilder();
        if (resourceKey != null) {
            msg.setResourceKey(resourceKey);
        } else {
            msg.setPublicResourceKey(publicResourceKey);
        }
        return msg.build();
    }

    public boolean isPublic() {
        return this.publicResourceKey != null;
    }

}
