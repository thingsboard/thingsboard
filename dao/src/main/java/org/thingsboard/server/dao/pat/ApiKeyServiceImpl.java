/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.pat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.validator.ApiKeyDataValidator;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.user.UserServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.user.UserServiceImpl.INCORRECT_USER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl extends AbstractCachedEntityService<ApiKeyCacheKey, ApiKey, ApiKeyEvictEvent> implements ApiKeyService {

    private static final String INCORRECT_API_KEY_ID = "Incorrect ApiKeyId ";
    private static final int DEFAULT_API_KEY_BYTES = 32;

    private final ApiKeyDao apiKeyDao;
    private final ApiKeyInfoDao apiKeyInfoDao;
    private final ApiKeyDataValidator apiKeyValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(ApiKeyEvictEvent event) {
        cache.evict(ApiKeyCacheKey.of(event.hash()));
    }

    @Override
    public ApiKey saveApiKey(TenantId tenantId, ApiKeyInfo apiKeyInfo) {
        log.trace("Executing saveApiKey [{}]", apiKeyInfo);
        try {
            var apiKey = new ApiKey(apiKeyInfo);
            var old = apiKeyValidator.validate(apiKey, ApiKeyInfo::getTenantId);
            if (old == null) {
                String hash = generateApiKeySecret();
                apiKey.setHash(hash);
            } else {
                apiKey.setHash(old.getHash());
            }
            var savedApiKey = apiKeyDao.save(tenantId, apiKey);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedApiKey.getId()).entity(savedApiKey).created(apiKey.getId() == null).build());
            if (old != null && old.isEnabled() != apiKey.isEnabled()) {
                publishEvictEvent(new ApiKeyEvictEvent(apiKey.getHash()));
            }
            return savedApiKey;
        } catch (Exception e) {
            checkConstraintViolation(e, "api_hash_unq_key", "Api Key with such hash already exists!");
            throw e;
        }
    }

    @Override
    public ApiKey findApiKeyById(TenantId tenantId, ApiKeyId apiKeyId) {
        log.trace("Executing findApiKeyById [{}] [{}]", tenantId, apiKeyId);
        validateId(apiKeyId, id -> INCORRECT_API_KEY_ID + id);
        return apiKeyDao.findById(tenantId, apiKeyId.getId());
    }

    @Override
    public PageData<ApiKeyInfo> findApiKeysByUserId(TenantId tenantId, UserId userId, PageLink pageLink) {
        log.trace("Executing findApiKeysByUserId [{}][{}]", tenantId, userId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        return apiKeyInfoDao.findByUserId(tenantId, userId, pageLink);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findApiKeyById(tenantId, new ApiKeyId(entityId.getId())));
    }

    @Override
    public void deleteApiKey(TenantId tenantId, ApiKey apiKey, boolean force) {
        deleteApiKey(tenantId, apiKey.getId());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteApiKey(tenantId, id);
    }

    private void deleteApiKey(TenantId tenantId, EntityId entityId) {
        UUID apiKeyId = entityId.getId();
        validateId(apiKeyId, id -> INCORRECT_API_KEY_ID + id);
        ApiKey apiKey = apiKeyDao.findById(tenantId, apiKeyId);
        if (apiKey == null) {
            return;
        }
        apiKeyDao.removeById(tenantId, apiKeyId);
        publishEvictEvent(new ApiKeyEvictEvent(apiKey.getHash()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteApiKeysByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Set<String> hashes = apiKeyDao.deleteByTenantId(tenantId);
        hashes.forEach(hash -> publishEvictEvent(new ApiKeyEvictEvent(hash)));
    }

    @Override
    public void deleteByUserId(TenantId tenantId, UserId userId) {
        log.trace("Executing deleteApiKeysByUserId, tenantId [{}]", tenantId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        Set<String> hashes = apiKeyDao.deleteByUserId(tenantId, userId);
        hashes.forEach(hash -> publishEvictEvent(new ApiKeyEvictEvent(hash)));
    }

    @Override
    public ApiKey findApiKeyByHash(String hash) {
        log.trace("Executing findApiKeyByHash [{}]", hash);
        var cacheKey = ApiKeyCacheKey.of(hash);
        return cache.getAndPutInTransaction(cacheKey, () -> apiKeyDao.findByHash(hash), true);
    }

    private static String generateApiKeySecret() {
        return StringUtils.generateSafeToken(DEFAULT_API_KEY_BYTES);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_KEY;
    }

}
