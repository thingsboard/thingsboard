/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.cache;

import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.Optional;

public interface TenantCacheService<K, V> {
    Optional<V> find(TenantId tenantId, K key);
    List<V> find(TenantId tenantId, List<K> keys);
    void insert(TenantId tenantId, K key, V value);
    void update(TenantId tenantId, K key, V value);
    void remove(TenantId tenantId, List<K> keys);
}
