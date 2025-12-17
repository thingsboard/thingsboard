/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeCommunicationFailureTrigger;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldRequestMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EdgeUplinkMessageDispatcher {

    private final EdgeContextComponent ctx;

    public ListenableFuture<List<Void>> processUplinkMsg(EdgeSessionState state, UplinkMsg uplinkMsg) {
        Edge edge = state.getEdge();
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
                for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : uplinkMsg.getDeviceProfileUpdateMsgList()) {
                    result.add(ctx.getDeviceProfileProcessor().processDeviceProfileMsgFromEdge(edge.getTenantId(), edge, deviceProfileUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceMsgFromEdge(edge.getTenantId(), edge, deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceCredentialsMsgFromEdge(edge.getTenantId(), edge.getId(), deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAssetProfileUpdateMsgCount() > 0) {
                for (AssetProfileUpdateMsg assetProfileUpdateMsg : uplinkMsg.getAssetProfileUpdateMsgList()) {
                    result.add(ctx.getAssetProfileProcessor().processAssetProfileMsgFromEdge(edge.getTenantId(), edge, assetProfileUpdateMsg));
                }
            }
            if (uplinkMsg.getAssetUpdateMsgCount() > 0) {
                for (AssetUpdateMsg assetUpdateMsg : uplinkMsg.getAssetUpdateMsgList()) {
                    result.add(ctx.getAssetProcessor().processAssetMsgFromEdge(edge.getTenantId(), edge, assetUpdateMsg));
                }
            }
            if (uplinkMsg.getRuleChainUpdateMsgCount() > 0) {
                for (RuleChainUpdateMsg ruleChainUpdateMsg : uplinkMsg.getRuleChainUpdateMsgList()) {
                    result.add(ctx.getRuleChainProcessor().processRuleChainMsgFromEdge(edge.getTenantId(), edge, ruleChainUpdateMsg));
                }
            }
            if (uplinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
                for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : uplinkMsg.getRuleChainMetadataUpdateMsgList()) {
                    result.add(ctx.getRuleChainProcessor().processRuleChainMetadataMsgFromEdge(edge.getTenantId(), edge, ruleChainMetadataUpdateMsg));
                }
            }
            if (uplinkMsg.getEntityViewUpdateMsgCount() > 0) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : uplinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(ctx.getEntityViewProcessor().processEntityViewMsgFromEdge(edge.getTenantId(), edge, entityViewUpdateMsg));
                }
            }
            if (uplinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    result.addAll(ctx.getTelemetryProcessor().processTelemetryMsg(edge.getTenantId(), entityData));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(ctx.getAlarmProcessor().processAlarmMsgFromEdge(edge.getTenantId(), edge.getId(), alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmCommentUpdateMsgCount() > 0) {
                for (AlarmCommentUpdateMsg alarmCommentUpdateMsg : uplinkMsg.getAlarmCommentUpdateMsgList()) {
                    result.add(ctx.getAlarmCommentProcessor().processAlarmCommentMsgFromEdge(edge.getTenantId(), edge.getId(), alarmCommentUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    result.add(ctx.getRelationProcessor().processRelationMsgFromEdge(edge.getTenantId(), edge, relationUpdateMsg));
                }
            }
            if (uplinkMsg.getDashboardUpdateMsgCount() > 0) {
                for (DashboardUpdateMsg dashboardUpdateMsg : uplinkMsg.getDashboardUpdateMsgList()) {
                    result.add(ctx.getDashboardProcessor().processDashboardMsgFromEdge(edge.getTenantId(), edge, dashboardUpdateMsg));
                }
            }
            if (uplinkMsg.getResourceUpdateMsgCount() > 0) {
                for (ResourceUpdateMsg resourceUpdateMsg : uplinkMsg.getResourceUpdateMsgList()) {
                    result.add(ctx.getResourceProcessor().processResourceMsgFromEdge(edge.getTenantId(), edge, resourceUpdateMsg));
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgCount() > 0) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRuleChainMetadataRequestMsg(edge.getTenantId(), edge, ruleChainMetadataRequestMsg));
                }
            }
            if (uplinkMsg.getAttributesRequestMsgCount() > 0) {
                for (AttributesRequestMsg attributesRequestMsg : uplinkMsg.getAttributesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processAttributesRequestMsg(edge.getTenantId(), edge, attributesRequestMsg));
                }
            }
            if (uplinkMsg.getRelationRequestMsgCount() > 0) {
                for (RelationRequestMsg relationRequestMsg : uplinkMsg.getRelationRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRelationRequestMsg(edge.getTenantId(), edge, relationRequestMsg));
                }
            }
            if (uplinkMsg.getCalculatedFieldRequestMsgCount() > 0) {
                for (CalculatedFieldRequestMsg calculatedFieldRequestMsg : uplinkMsg.getCalculatedFieldRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processCalculatedFieldRequestMsg(edge.getTenantId(), edge, calculatedFieldRequestMsg));
                }
            }
            if (uplinkMsg.getUserCredentialsRequestMsgCount() > 0) {
                for (UserCredentialsRequestMsg userCredentialsRequestMsg : uplinkMsg.getUserCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processUserCredentialsRequestMsg(edge.getTenantId(), edge, userCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : uplinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processDeviceCredentialsRequestMsg(edge.getTenantId(), edge, deviceCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcCallMsg : uplinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(ctx.getDeviceProcessor().processDeviceRpcCallFromEdge(edge.getTenantId(), edge, deviceRpcCallMsg));
                }
            }
            if (uplinkMsg.getWidgetBundleTypesRequestMsgCount() > 0) {
                for (WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg : uplinkMsg.getWidgetBundleTypesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processWidgetBundleTypesRequestMsg(edge.getTenantId(), edge, widgetBundleTypesRequestMsg));
                }
            }
            if (uplinkMsg.getEntityViewsRequestMsgCount() > 0) {
                for (EntityViewsRequestMsg entityViewRequestMsg : uplinkMsg.getEntityViewsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processEntityViewsRequestMsg(edge.getTenantId(), edge, entityViewRequestMsg));
                }
            }
            if (uplinkMsg.getCalculatedFieldUpdateMsgCount() > 0) {
                for (CalculatedFieldUpdateMsg calculatedFieldUpdateMsg : uplinkMsg.getCalculatedFieldUpdateMsgList()) {
                    result.add(ctx.getCalculatedFieldProcessor().processCalculatedFieldMsgFromEdge(edge.getTenantId(), edge, calculatedFieldUpdateMsg));
                }
            }
            if (uplinkMsg.getAiModelUpdateMsgCount() > 0) {
                for (AiModelUpdateMsg aiModelUpdateMsg : uplinkMsg.getAiModelUpdateMsgList()) {
                    result.add(ctx.getAiModelProcessor().processAiModelMsgFromEdge(edge.getTenantId(), edge, aiModelUpdateMsg));
                }
            }
            if (uplinkMsg.getUserUpdateMsgCount() > 0) {
                for (UserUpdateMsg userUpdateMsg : uplinkMsg.getUserUpdateMsgList()) {
                    state.lockSequenceDependency();
                    try {
                        result.add(ctx.getUserProcessor().processUserMsgFromEdge(edge.getTenantId(), edge, userUpdateMsg));
                    } finally {
                        state.unlockSequenceDependency();
                    }
                }
            }
            if (uplinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
                for (UserCredentialsUpdateMsg userCredentialsUpdateMsg : uplinkMsg.getUserCredentialsUpdateMsgList()) {
                    state.lockSequenceDependency();
                    try {
                        result.add(ctx.getUserProcessor().processUserCredentialsMsgFromEdge(edge.getTenantId(), edge, userCredentialsUpdateMsg));
                    } finally {
                        state.unlockSequenceDependency();
                    }
                }
            }
        } catch (Exception e) {
            String failureMsg = String.format("Can't process uplink msg [%s] from edge", uplinkMsg);
            log.trace("[{}][{}] Can't process uplink msg [{}]", edge.getTenantId(), edge.getId(), uplinkMsg, e);
            ctx.getRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(edge.getTenantId()).edgeId(edge.getId())
                    .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(e.getMessage()).build());
            return Futures.immediateFailedFuture(e);
        }
        return Futures.allAsList(result);
    }
}
