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

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import org.thingsboard.rule.engine.api.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
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
import scala.concurrent.duration.Duration;

import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    public RuleNodeId getSelfId() {
        return nodeCtx.getSelf().getId();
    }

    @Override
    public void tellNext(TbMsg msg, Set<String> relationTypes) {
        relationTypes.forEach(type -> tellNext(msg, type));
    }

    @Override
    public ListeningExecutor getJsExecutor() {
        return mainCtx.getExecutor();
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
    public RelationService getRelationService() {
        return mainCtx.getRelationService();
    }
}
