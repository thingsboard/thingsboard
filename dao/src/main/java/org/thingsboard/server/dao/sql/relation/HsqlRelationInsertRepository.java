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
package org.thingsboard.server.dao.sql.relation;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@HsqlDao
@SqlDao
@Repository
@Transactional
public class HsqlRelationInsertRepository extends AbstractRelationInsertRepository implements RelationInsertRepository {

    private static final String INSERT_ON_CONFLICT_DO_UPDATE = "MERGE INTO relation USING (VALUES :fromId, :fromType, :toId, :toType, :relationTypeGroup, :relationType, :additionalInfo) R " +
            "(from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info) " +
            "ON (relation.from_id = R.from_id AND relation.from_type = R.from_type AND relation.relation_type_group = R.relation_type_group AND relation.relation_type = R.relation_type AND relation.to_id = R.to_id AND relation.to_type = R.to_type) " +
            "WHEN MATCHED THEN UPDATE SET relation.additional_info = R.additional_info " +
            "WHEN NOT MATCHED THEN INSERT (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info) VALUES (R.from_id, R.from_type, R.to_id, R.to_type, R.relation_type_group, R.relation_type, R.additional_info)";

    @Override
    public RelationEntity saveOrUpdate(RelationEntity entity) {
        return processSaveOrUpdate(entity);
    }

    @Override
    protected RelationEntity processSaveOrUpdate(RelationEntity entity) {
        getQuery(entity, INSERT_ON_CONFLICT_DO_UPDATE).executeUpdate();
        return entityManager.find(RelationEntity.class, new RelationCompositeKey(entity.toData()));
    }
}
