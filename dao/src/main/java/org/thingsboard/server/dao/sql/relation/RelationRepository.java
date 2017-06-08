/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;

import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true")
public interface RelationRepository extends CrudRepository<RelationEntity, RelationCompositeKey> {

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeGroup(UUID fromId,
                                                                        String fromType,
                                                                        String relationTypeGroup);

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(UUID fromId,
                                                                                       String fromType,
                                                                                       String relationType,
                                                                                       String relationTypeGroup);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeGroup(UUID toId,
                                                                    String toType,
                                                                    String relationTypeGroup);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(UUID toId,
                                                                                   String toType,
                                                                                   String relationType,
                                                                                   String relationTypeGroup);

    List<RelationEntity> findAllByFromIdAndFromType(UUID fromId,
                                                    String fromType);

//    @Query(nativeQuery = true, value = "SELECT * FROM RELATION WHERE FROM_ID = :fromId " +
//            "AND FROM_TYPE = :fromType " +
//            "AND TO_TYPE = :toType " +
//            "AND RELATION_TYPE = :relationType " +
//            "AND RELATION_TYPE_GROUP = :relationTypeGroup " +
//            "AND ID > :idOffset ORDER BY RELATION_TYPE_GROUP ASC, RELATION_TYPE ASC, TO_TYPE ASC")
//    List<RelationEntity> findRelations(@Param("fromId") UUID fromId,
//                                       @Param("fromType") String fromType,
//                                       @Param("toType") String toType,
//                                       @Param("relationType") String relationType,
//                                       @Param("relationTypeGroup") String relationTypeGroup,
//                                       TimePageLink pageLink);


//            pageLink, ModelConstants.RELATION_TO_ID_PROPERTY);
}
