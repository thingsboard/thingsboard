package org.thingsboard.server.service.attributes;

import groovy.lang.Tuple2;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.service.cache.TenantCacheService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AttributesTenantCacheService implements TenantCacheService<AttributesKey, AttributeKvEntry> {
    private final Map<TenantId, Map<AttributesKey, AttributeKvEntry>> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<AttributeKvEntry> find(TenantId tenantId, AttributesKey key) {
        return null;
    }

    @Override
    public List<AttributeKvEntry> find(TenantId tenantId, List<AttributesKey> keys) {
        return null;
    }

    @Override
    public void insert(TenantId tenantId, AttributesKey key, AttributeKvEntry value) {

    }

    @Override
    public void update(TenantId tenantId, AttributesKey key, AttributeKvEntry value) {

    }

    @Override
    public void remove(TenantId tenantId, List<AttributesKey> keys) {

    }
}
