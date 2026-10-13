// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public class AgentBulkAction extends BaseData<AgentBulkActionId> implements HasId<AgentBulkActionId>, HasTenantId, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_BULK_ACTION;
    }

    private TenantId tenantId;
    private UUID agentProfileId;
    private UUID applicationProfileId;
    private AgentAppEventActionType actionType;
    private AgentBulkActionStatus status;
    private String errorMsg;
    private Long processingStartedTime;
    private int total;
    private int submitted;
    private Map<SkipReason, Integer> skipCounts;

    public AgentBulkAction() {
        super();
    }

    public AgentBulkAction(AgentBulkActionId id) {
        super(id);
    }

    public AgentBulkAction(AgentBulkAction action) {
        super(action);
        this.tenantId = action.getTenantId();
        this.agentProfileId = action.getAgentProfileId();
        this.applicationProfileId = action.getApplicationProfileId();
        this.actionType = action.getActionType();
        this.status = action.getStatus();
        this.errorMsg = action.getErrorMsg();
        this.processingStartedTime = action.getProcessingStartedTime();
        this.total = action.getTotal();
        this.submitted = action.getSubmitted();
        this.skipCounts = action.getSkipCounts();
    }

    @Schema(description = "JSON object with the Agent Bulk Action Id.")
    @Override
    public AgentBulkActionId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the bulk action creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
}
