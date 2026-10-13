// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationDao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CitusDaoSqlTest
public class CitusRelationServiceTest extends RelationServiceTest {

    @Autowired
    private RelationDao relationDao;

    /**
     * End-to-end contract for the per-row relation version under Citus (where the global
     * {@code relation_version_seq} is replaced by an INSERT version of 1 plus an
     * {@code ON CONFLICT ... version = relation.version + 1} bump): the first save of a relation key
     * yields version 1, each subsequent save of the same key increments it, and deleting then
     * re-creating the relation restarts the version at 1 (a brand-new row, not a continuation of the
     * old per-row counter). This exercises the real RelationService -> SqlRelationInsertRepository path
     * against a live Citus cluster, complementing the SQL-shape unit test SqlRelationInsertRepositoryCitusSqlTest.
     */
    @Test
    public void testPerRowVersionContract() {
        AssetId fromId = new AssetId(Uuids.timeBased());
        AssetId toId = new AssetId(Uuids.timeBased());
        EntityRelation relation = new EntityRelation(fromId, toId, EntityRelation.CONTAINS_TYPE);

        Assert.assertEquals("First save must set per-row version to 1",
                Long.valueOf(1), versionOfSave(relation));
        Assert.assertEquals("Second save (ON CONFLICT) must bump per-row version to 2",
                Long.valueOf(2), versionOfSave(relation));
        Assert.assertEquals("Third save (ON CONFLICT) must bump per-row version to 3",
                Long.valueOf(3), versionOfSave(relation));

        // the persisted (read-back) version matches the last write
        Assert.assertEquals("Read-back version must equal the last written per-row version",
                Long.valueOf(3), versionOfRead(fromId, toId));

        // the Citus delete RETURNING yields the row's own stored version (no nextval), which is what
        // feeds the RELATION_DELETED event / EDQS versioning
        EntityRelation deleted = relationDao.deleteRelation(SYSTEM_TENANT_ID, relation);
        Assert.assertNotNull("deleteRelation must return the deleted relation", deleted);
        Assert.assertEquals("Deleted relation must carry the row's last written per-row version",
                Long.valueOf(3), deleted.getVersion());

        Assert.assertEquals("Per-row version must restart at 1 after delete + recreate",
                Long.valueOf(1), versionOfSave(relation));
    }

    /**
     * Same per-row version contract for the bulk delete paths: the {@code DELETE ... RETURNING} of
     * {@link RelationDao#deleteOutboundRelations} / {@link RelationDao#deleteInboundRelations} under Citus must
     * return each deleted row's own stored version (not a fresh sequence value), since those versions feed the
     * RELATION_DELETED events.
     */
    @Test
    public void testBulkDeletedRelationsCarryStoredPerRowVersions() {
        AssetId fromId = new AssetId(Uuids.timeBased());
        AssetId firstToId = new AssetId(Uuids.timeBased());
        AssetId secondToId = new AssetId(Uuids.timeBased());
        EntityRelation firstRelation = new EntityRelation(fromId, firstToId, EntityRelation.CONTAINS_TYPE);
        EntityRelation secondRelation = new EntityRelation(fromId, secondToId, EntityRelation.CONTAINS_TYPE);

        relationService.saveRelation(SYSTEM_TENANT_ID, firstRelation);
        relationService.saveRelation(SYSTEM_TENANT_ID, firstRelation);
        relationService.saveRelation(SYSTEM_TENANT_ID, secondRelation);

        List<EntityRelation> deletedOutbound = relationDao.deleteOutboundRelations(SYSTEM_TENANT_ID, fromId);
        Map<EntityId, Long> versionsByTo = deletedOutbound.stream()
                .collect(Collectors.toMap(EntityRelation::getTo, EntityRelation::getVersion));
        Assert.assertEquals("Both outbound relations must be deleted", 2, versionsByTo.size());
        Assert.assertEquals("Twice-saved relation must be returned with its stored per-row version",
                Long.valueOf(2), versionsByTo.get(firstToId));
        Assert.assertEquals("Once-saved relation must be returned with its stored per-row version",
                Long.valueOf(1), versionsByTo.get(secondToId));

        AssetId inboundTargetId = new AssetId(Uuids.timeBased());
        EntityRelation inboundRelation = new EntityRelation(fromId, inboundTargetId, EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(SYSTEM_TENANT_ID, inboundRelation);
        relationService.saveRelation(SYSTEM_TENANT_ID, inboundRelation);

        List<EntityRelation> deletedInbound = relationDao.deleteInboundRelations(SYSTEM_TENANT_ID, inboundTargetId);
        Assert.assertEquals("The inbound relation must be deleted", 1, deletedInbound.size());
        Assert.assertEquals("Inbound bulk delete must return the stored per-row version",
                Long.valueOf(2), deletedInbound.get(0).getVersion());
    }

    private Long versionOfSave(EntityRelation relation) {
        EntityRelation saved = relationService.saveRelation(SYSTEM_TENANT_ID, relation);
        Assert.assertNotNull("saveRelation must return the persisted relation", saved);
        return saved.getVersion();
    }

    private Long versionOfRead(EntityId from, EntityId to) {
        EntityRelation read = relationService.getRelation(SYSTEM_TENANT_ID, from, to, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertNotNull("Relation must exist after save", read);
        return read.getVersion();
    }
}
