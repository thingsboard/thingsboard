// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edge;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema
@Data
@NoArgsConstructor
public class EdgeProfileData implements Serializable {

    @Serial
    private static final long serialVersionUID = -2847133622057230671L;

    // every group type an edge profile may carry; no consumer keeps a list of its own
    public static final Set<EntityType> SYNCABLE_GROUP_TYPES =
            EnumSet.of(EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.USER);

    // a new kind of row needs a field below, an entry here and a branch in removeReferencesTo
    public static final Set<EntityType> REFERENCED_ENTITY_TYPES =
            EnumSet.of(EntityType.RULE_CHAIN, EntityType.ENTITY_GROUP, EntityType.INTEGRATION, EntityType.SCHEDULER_EVENT);

    // An explicit JSON null must not replace the lists: every consumer iterates them without a null check.
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<EdgeProfileEntityGroup> entityGroups = new ArrayList<>();
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<RuleChainId> ruleChainIds = new ArrayList<>();
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<IntegrationId> integrationIds = new ArrayList<>();
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<SchedulerEventId> schedulerEventIds = new ArrayList<>();

    public EdgeProfileData(EdgeProfileData other) {
        this.entityGroups = new ArrayList<>();
        for (EdgeProfileEntityGroup row : other.getEntityGroups()) {
            this.entityGroups.add(new EdgeProfileEntityGroup(row.getGroupType(), row.getEntityGroupId()));
        }
        this.ruleChainIds = new ArrayList<>(other.getRuleChainIds());
        this.integrationIds = new ArrayList<>(other.getIntegrationIds());
        this.schedulerEventIds = new ArrayList<>(other.getSchedulerEventIds());
    }

    public boolean removeReferencesTo(EntityId entityId) {
        if (entityId == null) {
            return false;
        }
        UUID id = entityId.getId();
        return ruleChainIds.removeIf(rowId -> rowId.getId().equals(id))
                | integrationIds.removeIf(rowId -> rowId.getId().equals(id))
                | schedulerEventIds.removeIf(rowId -> rowId.getId().equals(id))
                | entityGroups.removeIf(group -> group.getEntityGroupId() != null && group.getEntityGroupId().getId().equals(id));
    }

}
