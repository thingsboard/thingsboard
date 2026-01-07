/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
