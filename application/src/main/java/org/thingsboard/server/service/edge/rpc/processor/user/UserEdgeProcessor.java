// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.user;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.edge.rpc.EdgeVersionComparator;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class UserEdgeProcessor extends BaseUserProcessor implements UserProcessor {

    @Override
    public ListenableFuture<Void> processUserMsgFromEdge(TenantId tenantId, Edge edge, UserUpdateMsg userUpdateMsg, EdgeVersion edgeVersion) {
        log.trace("[{}] executing processUserMsgFromEdge [{}] from edge [{}]", tenantId, userUpdateMsg, edge.getId());
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (userUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateUser(tenantId, userId, userUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (userUpdateMsg.hasEntityGroupIdMSB() && userUpdateMsg.hasEntityGroupIdLSB()) {
                        EntityGroupId entityGroupId = new EntityGroupId(
                                new UUID(userUpdateMsg.getEntityGroupIdMSB(), userUpdateMsg.getEntityGroupIdLSB()));
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, userId);
                        yield Futures.immediateFuture(null);
                    } else if (EdgeVersionComparator.INSTANCE.compare(edgeVersion, EdgeVersion.V_4_3_0) >= 0) {
                        deleteUserAndPushEntityDeletedEventToRuleEngine(tenantId, userId, edge);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(userUpdateMsg.getMsgType());
            };
        } catch (DataValidationException | ThingsboardException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed users violated {}", tenantId, userUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public ListenableFuture<Void> processUserCredentialsMsgFromEdge(TenantId tenantId, Edge edge, UserCredentialsUpdateMsg userCredentialsUpdateMsg) {
        log.debug("[{}] Executing processUserCredentialsMsgFromEdge, userCredentialsUpdateMsg [{}]", tenantId, userCredentialsUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            super.updateUserCredentials(tenantId, userCredentialsUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void saveOrUpdateUser(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg, Edge edge) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateUser(tenantId, userId, userUpdateMsg);
        boolean isCreated = resultPair.getFirst();
        if (isCreated) {
            createRelationFromEdge(tenantId, edge.getId(), userId);
            pushUserCreatedEventToRuleEngine(tenantId, edge, userId);
        }
        addUserToEdgeAllUserGroup(tenantId, edge, userId);
        boolean userEmailUpdated = resultPair.getSecond();
        if (userEmailUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER, EdgeEventActionType.UPDATED, userId, null);
        }
    }

    private void addUserToEdgeAllUserGroup(TenantId tenantId, Edge edge, UserId userId) {
        User user = edgeCtx.getUserService().findUserById(tenantId, userId);
        addEntityToEdgeAllGroup(tenantId, edge, user);
    }

    private void pushUserCreatedEventToRuleEngine(TenantId tenantId, Edge edge, UserId userId) {
        try {
            User user = edgeCtx.getUserService().findUserById(tenantId, userId);
            if (user != null) {
                String userAsString = JacksonUtil.toString(user);
                TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, user.getCustomerId());
                pushEntityEventToRuleEngine(tenantId, userId, user.getCustomerId(), TbMsgType.ENTITY_CREATED, userAsString, msgMetaData);
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push user action to rule engine: {}", tenantId, userId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        UserId userId = new UserId(edgeEvent.getEntityId());
        EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
        switch (edgeEvent.getAction()) {
            case ADDED, ADDED_TO_ENTITY_GROUP, UPDATED, ASSIGNED_TO_EDGE -> {
                User user = edgeCtx.getUserService().findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(msgType, user, entityGroupId));
                    UserCredentials userCredentialsByUserId = edgeCtx.getUserService().findUserCredentialsByUserId(edgeEvent.getTenantId(), userId);
                    if (userCredentialsByUserId != null) {
                        builder.addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentialsByUserId));
                    }
                    List<ApiKey> apiKeys = edgeCtx.getApiKeyService().findApiKeysByUserId(edgeEvent.getTenantId(), userId);
                    for (ApiKey apiKey : apiKeys) {
                        builder.addApiKeyUpdateMsg(EdgeMsgConstructorUtils.constructApiKeyUpdatedMsg(msgType, apiKey));
                    }
                    return builder.build();
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP, UNASSIGNED_FROM_EDGE, CHANGE_OWNER -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserDeleteMsg(userId, entityGroupId))
                        .build();
            }
            case CREDENTIALS_UPDATED -> {
                UserCredentials userCredentialsByUserId = edgeCtx.getUserService().findUserCredentialsByUserId(edgeEvent.getTenantId(), userId);
                if (userCredentialsByUserId != null) {
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentialsByUserId))
                            .build();
                }
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.USER;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, User user, UserUpdateMsg userUpdateMsg) {
        CustomerId customerUUID = user.getCustomerId() != null ? user.getCustomerId() : customerId;
        user.setCustomerId(customerUUID);
    }

}