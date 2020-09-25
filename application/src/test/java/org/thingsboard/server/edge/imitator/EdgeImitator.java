/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.edge.rpc.EdgeGrpcClient;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class EdgeImitator {

    private String routingKey;
    private String routingSecret;

    private EdgeRpcClient edgeRpcClient;

    @Getter
    private EdgeStorage storage;


    public EdgeImitator(String host, int port, String routingKey, String routingSecret) throws NoSuchFieldException, IllegalAccessException {
        edgeRpcClient = new EdgeGrpcClient();
        storage = new EdgeStorage();
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        setEdgeCredentials("rpcHost", host);
        setEdgeCredentials("rpcPort", port);
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
        edgeRpcClient.disconnect();
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        log.info("onUplinkResponse: {}", msg);
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        storage.setConfiguration(edgeConfiguration);
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
        if (downlinkMsg.getDeviceUpdateMsgList() != null && !downlinkMsg.getDeviceUpdateMsgList().isEmpty()) {
            for (DeviceUpdateMsg deviceUpdateMsg: downlinkMsg.getDeviceUpdateMsgList()) {
                result.add(storage.processEntity(deviceUpdateMsg.getMsgType(), EntityType.DEVICE, new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getAssetUpdateMsgList() != null && !downlinkMsg.getAssetUpdateMsgList().isEmpty()) {
            for (AssetUpdateMsg assetUpdateMsg: downlinkMsg.getAssetUpdateMsgList()) {
                result.add(storage.processEntity(assetUpdateMsg.getMsgType(), EntityType.ASSET, new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getRuleChainUpdateMsgList() != null && !downlinkMsg.getRuleChainUpdateMsgList().isEmpty()) {
            for (RuleChainUpdateMsg ruleChainUpdateMsg: downlinkMsg.getRuleChainUpdateMsgList()) {
                result.add(storage.processEntity(ruleChainUpdateMsg.getMsgType(), EntityType.RULE_CHAIN, new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getDashboardUpdateMsgList() != null && !downlinkMsg.getDashboardUpdateMsgList().isEmpty()) {
            for (DashboardUpdateMsg dashboardUpdateMsg: downlinkMsg.getDashboardUpdateMsgList()) {
                result.add(storage.processEntity(dashboardUpdateMsg.getMsgType(), EntityType.DASHBOARD, new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getRelationUpdateMsgList() != null && !downlinkMsg.getRelationUpdateMsgList().isEmpty()) {
            for (RelationUpdateMsg relationUpdateMsg: downlinkMsg.getRelationUpdateMsgList()) {
                result.add(storage.processRelation(relationUpdateMsg));
            }
        }
        if (downlinkMsg.getAlarmUpdateMsgList() != null && !downlinkMsg.getAlarmUpdateMsgList().isEmpty()) {
            for (AlarmUpdateMsg alarmUpdateMsg: downlinkMsg.getAlarmUpdateMsgList()) {
                result.add(storage.processAlarm(alarmUpdateMsg));
            }
        }
        return Futures.allAsList(result);
    }

}
