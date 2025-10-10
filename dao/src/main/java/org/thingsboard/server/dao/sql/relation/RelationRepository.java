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
package org.thingsboard.server.dao.sql.relation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationRepository
        extends JpaRepository<RelationEntity, RelationCompositeKey>, JpaSpecificationExecutor<RelationEntity> {

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeGroup(UUID fromId,
                                                                        String fromType,
                                                                        String relationTypeGroup);

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeGroupIn(UUID fromId,
                                                                          String fromType,
                                                                          List<String> relationTypeGroups);

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(UUID fromId,
                                                                                       String fromType,
                                                                                       String relationType,
                                                                                       String relationTypeGroup);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeGroup(UUID toId,
                                                                    String toType,
                                                                    String relationTypeGroup);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeGroupIn(UUID toId,
                                                                      String toType,
                                                                      List<String> relationTypeGroups);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(UUID toId,
                                                                                   String toType,
                                                                                   String relationType,
                                                                                   String relationTypeGroup);

    @Query("SELECT r FROM RelationEntity r WHERE " +
            "r.relationTypeGroup = 'RULE_NODE' AND r.toType = 'RULE_CHAIN' " +
            "AND r.toId in (SELECT id from RuleChainEntity where type = :ruleChainType )")
    List<RelationEntity> findRuleNodeToRuleChainRelations(@Param("ruleChainType") RuleChainType ruleChainType, Pageable page);

    @Transactional
    <S extends RelationEntity> S save(S entity);

    @Transactional
    void deleteById(RelationCompositeKey id);

    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.fromId = :fromId and r.fromType = :fromType")
    void deleteByFromIdAndFromType(@Param("fromId") UUID fromId, @Param("fromType") String fromType);

    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.toId = :toId and r.toType = :toType and r.relationTypeGroup in :relationTypeGroups")
    void deleteByToIdAndToTypeAndRelationTypeGroupIn(@Param("toId") UUID toId, @Param("toType") String toType, @Param("relationTypeGroups") List<String> relationTypeGroups);

    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.fromId = :fromId and r.fromType = :fromType and r.relationTypeGroup in :relationTypeGroups")
    void deleteByFromIdAndFromTypeAndRelationTypeGroupIn(@Param("fromId") UUID fromId, @Param("fromType") String fromType, @Param("relationTypeGroups") List<String> relationTypeGroups);

    @Query(value = "SELECT from_id, from_type, relation_type_group, relation_type, to_id, to_type, additional_info, version FROM relation" +
            " WHERE (from_id, from_type, relation_type_group, relation_type, to_id, to_type) > " +
            "(:fromId, :fromType, :relationTypeGroup, :relationType, :toId, :toType) ORDER BY " +
            "from_id, from_type, relation_type_group, relation_type, to_id, to_type LIMIT :batchSize", nativeQuery = true)
    List<RelationEntity> findNextBatch(@Param("fromId") UUID fromId,
                                       @Param("fromType") String fromType,
                                       @Param("relationTypeGroup") String relationTypeGroup,
                                       @Param("relationType") String relationType,
                                       @Param("toId") UUID toId,
                                       @Param("toType") String toType,
                                       @Param("batchSize") int batchSize);

    @Query(value = """
                SELECT r.from_id, r.from_type, r.relation_type_group, r.relation_type, r.to_id, r.to_type, r.additional_info, r.version
                FROM relation r
                LEFT JOIN device d ON r.to_id = d.id AND r.to_type = 'DEVICE'
                LEFT JOIN asset a  ON r.to_id = a.id AND r.to_type = 'ASSET'
                WHERE r.from_id = :fromId
                  AND r.from_type = :fromType
                  AND r.relation_type = :relationType
                  AND r.relation_type_group = :relationTypeGroup
                  AND ((d.device_profile_id = :profileId) OR (a.asset_profile_id = :profileId))
                  AND (d.id IS NOT NULL OR a.id IS NOT NULL)
            """, nativeQuery = true)
    List<RelationEntity> findByFromAndProfile(@Param("fromId") UUID fromId,
                                              @Param("fromType") String fromType,
                                              @Param("relationTypeGroup") String relationTypeGroup,
                                              @Param("relationType") String relationType,
                                              @Param("profileId") UUID profileId);

    @Query(value = """
                SELECT r.from_id, r.from_type, r.relation_type_group, r.relation_type, r.to_id, r.to_type, r.additional_info, r.version
                FROM relation r
                LEFT JOIN device d ON r.from_id = d.id AND r.from_type = 'DEVICE'
                LEFT JOIN asset a ON r.from_id = a.id AND r.from_type = 'ASSET'
                WHERE r.to_id = :toId
                  AND r.to_type = :toType
                  AND r.relation_type = :relationType
                  AND r.relation_type_group = :relationTypeGroup
                  AND ((d.device_profile_id = :profileId) OR (a.asset_profile_id = :profileId))
                  AND (d.id IS NOT NULL OR a.id IS NOT NULL)
                LIMIT 1
            """, nativeQuery = true)
    Optional<RelationEntity> findByToAndProfile(@Param("toId") UUID toId,
                                                @Param("toType") String toType,
                                                @Param("relationTypeGroup") String relationTypeGroup,
                                                @Param("relationType") String relationType,
                                                @Param("profileId") UUID profileId);
}
