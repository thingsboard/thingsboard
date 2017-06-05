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

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.CassandraAbstractSearchTimeDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

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

    @Query(nativeQuery = true, value = "SELECT * FROM DEVICE WHERE TENANT_ID = :tenantId " +
            "AND CUSTOMER_ID = :customerId " +
            "AND LOWER(SEARCH_TEXT) LIKE LOWER(CONCAT(:searchText, '%')) " +
            "AND ID > :idOffset ORDER BY ID LIMIT :limit")
    List<RelationEntity> findRelations(@Param("fromId") UUID fromId,
                                       @Param("fromType") String fromType,
                                       @Param("toType") String toType,
                                       @Param("relationType") String relationType,
                                       @Param("relationTypeGroup") String relationTypeGroup,
                                       TimePageLink pageLink);


//    Select.Where query = CassandraAbstractSearchTimeDao.buildQuery(ModelConstants.RELATION_BY_TYPE_AND_CHILD_TYPE_VIEW_NAME,
//            Arrays.asList(eq(ModelConstants.RELATION_FROM_ID_PROPERTY, from.getId()),
//                    eq(ModelConstants.RELATION_FROM_TYPE_PROPERTY, from.getEntityType().name()),
//                    eq(ModelConstants.RELATION_TYPE_GROUP_PROPERTY, typeGroup.name()),
//                    eq(ModelConstants.RELATION_TYPE_PROPERTY, relationType),
//                    eq(ModelConstants.RELATION_TO_TYPE_PROPERTY, childType.name())),
//            Arrays.asList(QueryBuilder.asc(ModelConstants.RELATION_TYPE_GROUP_PROPERTY),
//                    QueryBuilder.asc(ModelConstants.RELATION_TYPE_PROPERTY),
//                    QueryBuilder.asc(ModelConstants.RELATION_TO_TYPE_PROPERTY)),
//            pageLink, ModelConstants.RELATION_TO_ID_PROPERTY);
}

//    private UUID fromId;
//    private String fromType;
//    private UUID toId;
//    private String toType;
//    private String relationTypeGroup;
//    private String relationType;
//
//
//    ListenableFuture<Boolean> checkRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);
//
//    ListenableFuture<Boolean> saveRelation(EntityRelation relation);
//
//    ListenableFuture<Boolean> deleteRelation(EntityRelation relation);
//
//    ListenableFuture<Boolean> deleteRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);
//
//    ListenableFuture<Boolean> deleteOutboundRelations(EntityId entity);
//
//    ListenableFuture<List<EntityRelation>> findRelations(EntityId from, String relationType, RelationTypeGroup typeGroup, EntityType toType, TimePageLink pageLink);