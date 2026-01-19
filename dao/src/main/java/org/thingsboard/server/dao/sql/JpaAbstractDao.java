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
package org.thingsboard.server.dao.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.exception.EntityVersionMismatchException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@SqlDao
public abstract class JpaAbstractDao<E extends BaseEntity<D>, D>
        extends JpaAbstractDaoListeningExecutorService
        implements Dao<D> {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public D save(TenantId tenantId, D domain) {
        return save(tenantId, domain, false);
    }

    private D save(TenantId tenantId, D domain, boolean flush) {
        E entity;
        try {
            entity = getEntityClass().getConstructor(domain.getClass()).newInstance(domain);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", domain, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + domain + "}", e);
        }
        log.debug("Saving entity {}", entity);
        boolean isNew = entity.getUuid() == null;
        if (isNew) {
            entity.setCreatedTime(System.currentTimeMillis());
        } else {
            if (entity.getCreatedTime() == 0) {
                if (entity.getUuid().version() == 1) {
                    entity.setCreatedTime(Uuids.unixTimestamp(entity.getUuid()));
                } else {
                    entity.setCreatedTime(System.currentTimeMillis());
                }
            }
        }
        try {
            entity = doSave(entity, isNew, flush);
        } catch (OptimisticLockException e) {
            throw new EntityVersionMismatchException(getEntityType(), e);
        }
        return DaoUtil.getData(entity);
    }

    protected E doSave(E entity, boolean isNew, boolean flush) {
        boolean flushed = false;
        EntityManager entityManager = getEntityManager();
        if (isNew) {
            entity = create(entity);
        } else {
            entity = update(entity);
        }
        if (entity instanceof HasVersion versionedEntity) {
            /*
             * by default, Hibernate doesn't issue an update query and thus version increment
             * if the entity was not modified. to bypass this and always increment the version, we do it manually
             * */
            versionedEntity.setVersion(versionedEntity.getVersion() + 1);
            /*
             * flushing and then removing the entity from the persistence context so that it is not affected
             * by next flushes (e.g. when a transaction is committed) to avoid double version increment
             * */
            entityManager.flush();
            entityManager.detach(versionedEntity);
            flushed = true;
        }
        if (flush && !flushed) {
            entityManager.flush();
        }
        return entity;
    }

    private E create(E entity) {
        if (entity instanceof HasVersion versionedEntity) {
            versionedEntity.setVersion(0L);
        }
        if (entity.getUuid() == null) {
            getEntityManager().persist(entity);
        } else {
            if (entity instanceof HasVersion) {
                /*
                 * Hibernate 6 does not allow creating versioned entities with preset IDs.
                 * Bypassing by calling the underlying session directly
                 * */
                Session session = getEntityManager().unwrap(Session.class);
                session.save(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
        }
        return entity;
    }

    private E update(E entity) {
        if (entity instanceof HasVersion versionedEntity) {
            if (versionedEntity.getVersion() == null) {
                HasVersion existingEntity = entityManager.find(versionedEntity.getClass(), entity.getUuid());
                if (existingEntity != null) {
                    /*
                     * manually resetting the version to latest to allow force overwriting of the entity
                     * */
                    versionedEntity.setVersion(existingEntity.getVersion());
                } else {
                    return create(entity);
                }
            }
            versionedEntity = entityManager.merge(versionedEntity);
            entity = (E) versionedEntity;
        } else {
            entity = entityManager.merge(entity);
        }
        return entity;
    }

    @Override
    @Transactional
    public D saveAndFlush(TenantId tenantId, D domain) {
        return save(tenantId, domain, true);
    }

    @Override
    public D findById(TenantId tenantId, UUID key) {
        log.debug("Get entity by key {}", key);
        Optional<E> entity = getRepository().findById(key);
        return DaoUtil.getData(entity);
    }

    @Override
    public ListenableFuture<D> findByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Get entity by key async {}", key);
        return service.submit(() -> DaoUtil.getData(getRepository().findById(key)));
    }

    @Override
    public boolean existsById(TenantId tenantId, UUID key) {
        log.debug("Exists by key {}", key);
        return getRepository().existsById(key);
    }

    @Override
    public ListenableFuture<Boolean> existsByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Exists by key async {}", key);
        return service.submit(() -> getRepository().existsById(key));
    }

    @Override
    @Transactional
    public void removeById(TenantId tenantId, UUID id) {
        JpaRepository<E, UUID> repository = getRepository();
        repository.deleteById(id);
        repository.flush();
        log.debug("Remove request: {}", id);
    }

    @Override
    @Transactional
    public void removeAllByIds(Collection<UUID> ids) {
        JpaRepository<E, UUID> repository = getRepository();
        ids.forEach(repository::deleteById);
        repository.flush();
    }

    @Override
    public List<D> find(TenantId tenantId) {
        List<E> entities = Lists.newArrayList(getRepository().findAll());
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<UUID> findIdsByTenantIdAndIdOffset(TenantId tenantId, UUID idOffset, int limit) {
        String query = "SELECT id FROM " + getEntityType().getTableName() + " WHERE " + getTenantIdColumn() + " = ? ";
        Object[] params;
        if (idOffset == null) {
            params = new Object[]{tenantId.getId(), limit};
        } else {
            query += " AND id > ? ";
            params = new Object[]{tenantId.getId(), idOffset, limit};
        }
        query += " ORDER BY id LIMIT ?";

        return getJdbcTemplate().queryForList(query, UUID.class, params);
    }

    protected String getTenantIdColumn() {
        return ModelConstants.TENANT_ID_COLUMN;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    protected abstract Class<E> getEntityClass();

    protected abstract JpaRepository<E, UUID> getRepository();

}
