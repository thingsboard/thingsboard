// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.common.data.HasVersion;

import java.io.Serializable;

public abstract class CachedVersionedEntityService<K extends VersionedCacheKey, V extends Serializable & HasVersion, E> extends AbstractCachedEntityService<K, V, E> {

    @Autowired
    protected VersionedTbCache<K, V> cache;

}
