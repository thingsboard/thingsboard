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
