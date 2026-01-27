/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.user;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class UserEdgeProcessor extends BaseUserProcessor implements UserProcessor {

    @Override
    public ListenableFuture<Void> processUserMsgFromEdge(TenantId tenantId, Edge edge, UserUpdateMsg userUpdateMsg) {
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
                    deleteUserAndPushEntityDeletedEventToRuleEngine(tenantId, userId, edge);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(userUpdateMsg.getMsgType());
            };
        } catch (DataValidationException e) {
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

    private void saveOrUpdateUser(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateUser(tenantId, userId, userUpdateMsg);
        boolean isCreated = resultPair.getFirst();
        if (isCreated) {
            createRelationFromEdge(tenantId, edge.getId(), userId);
            pushUserCreatedEventToRuleEngine(tenantId, edge, userId);
        }

        boolean userEmailUpdated = resultPair.getSecond();

        if (userEmailUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER, EdgeEventActionType.UPDATED, userId, null);
        }
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
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                User user = edgeCtx.getUserService().findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(msgType, user));
                    UserCredentials userCredentialsByUserId = edgeCtx.getUserService().findUserCredentialsByUserId(edgeEvent.getTenantId(), userId);
                    if (userCredentialsByUserId != null) {
                        builder.addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentialsByUserId));
                    }
                    return builder.build();
                }
            }
            case DELETED -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserDeleteMsg(userId))
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
