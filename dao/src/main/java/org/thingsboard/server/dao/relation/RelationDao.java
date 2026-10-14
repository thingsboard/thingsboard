// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.relation;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.util.List;

/**
 * Created by ashvayka on 25.04.17.
 *
 * <p><b>Write-path convention.</b> Every relation-mutating path (implemented here and orchestrated by the relation
 * service) must run inside a {@code @Transactional} boundary and take the appropriate {@link RelationWriteLock} before
 * its DML — the advisory lock and the DML must share one transaction/connection. See {@link RelationWriteLock} for the
 * full locking contract (per-endpoint {@code lockEntities}, covering locks, lock-acquisition order, and the transaction
 * assertion that fails fast in both Citus and plain-PostgreSQL modes).
 */
public interface RelationDao {

    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup);

    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from);

    List<EntityRelation> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup);

    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup);

    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to);

    List<EntityRelation> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    EntityRelation saveRelation(TenantId tenantId, EntityRelation relation);

    List<EntityRelation> saveRelations(TenantId tenantId, List<EntityRelation> relations);

    ListenableFuture<EntityRelation> saveRelationAsync(TenantId tenantId, EntityRelation relation);

    EntityRelation deleteRelation(TenantId tenantId, EntityRelation relation);

    ListenableFuture<EntityRelation> deleteRelationAsync(TenantId tenantId, EntityRelation relation);

    EntityRelation deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<EntityRelation> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    List<EntityRelation> deleteOutboundRelations(TenantId tenantId, EntityId entity);

    List<EntityRelation> deleteOutboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup);

    List<EntityRelation> deleteInboundRelations(TenantId tenantId, EntityId entity);

    List<EntityRelation> deleteInboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup);

    List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit);

    List<EntityRelation> findByRelationPathQuery(TenantId tenantId, EntityRelationPathQuery relationPathQuery, int limit);

}
