// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.audit;

import lombok.Getter;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.Optional;

public enum ActionType {

    /**
     * Entity created. Pushes {@link TbMsgType#ENTITY_CREATED} to rule engine.
     * Audit log payload: full entity JSON.
     */
    ADDED(TbMsgType.ENTITY_CREATED),
    /**
     * Entity deleted. Pushes {@link TbMsgType#ENTITY_DELETED} to rule engine.
     * Audit log payload: entity string id.
     */
    DELETED(TbMsgType.ENTITY_DELETED),
    /**
     * Entity updated. Pushes {@link TbMsgType#ENTITY_UPDATED} to rule engine.
     * Audit log payload: full entity JSON.
     */
    UPDATED(TbMsgType.ENTITY_UPDATED),
    /**
     * Server-side or shared attributes updated via API.
     * Pushes {@link TbMsgType#ATTRIBUTES_UPDATED} to rule engine.
     * Rule engine msg metadata includes {@code scope} ({@code SERVER_SCOPE} or {@code SHARED_SCOPE}).
     * Rule engine msg data: key-value pairs of the updated attributes.
     * Audit log payload: updated attributes and their values.
     */
    ATTRIBUTES_UPDATED(TbMsgType.ATTRIBUTES_UPDATED),
    /**
     * Attributes deleted via API.
     * Pushes {@link TbMsgType#ATTRIBUTES_DELETED} to rule engine.
     * Rule engine msg metadata includes {@code scope} ({@code SERVER_SCOPE} or {@code SHARED_SCOPE}).
     * Rule engine msg data: {@code {"attributes": ["key1", "key2"]}}.
     * Audit log payload: list of deleted attribute keys.
     */
    ATTRIBUTES_DELETED(TbMsgType.ATTRIBUTES_DELETED),
    /**
     * Timeseries data saved via API (not from device transport).
     * Pushes {@link TbMsgType#TIMESERIES_UPDATED} to rule engine.
     * Rule engine msg data: {@code {"timeseries": [{"ts": ..., "values": {...}}, ...]}}.
     * Audit log payload: timeseries entries.
     */
    TIMESERIES_UPDATED(TbMsgType.TIMESERIES_UPDATED),
    /**
     * Timeseries data deleted via API.
     * Pushes {@link TbMsgType#TIMESERIES_DELETED} to rule engine.
     * Rule engine msg data: {@code {"timeseries": ["key1", ...], "startTs": ..., "endTs": ...}}.
     * Audit log payload: deleted timeseries keys.
     */
    TIMESERIES_DELETED(TbMsgType.TIMESERIES_DELETED),
    /**
     * RPC call to device. Does not push to rule engine (RPC has its own lifecycle messages).
     * Audit log payload: RPC method and params.
     */
    RPC_CALL,
    /**
     * Device credentials updated. Does not push to rule engine.
     * Audit log payload: new credentials value.
     */
    CREDENTIALS_UPDATED,
    /**
     * Entity assigned to a customer. Pushes {@link TbMsgType#ENTITY_ASSIGNED} to rule engine.
     * Rule engine msg metadata includes {@code assignedCustomerId} and {@code assignedCustomerName}.
     * Audit log payload: customer name.
     */
    ASSIGNED_TO_CUSTOMER(TbMsgType.ENTITY_ASSIGNED),
    /**
     * Entity unassigned from a customer. Pushes {@link TbMsgType#ENTITY_UNASSIGNED} to rule engine.
     * Rule engine msg metadata includes {@code unassignedCustomerId} and {@code unassignedCustomerName}.
     * Audit log payload: customer name.
     */
    UNASSIGNED_FROM_CUSTOMER(TbMsgType.ENTITY_UNASSIGNED),
    /**
     * Entity ownership changed (PE only). Pushes {@link TbMsgType#OWNER_CHANGED} to rule engine.
     * Rule engine msg metadata includes {@code targetOwnerId} and {@code targetOwnerType}.
     * Audit log payload: new owner info.
     */
    CHANGE_OWNER(TbMsgType.OWNER_CHANGED),
    /**
     * User account or integration activated. Does not push to rule engine.
     * Audit log payload: entity string id.
     */
    ACTIVATED,
    /**
     * User account or integration suspended. Does not push to rule engine.
     * Audit log payload: entity string id.
     */
    SUSPENDED,
    /**
     * Device credentials read. Read-only action. Does not push to rule engine.
     * Audit log payload: device id.
     */
    CREDENTIALS_READ(true),
    /**
     * Attributes read. Read-only action. Does not push to rule engine.
     * Audit log payload: attribute keys read.
     */
    ATTRIBUTES_READ(true),
    /**
     * Relation created or updated. Pushes {@link TbMsgType#RELATION_ADD_OR_UPDATE} to rule engine.
     * Rule engine msg data: relation JSON ({@code from}, {@code to}, {@code type}, {@code typeGroup}).
     */
    RELATION_ADD_OR_UPDATE(TbMsgType.RELATION_ADD_OR_UPDATE),
    /**
     * Relation deleted. Pushes {@link TbMsgType#RELATION_DELETED} to rule engine.
     * Rule engine msg data: relation JSON ({@code from}, {@code to}, {@code type}, {@code typeGroup}).
     */
    RELATION_DELETED(TbMsgType.RELATION_DELETED),
    /**
     * All relations for an entity deleted. Pushes {@link TbMsgType#RELATIONS_DELETED} to rule engine.
     * Rule engine msg data: empty JSON object.
     */
    RELATIONS_DELETED(TbMsgType.RELATIONS_DELETED),
    /**
     * Alarm acknowledged by a user. Pushes {@link TbMsgType#ALARM_ACK} to rule engine.
     * Rule engine msg data: full alarm JSON. Originator: alarm id.
     */
    ALARM_ACK(TbMsgType.ALARM_ACK, true),
    /**
     * Alarm cleared by a user. Pushes {@link TbMsgType#ALARM_CLEAR} to rule engine.
     * Rule engine msg data: full alarm JSON. Originator: alarm id.
     */
    ALARM_CLEAR(TbMsgType.ALARM_CLEAR, true),
    /**
     * Alarm deleted by a user. Pushes {@link TbMsgType#ALARM_DELETE} to rule engine.
     * Rule engine msg data: full alarm JSON. Originator: alarm id.
     */
    ALARM_DELETE(TbMsgType.ALARM_DELETE, true),
    /**
     * Alarm assigned to a user. Pushes {@link TbMsgType#ALARM_ASSIGNED} to rule engine.
     * Rule engine msg data: full alarm JSON. Originator: alarm id.
     */
    ALARM_ASSIGNED(TbMsgType.ALARM_ASSIGNED, true),
    /**
     * Alarm unassigned from a user. Pushes {@link TbMsgType#ALARM_UNASSIGNED} to rule engine.
     * Rule engine msg data: full alarm JSON. Originator: alarm id.
     */
    ALARM_UNASSIGNED(TbMsgType.ALARM_UNASSIGNED, true),
    /**
     * Entity added to an entity group (PE only). Pushes {@link TbMsgType#ADDED_TO_ENTITY_GROUP} to rule engine.
     * Rule engine msg metadata includes {@code addedToEntityGroupId} and {@code addedToEntityGroupName}.
     * Audit log payload: entity group name.
     */
    ADDED_TO_ENTITY_GROUP(TbMsgType.ADDED_TO_ENTITY_GROUP),
    /**
     * Entity removed from an entity group (PE only). Pushes {@link TbMsgType#REMOVED_FROM_ENTITY_GROUP} to rule engine.
     * Rule engine msg metadata includes {@code removedFromEntityGroupId} and {@code removedFromEntityGroupName}.
     * Audit log payload: entity group name.
     */
    REMOVED_FROM_ENTITY_GROUP(TbMsgType.REMOVED_FROM_ENTITY_GROUP),
    /**
     * REST API call to rule engine. Does not push to rule engine directly
     * (the REST controller creates a {@link TbMsgType#REST_API_REQUEST} message itself).
     * Audit log payload: call details.
     */
    REST_API_RULE_ENGINE_CALL,
    /**
     * Entity made public via entity group (PE only). Does not push to rule engine.
     * Audit log payload: entity group name.
     */
    MADE_PUBLIC,
    /**
     * Entity made private via entity group (PE only). Does not push to rule engine.
     * Audit log payload: entity group name.
     */
    MADE_PRIVATE,
    /**
     * User logged in. Does not push to rule engine.
     */
    LOGIN,
    /**
     * User logged out. Does not push to rule engine.
     */
    LOGOUT,
    /**
     * User account locked out due to too many failed login attempts. Does not push to rule engine.
     */
    LOCKOUT,
    /**
     * Entity assigned from another tenant (incoming side of cross-tenant transfer).
     * Pushes {@link TbMsgType#ENTITY_ASSIGNED_FROM_TENANT} to rule engine.
     * Rule engine msg metadata includes {@code assignedFromTenantId} and {@code assignedFromTenantName}.
     */
    ASSIGNED_FROM_TENANT(TbMsgType.ENTITY_ASSIGNED_FROM_TENANT),
    /**
     * Entity assigned to another tenant (outgoing side of cross-tenant transfer).
     * Pushes {@link TbMsgType#ENTITY_ASSIGNED_TO_TENANT} to rule engine.
     * Rule engine msg metadata includes {@code assignedToTenantId} and {@code assignedToTenantName}.
     */
    ASSIGNED_TO_TENANT(TbMsgType.ENTITY_ASSIGNED_TO_TENANT),
    /**
     * Device provisioned successfully. Pushes {@link TbMsgType#PROVISION_SUCCESS} to rule engine.
     * Rule engine msg data: full device JSON.
     */
    PROVISION_SUCCESS(TbMsgType.PROVISION_SUCCESS),
    /**
     * Device provisioning failed. Pushes {@link TbMsgType#PROVISION_FAILURE} to rule engine.
     * Rule engine msg data: full device JSON.
     */
    PROVISION_FAILURE(TbMsgType.PROVISION_FAILURE),
    /**
     * Entity assigned to an Edge instance. Pushes {@link TbMsgType#ENTITY_ASSIGNED_TO_EDGE} to rule engine.
     * Rule engine msg metadata includes {@code assignedEdgeId} and {@code assignedEdgeName}.
     * Audit log payload: edge name.
     */
    ASSIGNED_TO_EDGE(TbMsgType.ENTITY_ASSIGNED_TO_EDGE),
    /**
     * Entity unassigned from an Edge instance. Pushes {@link TbMsgType#ENTITY_UNASSIGNED_FROM_EDGE} to rule engine.
     * Rule engine msg metadata includes {@code unassignedEdgeId} and {@code unassignedEdgeName}.
     */
    UNASSIGNED_FROM_EDGE(TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE),
    /**
     * Comment added to an alarm. Pushes {@link TbMsgType#COMMENT_CREATED} to rule engine.
     * Rule engine msg metadata includes {@code comment} (JSON string of the AlarmComment object).
     * Rule engine msg data: full alarm JSON. Originator: alarm id.
     */
    ADDED_COMMENT(TbMsgType.COMMENT_CREATED),
    /**
     * Alarm comment updated. Pushes {@link TbMsgType#COMMENT_UPDATED} to rule engine.
     * Rule engine msg metadata includes {@code comment} (JSON string of the AlarmComment object).
     * Rule engine msg data: full alarm JSON. Originator: alarm id.
     */
    UPDATED_COMMENT(TbMsgType.COMMENT_UPDATED),
    /**
     * Alarm comment deleted. Does not push to rule engine.
     */
    DELETED_COMMENT,
    /**
     * SMS sent. Does not push to rule engine.
     */
    SMS_SENT,
    /**
     * Non-production mode re-confirmed by a system administrator. Does not push to rule engine.
     */
    NON_PRODUCTION_CONFIRMED,
    /**
     * Community Grant instance checker run on the offline path. Nothing in this product writes it: Community
     * Grant enrollment is a Community Edition path.
     * <p>
     * It is declared here because audit rows outlive the product they were written by. A deployment converted
     * from CE keeps its audit_log across the conversion, {@code AuditLogEntity} maps this enum
     * {@code @Enumerated(EnumType.STRING)}, and Hibernate resolves a stored name against the constants that
     * exist - so a name with no constant is not an unknown row, it is an exception thrown while reading the
     * audit log at all. Removing this constant makes those deployments unable to open the page.
     */
    COMMUNITY_GRANT_OFFLINE_CHECKER_RUN,
    /**
     * Community Grant access request. Never written by this product, and declared for the same reason as
     * {@link #COMMUNITY_GRANT_OFFLINE_CHECKER_RUN}.
     */
    COMMUNITY_GRANT_REQUEST_ACCESS,
    /**
     * Community Grant enrollment started or declined. Never written by this product, and declared for the
     * same reason as {@link #COMMUNITY_GRANT_OFFLINE_CHECKER_RUN}.
     */
    COMMUNITY_GRANT_ENROLLMENT;

    @Getter
    private final boolean read;

    private final TbMsgType ruleEngineMsgType;

    @Getter
    private final boolean alarmAction;

    ActionType() {
        this(false, null, false);
    }

    ActionType(boolean read) {
        this(read, null, false);
    }

    ActionType(TbMsgType ruleEngineMsgType) {
        this(false, ruleEngineMsgType, false);
    }

    ActionType(TbMsgType ruleEngineMsgType, boolean isAlarmAction) {
        this(false, ruleEngineMsgType, isAlarmAction);
    }

    ActionType(boolean read, TbMsgType ruleEngineMsgType, boolean alarmAction) {
        this.read = read;
        this.ruleEngineMsgType = ruleEngineMsgType;
        this.alarmAction = alarmAction;
    }

    public Optional<TbMsgType> getRuleEngineMsgType() {
        return Optional.ofNullable(ruleEngineMsgType);
    }

}
