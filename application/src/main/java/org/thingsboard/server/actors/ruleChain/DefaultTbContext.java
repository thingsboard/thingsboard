/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorRef;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Function;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.script.NashornJsEngine;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 19.03.18.
 */
class DefaultTbContext implements TbContext {

    private final ActorSystemContext mainCtx;
    private final RuleNodeCtx nodeCtx;

    public DefaultTbContext(ActorSystemContext mainCtx, RuleNodeCtx nodeCtx) {
        this.mainCtx = mainCtx;
        this.nodeCtx = nodeCtx;
    }

    @Override
    public void tellNext(TbMsg msg) {
        tellNext(msg, (String) null);
    }

    @Override
    public void tellNext(TbMsg msg, String relationType) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg);
        }
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getId(), relationType, msg), nodeCtx.getSelfActor());
    }

    @Override
    public void tellSelf(TbMsg msg, long delayMs) {
        //TODO: add persistence layer
        scheduleMsgWithDelay(new RuleNodeToSelfMsg(msg), delayMs, nodeCtx.getSelfActor());
    }

    private void scheduleMsgWithDelay(Object msg, long delayInMs, ActorRef target) {
        mainCtx.getScheduler().scheduleOnce(Duration.create(delayInMs, TimeUnit.MILLISECONDS), target, msg, mainCtx.getActorSystem().dispatcher(), nodeCtx.getSelfActor());
    }

    @Override
    public void tellOthers(TbMsg msg) {
        throw new RuntimeException("Not Implemented!");
    }

    @Override
    public void tellSibling(TbMsg msg, ServerAddress address) {
        throw new RuntimeException("Not Implemented!");
    }

    @Override
    public void spawn(TbMsg msg) {
        throw new RuntimeException("Not Implemented!");
    }

    @Override
    public void ack(TbMsg msg) {

    }

    @Override
    public void tellError(TbMsg msg, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, th);
        }
        nodeCtx.getSelfActor().tell(new RuleNodeToSelfErrorMsg(msg, th), nodeCtx.getSelfActor());
    }

    @Override
    public void updateSelf(RuleNode self) {
        nodeCtx.setSelf(self);
    }

    @Override
    public TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(UUIDs.timeBased(), type, originator, metaData, data, nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId(), 0L);
    }

    @Override
    public RuleNodeId getSelfId() {
        return nodeCtx.getSelf().getId();
    }

    @Override
    public TenantId getTenantId() {
        return nodeCtx.getTenantId();
    }

    @Override
    public void tellNext(TbMsg msg, Set<String> relationTypes) {
        relationTypes.forEach(type -> tellNext(msg, type));
    }

    @Override
    public ListeningExecutor getJsExecutor() {
        return mainCtx.getJsExecutor();
    }

    @Override
    public ListeningExecutor getMailExecutor() {
        return mainCtx.getMailExecutor();
    }

    @Override
    public ListeningExecutor getDbCallbackExecutor() {
        return mainCtx.getDbCallbackExecutor();
    }

    @Override
    public ScriptEngine createJsScriptEngine(String script, String functionName, String... argNames) {
        return new NashornJsEngine(script, functionName, argNames);
    }

    @Override
    public AttributesService getAttributesService() {
        return mainCtx.getAttributesService();
    }

    @Override
    public CustomerService getCustomerService() {
        return mainCtx.getCustomerService();
    }

    @Override
    public UserService getUserService() {
        return mainCtx.getUserService();
    }

    @Override
    public PluginService getPluginService() {
        return mainCtx.getPluginService();
    }

    @Override
    public AssetService getAssetService() {
        return mainCtx.getAssetService();
    }

    @Override
    public DeviceService getDeviceService() {
        return mainCtx.getDeviceService();
    }

    @Override
    public AlarmService getAlarmService() {
        return mainCtx.getAlarmService();
    }

    @Override
    public RuleChainService getRuleChainService() {
        return mainCtx.getRuleChainService();
    }

    @Override
    public TimeseriesService getTimeseriesService() {
        return mainCtx.getTsService();
    }

    @Override
    public RuleEngineTelemetryService getTelemetryService() {
        return mainCtx.getTsSubService();
    }

    @Override
    public RelationService getRelationService() {
        return mainCtx.getRelationService();
    }

    @Override
    public MailService getMailService() {
        return mainCtx.getMailService();
    }

    @Override
    public RuleEngineRpcService getRpcService() {
        return new RuleEngineRpcService() {
            @Override
            public void sendRpcReply(DeviceId deviceId, int requestId, String body) {
                mainCtx.getDeviceRpcService().sendRpcReplyToDevice(nodeCtx.getTenantId(), deviceId, requestId, body);
            }

            @Override
            public void sendRpcRequest(RuleEngineDeviceRpcRequest src, Consumer<RuleEngineDeviceRpcResponse> consumer) {
                ToDeviceRpcRequest request = new ToDeviceRpcRequest(UUIDs.timeBased(), nodeCtx.getTenantId(), src.getDeviceId(),
                        src.isOneway(), System.currentTimeMillis() + src.getTimeout(), new ToDeviceRpcRequestBody(src.getMethod(), src.getBody()));
                mainCtx.getDeviceRpcService().process(request, response -> {
                    consumer.accept(RuleEngineDeviceRpcResponse.builder()
                            .deviceId(src.getDeviceId())
                            .requestId(src.getRequestId())
                            .error(response.getError())
                            .response(response.getResponse())
                            .build());
                });
            }
        };
    }
}
