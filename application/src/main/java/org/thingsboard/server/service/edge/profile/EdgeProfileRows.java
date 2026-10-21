// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.EdgeProfileJobRow;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.group.BaseEntityGroupService;

import java.util.ArrayList;
import java.util.List;

public final class EdgeProfileRows {

    public static final String STAMP_KEY = "edgeProfileId";

    private EdgeProfileRows() {
    }

    public record EdgeAssignmentRowRef(EntityId id, EntityType groupType) {

        public static EdgeAssignmentRowRef ruleChain(RuleChainId id) {
            return new EdgeAssignmentRowRef(id, null);
        }

        public static EdgeAssignmentRowRef entityGroup(EntityGroupId id, EntityType groupType) {
            return new EdgeAssignmentRowRef(id, groupType);
        }

        public static EdgeAssignmentRowRef integration(IntegrationId id) {
            return new EdgeAssignmentRowRef(id, null);
        }

        public static EdgeAssignmentRowRef schedulerEvent(SchedulerEventId id) {
            return new EdgeAssignmentRowRef(id, null);
        }

        public EntityType entityType() {
            return id.getEntityType();
        }

        public String relationType() {
            if (EntityType.ENTITY_GROUP.equals(id.getEntityType())) {
                return BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
            }
            return EntityRelation.CONTAINS_TYPE;
        }

    }

    public static ObjectNode stamp(EdgeProfileId edgeProfileId) {
        ObjectNode node = JacksonUtil.newObjectNode();
        node.put(STAMP_KEY, edgeProfileId.getId().toString());
        return node;
    }

    public static boolean isStampedBy(EntityRelation relation, EdgeProfileId edgeProfileId) {
        JsonNode info = relation.getAdditionalInfo();
        return info != null && info.hasNonNull(STAMP_KEY) && edgeProfileId.getId().toString().equals(info.get(STAMP_KEY).asText());
    }

    public static boolean isStamped(EntityRelation relation) {
        JsonNode info = relation.getAdditionalInfo();
        return info != null && info.hasNonNull(STAMP_KEY);
    }

    public static JsonNode unstamped(EntityRelation relation) {
        if (!(relation.getAdditionalInfo() instanceof ObjectNode info)) {
            return relation.getAdditionalInfo();
        }
        ObjectNode copy = info.deepCopy();
        copy.remove(STAMP_KEY);
        return copy.isEmpty() ? null : copy;
    }

    public static EdgeProfileJobRow toJobRow(EdgeAssignmentRowRef row) {
        return new EdgeProfileJobRow(row.id(), row.groupType());
    }

    public static EdgeAssignmentRowRef fromJobRow(EdgeProfileJobRow row) {
        return new EdgeAssignmentRowRef(row.getEntityId(), row.getGroupType());
    }

    public static List<EdgeAssignmentRowRef> rowsOf(EdgeProfile profile) {
        List<EdgeAssignmentRowRef> rows = new ArrayList<>();
        for (RuleChainId id : profile.getProfileData().getRuleChainIds()) {
            rows.add(EdgeAssignmentRowRef.ruleChain(id));
        }
        for (EdgeProfileEntityGroup g : profile.getProfileData().getEntityGroups()) {
            rows.add(EdgeAssignmentRowRef.entityGroup(g.getEntityGroupId(), g.getGroupType()));
        }
        for (IntegrationId id : profile.getProfileData().getIntegrationIds()) {
            rows.add(EdgeAssignmentRowRef.integration(id));
        }
        for (SchedulerEventId id : profile.getProfileData().getSchedulerEventIds()) {
            rows.add(EdgeAssignmentRowRef.schedulerEvent(id));
        }
        return rows;
    }

    public static EntityId ownerOf(TenantId tenantId, CustomerId customerId) {
        if (customerId == null || customerId.isNullUid()) {
            return tenantId;
        }
        return customerId;
    }

}
