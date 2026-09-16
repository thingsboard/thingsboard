// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.customer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CaffeineTbTransactionalCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.Customer;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("CustomerCache")
public class CustomerCaffeineCache extends CaffeineTbTransactionalCache<CustomerCacheKey, Customer> {

    public CustomerCaffeineCache(CacheManager cacheManager) {
        super(cacheManager, CacheConstants.CUSTOMER_CACHE);
    }

}
