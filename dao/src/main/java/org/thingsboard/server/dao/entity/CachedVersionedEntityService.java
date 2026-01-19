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
