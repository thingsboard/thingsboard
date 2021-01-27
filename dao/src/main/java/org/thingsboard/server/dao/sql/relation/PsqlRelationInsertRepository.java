/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.relation;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.util.PsqlDao;

@PsqlDao
@Repository
@Transactional
public class PsqlRelationInsertRepository extends AbstractRelationInsertRepository implements RelationInsertRepository {

    private static final String INSERT_ON_CONFLICT_DO_UPDATE = "INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)" +
            " VALUES (:fromId, :fromType, :toId, :toType, :relationTypeGroup, :relationType, :additionalInfo) " +
            "ON CONFLICT (from_id, from_type, relation_type_group, relation_type, to_id, to_type) DO UPDATE SET additional_info = :additionalInfo returning *";

    @Override
    public RelationEntity saveOrUpdate(RelationEntity entity) {
        return processSaveOrUpdate(entity);
    }

    @Override
    protected RelationEntity processSaveOrUpdate(RelationEntity entity) {
        return (RelationEntity) getQuery(entity, INSERT_ON_CONFLICT_DO_UPDATE).getSingleResult();
    }
}
