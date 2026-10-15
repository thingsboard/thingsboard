// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.secret;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.secret.SecretCacheEvictEvent;
import org.thingsboard.server.cache.secret.SecretCacheKey;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.encryptionkey.EncryptionService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.validator.SecretDataValidator;
import org.thingsboard.server.dao.sql.HasSecretsEntityDao;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecretServiceImpl extends AbstractCachedEntityService<SecretCacheKey, Secret, SecretCacheEvictEvent> implements SecretService {

    private static final String INCORRECT_SECRET_ID = "Incorrect secretId ";

    private final SecretDao secretDao;
    private final SecretInfoDao secretInfoDao;
    private final SecretDataValidator secretValidator;
    private final EncryptionService encryptionService;

    private final Map<EntityType, HasSecretsEntityDao> hasSecretsEntityDaos = new EnumMap<>(EntityType.class);

    @Autowired
    private void setHasSecretsEntityDaos(List<HasSecretsEntityDao> hasSecretsEntityDaos) {
        hasSecretsEntityDaos.forEach(dao -> this.hasSecretsEntityDaos.put(dao.getEntityType(), dao));
    }

    @Override
    public void handleEvictEvent(SecretCacheEvictEvent event) {
        cache.evict(new SecretCacheKey(event.tenantId(), event.customerId(), event.name()));
    }

    @Override
    public Secret saveSecret(TenantId tenantId, Secret secret) {
        log.trace("Executing saveSecret [{}]", secret);
        try {
            Secret old = secretValidator.validate(secret, Secret::getTenantId);

            boolean isValueUpdated = false;
            if (secret.getValue() != null) {
                byte[] encrypted = encryptionService.encrypt(tenantId, secret.getType(), secret.getValue().getBytes());
                secret.setEncryptedValue(encrypted);
                isValueUpdated = true;
            } else if (old != null) {
                secret.setEncryptedValue(old.getEncryptedValue());
            }

            Secret savedSecret = secretDao.save(tenantId, secret);
            publishEvictEvent(new SecretCacheEvictEvent(savedSecret.getTenantId(), savedSecret.getCustomerId(), savedSecret.getName()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedSecret.getId()).entity(savedSecret).created(secret.getId() == null).broadcastEvent(isValueUpdated).build());
            return savedSecret;
        } catch (Exception e) {
            checkConstraintViolation(e, "secret_unq_key", "Secret with such name already exists!");
            throw e;
        }
    }

    @Override
    public TbSecretDeleteResult deleteSecret(TenantId tenantId, SecretInfo secretInfo) {
        SecretId secretId = secretInfo.getId();
        log.trace("Executing deleteSecret [{}]", secretId);
        validateId(secretId, id -> INCORRECT_SECRET_ID + id);
        return deleteSecret(tenantId, secretId, false);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteSecret(tenantId, id, force);
    }

    private TbSecretDeleteResult deleteSecret(TenantId tenantId, EntityId entityId, boolean force) {
        UUID secretId = entityId.getId();
        validateId(secretId, id -> INCORRECT_SECRET_ID + id);
        SecretInfo secretInfo = secretInfoDao.findById(tenantId, secretId);
        if (secretInfo == null) {
            return TbSecretDeleteResult.builder().success(force).build();
        }
        return deleteSecret(tenantId, secretInfo, force);
    }

    private TbSecretDeleteResult deleteSecret(TenantId tenantId, SecretInfo secretInfo, boolean force) {
        TbSecretDeleteResult.TbSecretDeleteResultBuilder result = TbSecretDeleteResult.builder();
        boolean success = true;
        if (!force) {
            var entities = findEntitiesBySecret(tenantId, secretInfo);
            if (!entities.isEmpty()) {
                success = false;
                result.references(entities);
            }
        }
        if (success) {
            secretDao.removeById(tenantId, secretInfo.getId().getId());
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(secretInfo.getId()).build());
            publishEvictEvent(new SecretCacheEvictEvent(tenantId, secretInfo.getCustomerId(), secretInfo.getName()));
        }
        return result.success(success).build();
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteSecretsByTenantId, tenantId [{}]", tenantId);
        secretDao.deleteByTenantId(tenantId);
    }

    @Override
    public Secret findSecretById(TenantId tenantId, SecretId secretId) {
        log.trace("Executing findSecretById [{}] [{}]", tenantId, secretId);
        return secretDao.findById(tenantId, secretId.getId());
    }

    @Override
    public SecretInfo findSecretInfoById(TenantId tenantId, SecretId secretId) {
        log.trace("Executing findSecretInfoById [{}] [{}]", tenantId, secretId);
        return secretInfoDao.findById(tenantId, secretId.getId());
    }

    @Override
    public Secret findSecretByName(TenantId tenantId, CustomerId customerId, String name) {
        log.trace("Executing findSecretByName [{}] [{}] [{}]", tenantId, customerId, name);
        return cache.getAndPutInTransaction(new SecretCacheKey(tenantId, customerId, name),
                () -> secretDao.findByName(tenantId, customerId, name), true);
    }

    @Override
    public SecretInfo findSecretInfoByName(TenantId tenantId, CustomerId customerId, String name) {
        log.trace("Executing findSecretInfoByName [{}] [{}] [{}]", tenantId, customerId, name);
        return secretInfoDao.findByName(tenantId, customerId, name);
    }

    @Override
    public List<String> findSecretNamesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findSecretNamesByTenantIdAndCustomerId [{}] [{}]", tenantId, customerId);
        return secretInfoDao.findAllNamesByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Override
    public PageData<Secret> findSecretsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findSecretsByTenantIdAndCustomerId [{}] [{}]", tenantId, customerId);
        return secretDao.findByTenantIdAndCustomerId(tenantId, customerId, pageLink);
    }

    @Override
    public PageData<SecretInfo> findSecretInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findSecretInfosByTenantIdAndCustomerId [{}] [{}]", tenantId, customerId);
        return secretInfoDao.findByTenantIdAndCustomerId(tenantId, customerId, pageLink);
    }

    @Override
    public void deleteSecretsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteSecretsByTenantIdAndCustomerId [{}] [{}]", tenantId, customerId);
        customerSecretsRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public Map<EntityType, List<EntityInfo>> findEntitiesBySecret(TenantId tenantId, SecretInfo secretInfo) {
        // the scanned entities are tenant-level and resolve only tenant secrets
        if (EntityType.CUSTOMER.equals(secretInfo.getOwnerId().getEntityType())) {
            return Map.of();
        }
        Map<EntityType, List<EntityInfo>> affectedEntities = new HashMap<>();
        String placeholder = String.format("${secret:%s;type:%s}", secretInfo.getName(), secretInfo.getType());
        hasSecretsEntityDaos.forEach((entityType, hasSecretsEntityDao) -> {
            var entities = hasSecretsEntityDao.findByTenantIdAndSecretPlaceholder(tenantId, placeholder);
            if (!entities.isEmpty()) {
                affectedEntities.put(entityType, entities);
            }
        });
        return affectedEntities;
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findSecretById(tenantId, new SecretId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(secretDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    private final PaginatedRemover<CustomerId, SecretInfo> customerSecretsRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<SecretInfo> findEntities(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
            return secretInfoDao.findByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, SecretInfo secretInfo) {
            // force=true: customer deletion removes its secrets regardless of references
            deleteSecret(tenantId, secretInfo, true);
        }
    };

    @Override
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

}
