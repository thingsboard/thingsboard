// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.custommenu;

import java.util.function.Predicate;

public interface EtagCacheService<K> {

    String getETag(K cacheKey);

    void putETag(K cacheKey, String etag);

    void evictETags(K cacheKey);

    void invalidateByFilter(Predicate<K> cacheKey);

}
