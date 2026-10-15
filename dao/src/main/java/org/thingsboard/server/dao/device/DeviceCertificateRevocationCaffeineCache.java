// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("DeviceCertificateRevocationCache")
public class DeviceCertificateRevocationCaffeineCache extends CaffeineTbTransactionalCache<DeviceCertificateRevocationCacheKey, DeviceCertificateRevocation> {

    public DeviceCertificateRevocationCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.DEVICE_CERTIFICATE_REVOCATIONS_CACHE);
    }

}
