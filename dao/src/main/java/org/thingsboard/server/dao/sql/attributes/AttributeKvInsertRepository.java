/**
 * Copyright © 2016-2019 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.attributes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

//@Slf4j
//@SqlDao
//@Repository
public class AttributeKvInsertRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveIfNotExist(AttributeKvEntity entity) {
        entityManager.createNativeQuery("INSERT INTO attribute_kv" +
                " (entity_type, entity_id, attribute_type, attribute_key, bool_v, str_v, long_v, dbl_v, last_update_ts) " +
                "VALUES (:entity_type, :entity_id, :attribute_type, :attribute_key, cast(:bool_v AS boolean)," +
                " :str_v, cast(:long_v AS bigint), cast(:dbl_v AS double precision), cast(:last_update_ts AS bigint))" +
                "ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO NOTHING;")
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("bool_v", entity.getBooleanValue())
                .setParameter("str_v", entity.getStrValue())
                .setParameter("long_v", entity.getLongValue())
                .setParameter("dbl_v", entity.getDoubleValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

}