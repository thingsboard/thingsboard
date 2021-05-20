/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.edge.imitator;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.edge.rpc.EdgeGrpcClient;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetsBundleUpdateMsg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class EdgeImitator {

    private String routingKey;
    private String routingSecret;

    private EdgeRpcClient edgeRpcClient;

    private final Lock lock = new ReentrantLock();

    private CountDownLatch messagesLatch;
    private CountDownLatch responsesLatch;
    private List<Class<? extends AbstractMessage>> ignoredTypes;

    @Getter
    private EdgeConfiguration configuration;
    @Getter
    private UserId userId;
    @Getter
    private final List<AbstractMessage> downlinkMsgs;

    public EdgeImitator(String host, int port, String routingKey, String routingSecret) throws NoSuchFieldException, IllegalAccessException {
        edgeRpcClient = new EdgeGrpcClient();
        messagesLatch = new CountDownLatch(0);
        responsesLatch = new CountDownLatch(0);
        downlinkMsgs = new ArrayList<>();
        ignoredTypes = new ArrayList<>();
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        setEdgeCredentials("rpcHost", host);
        setEdgeCredentials("rpcPort", port);
        setEdgeCredentials("keepAliveTimeSec", 300);
    }

    private void setEdgeCredentials(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field fieldToSet = edgeRpcClient.getClass().getDeclaredField(fieldName);
        fieldToSet.setAccessible(true);
        fieldToSet.set(edgeRpcClient, value);
        fieldToSet.setAccessible(false);
    }

    public void connect() {
        edgeRpcClient.connect(routingKey, routingSecret,
                this::onUplinkResponse,
                this::onEdgeUpdate,
                this::onDownlink,
                this::onClose);

        edgeRpcClient.sendSyncRequestMsg();
    }

    public void disconnect() throws InterruptedException {
        edgeRpcClient.disconnect(false);
    }

    public void sendUplinkMsg(UplinkMsg uplinkMsg) {
        edgeRpcClient.sendUplinkMsg(uplinkMsg);
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        log.info("onUplinkResponse: {}", msg);
        responsesLatch.countDown();
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        this.configuration = edgeConfiguration;
    }

    private void onUserUpdate(UserUpdateMsg userUpdateMsg) {
        this.userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
    }

    private void onDownlink(DownlinkMsg downlinkMsg) {
        ListenableFuture<List<Void>> future = processDownlinkMsg(downlinkMsg);
        Futures.addCallback(future, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder().setSuccess(true).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }

            @Override
            public void onFailure(Throwable t) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(t.getMessage()).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }
        }, MoreExecutors.directExecutor());
    }

    private void onClose(Exception e) {
        log.info("onClose: {}", e.getMessage());
    }

    private ListenableFuture<List<Void>> processDownlinkMsg(DownlinkMsg downlinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        if (downlinkMsg.getDeviceUpdateMsgCount() > 0) {
            for (DeviceUpdateMsg deviceUpdateMsg: downlinkMsg.getDeviceUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
            for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : downlinkMsg.getDeviceProfileUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceProfileUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
            for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg: downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getAssetUpdateMsgCount() > 0) {
            for (AssetUpdateMsg assetUpdateMsg: downlinkMsg.getAssetUpdateMsgList()) {
                result.add(saveDownlinkMsg(assetUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainUpdateMsgCount() > 0) {
            for (RuleChainUpdateMsg ruleChainUpdateMsg: downlinkMsg.getRuleChainUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
            for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg: downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainMetadataUpdateMsg));
            }
        }
        if (downlinkMsg.getDashboardUpdateMsgCount() > 0) {
            for (DashboardUpdateMsg dashboardUpdateMsg: downlinkMsg.getDashboardUpdateMsgList()) {
                result.add(saveDownlinkMsg(dashboardUpdateMsg));
            }
        }
        if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
            for (RelationUpdateMsg relationUpdateMsg: downlinkMsg.getRelationUpdateMsgList()) {
                result.add(saveDownlinkMsg(relationUpdateMsg));
            }
        }
        if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
            for (AlarmUpdateMsg alarmUpdateMsg: downlinkMsg.getAlarmUpdateMsgList()) {
                result.add(saveDownlinkMsg(alarmUpdateMsg));
            }
        }
        if (downlinkMsg.getEntityDataCount() > 0) {
            for (EntityDataProto entityData: downlinkMsg.getEntityDataList()) {
                result.add(saveDownlinkMsg(entityData));
            }
        }
        if (downlinkMsg.getEntityViewUpdateMsgCount() > 0) {
            for (EntityViewUpdateMsg entityViewUpdateMsg: downlinkMsg.getEntityViewUpdateMsgList()) {
                result.add(saveDownlinkMsg(entityViewUpdateMsg));
            }
        }
        if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
            for (CustomerUpdateMsg customerUpdateMsg: downlinkMsg.getCustomerUpdateMsgList()) {
                result.add(saveDownlinkMsg(customerUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
            for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg: downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetsBundleUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
            for (WidgetTypeUpdateMsg widgetTypeUpdateMsg: downlinkMsg.getWidgetTypeUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetTypeUpdateMsg));
            }
        }
        if (downlinkMsg.getUserUpdateMsgCount() > 0) {
            for (UserUpdateMsg userUpdateMsg: downlinkMsg.getUserUpdateMsgList()) {
                onUserUpdate(userUpdateMsg);
                result.add(saveDownlinkMsg(userUpdateMsg));
            }
        }
        if (downlinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
            for (UserCredentialsUpdateMsg userCredentialsUpdateMsg: downlinkMsg.getUserCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(userCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceRpcCallMsgCount() > 0) {
            for (DeviceRpcCallMsg deviceRpcCallMsg: downlinkMsg.getDeviceRpcCallMsgList()) {
                result.add(saveDownlinkMsg(deviceRpcCallMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
            for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg: downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsRequestMsg));
            }
        }
        return Futures.allAsList(result);
    }

    private ListenableFuture<Void> saveDownlinkMsg(AbstractMessage message) {
        if (!ignoredTypes.contains(message.getClass())) {
            try {
                lock.lock();
                downlinkMsgs.add(message);
            } finally {
                lock.unlock();
            }
            messagesLatch.countDown();
        }
        return Futures.immediateFuture(null);
    }

    public boolean waitForMessages() throws InterruptedException {
        return waitForMessages(5);
    }

    public boolean waitForMessages(int timeout) throws InterruptedException {
        return messagesLatch.await(timeout, TimeUnit.SECONDS);
    }

    public void expectMessageAmount(int messageAmount) {
        messagesLatch = new CountDownLatch(messageAmount);
    }

    public boolean waitForResponses() throws InterruptedException {
        return responsesLatch.await(5, TimeUnit.SECONDS);
    }

    public void expectResponsesAmount(int messageAmount) {
        responsesLatch = new CountDownLatch(messageAmount);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> Optional<T> findMessageByType(Class<T> tClass) {
        Optional<T> result;
        try {
            lock.lock();
            result = (Optional<T>) downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg.getClass().isAssignableFrom(tClass)).findAny();
        } finally {
            lock.unlock();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> List<T> findAllMessagesByType(Class<T> tClass) {
        List<T> result;
        try {
            lock.lock();
            result = (List<T>) downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg.getClass().isAssignableFrom(tClass)).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
        return result;
    }

    public AbstractMessage getLatestMessage() {
        return downlinkMsgs.get(downlinkMsgs.size() - 1);
    }

    public void ignoreType(Class<? extends AbstractMessage> type) {
        ignoredTypes.add(type);
    }

    public void allowIgnoredTypes() {
        ignoredTypes.clear();
    }

}
