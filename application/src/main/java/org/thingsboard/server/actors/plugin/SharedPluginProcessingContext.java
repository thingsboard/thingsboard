/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.actors.plugin;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.controller.plugin.PluginWebSocketMsgEndpoint;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.extensions.api.device.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequestPluginMsg;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import scala.concurrent.duration.Duration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
public final class SharedPluginProcessingContext {
    final ActorRef parentActor;
    final ActorRef currentActor;
    final ActorSystemContext systemContext;
    final PluginWebSocketMsgEndpoint msgEndpoint;
    final AssetService assetService;
    final DeviceService deviceService;
    final RuleService ruleService;
    final PluginService pluginService;
    final CustomerService customerService;
    final TenantService tenantService;
    final TimeseriesService tsService;
    final AttributesService attributesService;
    final ClusterRpcService rpcService;
    final ClusterRoutingService routingService;
    final RelationService relationService;
    final PluginId pluginId;
    final TenantId tenantId;

    public SharedPluginProcessingContext(ActorSystemContext sysContext, TenantId tenantId, PluginId pluginId,
                                         ActorRef parentActor, ActorRef self) {
        super();
        this.tenantId = tenantId;
        this.pluginId = pluginId;
        this.parentActor = parentActor;
        this.currentActor = self;
        this.systemContext = sysContext;
        this.msgEndpoint = sysContext.getWsMsgEndpoint();
        this.tsService = sysContext.getTsService();
        this.attributesService = sysContext.getAttributesService();
        this.assetService = sysContext.getAssetService();
        this.deviceService = sysContext.getDeviceService();
        this.rpcService = sysContext.getRpcService();
        this.routingService = sysContext.getRoutingService();
        this.ruleService = sysContext.getRuleService();
        this.pluginService = sysContext.getPluginService();
        this.customerService = sysContext.getCustomerService();
        this.tenantService = sysContext.getTenantService();
        this.relationService = sysContext.getRelationService();
    }

    public PluginId getPluginId() {
        return pluginId;
    }

    public TenantId getPluginTenantId() {
        return tenantId;
    }

    public void toDeviceActor(DeviceAttributesEventNotificationMsg msg) {
        forward(msg.getDeviceId(), msg, rpcService::tell);
    }

    public void sendRpcRequest(ToDeviceRpcRequest msg) {
        log.trace("[{}] Forwarding msg {} to device actor!", pluginId, msg);
        ToDeviceRpcRequestPluginMsg rpcMsg = new ToDeviceRpcRequestPluginMsg(pluginId, tenantId, msg);
        forward(msg.getDeviceId(), rpcMsg, rpcService::tell);
    }

    private <T> void forward(DeviceId deviceId, T msg, BiConsumer<ServerAddress, T> rpcFunction) {
        Optional<ServerAddress> instance = routingService.resolveById(deviceId);
        if (instance.isPresent()) {
            log.trace("[{}] Forwarding msg {} to remote device actor!", pluginId, msg);
            rpcFunction.accept(instance.get(), msg);
        } else {
            log.trace("[{}] Forwarding msg {} to local device actor!", pluginId, msg);
            parentActor.tell(msg, ActorRef.noSender());
        }
    }

    public void scheduleTimeoutMsg(TimeoutMsg msg) {
        log.debug("Scheduling msg {} with delay {} ms", msg, msg.getTimeout());
        systemContext.getScheduler().scheduleOnce(
                Duration.create(msg.getTimeout(), TimeUnit.MILLISECONDS),
                currentActor,
                msg,
                systemContext.getActorSystem().dispatcher(),
                ActorRef.noSender());

    }

    public void persistError(String method, Exception e) {
        systemContext.persistError(tenantId, pluginId, method, e);
    }

    public ActorRef self() {
        return currentActor;
    }
}
