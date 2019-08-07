/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.attributes;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@SqlDao
@Repository
public class AttributeKvInsertRepository {

    private static final String INSERT_OR_UPDATE_LONG_VALUE = "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, long_v, last_update_ts) VALUES (:entity_type, :entity_id, :attribute_type, :attribute_key, :long_v, :last_update_ts) ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO UPDATE SET long_v = :long_v";

    private static final String INSERT_OR_UPDATE_BOOL_VALUE = "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, bool_v, last_update_ts) VALUES (:entity_type, :entity_id, :attribute_type, :attribute_key, :bool_v, :last_update_ts) ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO UPDATE SET bool_v = :bool_v";

    private static final String INSERT_OR_UPDATE_DBL_VALUE = "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, dbl_v, last_update_ts) VALUES (:entity_type, :entity_id, :attribute_type, :attribute_key, :dbl_v, :last_update_ts) ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO UPDATE SET dbl_v = :dbl_v";

    private static final String INSERT_OR_UPDATE_STR_VALUE = "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, str_v, last_update_ts) VALUES (:entity_type, :entity_id, :attribute_type, :attribute_key, :str_v, :last_update_ts) ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO UPDATE SET str_v = :str_v";

    @PersistenceContext
    private EntityManager entityManager;

    @Modifying
    @Transactional
    public void saveOrUpdate(AttributeKvEntity entity) {
        if (entity.getBooleanValue() != null) {
            saveBoolean(entity);
        }
        if (entity.getStrValue() != null) {
            saveString(entity);
        }
        if (entity.getLongValue() != null) {
            saveLong(entity);
        }
        if (entity.getDoubleValue() != null) {
            saveDouble(entity);
        }
    }

    private void saveString(AttributeKvEntity entity) {
        entityManager.createNativeQuery(INSERT_OR_UPDATE_STR_VALUE)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("str_v", entity.getStrValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

    private void saveLong(AttributeKvEntity entity) {
        entityManager.createNativeQuery(INSERT_OR_UPDATE_LONG_VALUE)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("long_v", entity.getLongValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

    private void saveBoolean(AttributeKvEntity entity) {
        entityManager.createNativeQuery(INSERT_OR_UPDATE_BOOL_VALUE)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("bool_v", entity.getBooleanValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

    private void saveDouble(AttributeKvEntity entity) {
        entityManager.createNativeQuery(INSERT_OR_UPDATE_DBL_VALUE)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("dbl_v", entity.getDoubleValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }
}