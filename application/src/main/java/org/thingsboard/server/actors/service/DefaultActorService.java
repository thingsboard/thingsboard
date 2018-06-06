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
package org.thingsboard.server.actors.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.app.AppActor;
import org.thingsboard.server.actors.rpc.RpcBroadcastMsg;
import org.thingsboard.server.actors.rpc.RpcManagerActor;
import org.thingsboard.server.actors.rpc.RpcSessionCreateRequestMsg;
import org.thingsboard.server.actors.session.SessionManagerActor;
import org.thingsboard.server.actors.stats.StatsActor;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.SessionAwareMsg;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.state.DeviceStateService;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.thingsboard.server.gen.cluster.ClusterAPIProtos.MessageType.CLUSTER_ACTOR_MESSAGE;

@Service
@Slf4j
public class DefaultActorService implements ActorService {

    private static final String ACTOR_SYSTEM_NAME = "Akka";

    public static final String APP_DISPATCHER_NAME = "app-dispatcher";
    public static final String CORE_DISPATCHER_NAME = "core-dispatcher";
    public static final String SYSTEM_RULE_DISPATCHER_NAME = "system-rule-dispatcher";
    public static final String SYSTEM_PLUGIN_DISPATCHER_NAME = "system-plugin-dispatcher";
    public static final String TENANT_RULE_DISPATCHER_NAME = "rule-dispatcher";
    public static final String TENANT_PLUGIN_DISPATCHER_NAME = "plugin-dispatcher";
    public static final String SESSION_DISPATCHER_NAME = "session-dispatcher";
    public static final String RPC_DISPATCHER_NAME = "rpc-dispatcher";

    @Autowired
    private ActorSystemContext actorContext;

    @Autowired
    private ClusterRpcService rpcService;

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private DeviceStateService deviceStateService;

    private ActorSystem system;

    private ActorRef appActor;

    private ActorRef sessionManagerActor;

    private ActorRef rpcManagerActor;

    @PostConstruct
    public void initActorSystem() {
        log.info("Initializing Actor system. {}", actorContext.getRuleChainService());
        actorContext.setActorService(this);
        system = ActorSystem.create(ACTOR_SYSTEM_NAME, actorContext.getConfig());
        actorContext.setActorSystem(system);

        appActor = system.actorOf(Props.create(new AppActor.ActorCreator(actorContext)).withDispatcher(APP_DISPATCHER_NAME), "appActor");
        actorContext.setAppActor(appActor);

        sessionManagerActor = system.actorOf(Props.create(new SessionManagerActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME),
                "sessionManagerActor");
        actorContext.setSessionManagerActor(sessionManagerActor);

        rpcManagerActor = system.actorOf(Props.create(new RpcManagerActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME),
                "rpcManagerActor");

        ActorRef statsActor = system.actorOf(Props.create(new StatsActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME), "statsActor");
        actorContext.setStatsActor(statsActor);

        rpcService.init(this);

        discoveryService.addListener(this);
        log.info("Actor system initialized.");
    }

    @PreDestroy
    public void stopActorSystem() {
        Future<Terminated> status = system.terminate();
        try {
            Terminated terminated = Await.result(status, Duration.Inf());
            log.info("Actor system terminated: {}", terminated);
        } catch (Exception e) {
            log.error("Failed to terminate actor system.", e);
        }
    }

    @Override
    public void onMsg(SendToClusterMsg msg) {
        appActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public void process(SessionAwareMsg msg) {
        log.debug("Processing session aware msg: {}", msg);
        sessionManagerActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public void onServerAdded(ServerInstance server) {
        log.trace("Processing onServerAdded msg: {}", server);
        broadcast(new ClusterEventMsg(server.getServerAddress(), true));
    }

    @Override
    public void onServerUpdated(ServerInstance server) {
        //Do nothing
    }

    @Override
    public void onServerRemoved(ServerInstance server) {
        log.trace("Processing onServerRemoved msg: {}", server);
        broadcast(new ClusterEventMsg(server.getServerAddress(), false));
    }

    @Override
    public void onEntityStateChange(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state) {
        log.trace("[{}] Processing {} state change event: {}", tenantId, entityId.getEntityType(), state);
        broadcast(new ComponentLifecycleMsg(tenantId, entityId, state));
    }

    @Override
    public void onCredentialsUpdate(TenantId tenantId, DeviceId deviceId) {
        DeviceCredentialsUpdateNotificationMsg msg = new DeviceCredentialsUpdateNotificationMsg(tenantId, deviceId);
        appActor.tell(new SendToClusterMsg(deviceId, msg), ActorRef.noSender());
    }

    @Override
    public void onDeviceNameOrTypeUpdate(TenantId tenantId, DeviceId deviceId, String deviceName, String deviceType) {
        log.trace("[{}] Processing onDeviceNameOrTypeUpdate event, deviceName: {}, deviceType: {}", deviceId, deviceName, deviceType);
        DeviceNameOrTypeUpdateMsg msg = new DeviceNameOrTypeUpdateMsg(tenantId, deviceId, deviceName, deviceType);
        appActor.tell(new SendToClusterMsg(deviceId, msg), ActorRef.noSender());
    }

    @Override
    public void onMsg(ServiceToRuleEngineMsg msg) {
        appActor.tell(msg, ActorRef.noSender());
    }

    public void broadcast(ToAllNodesMsg msg) {
        actorContext.getEncodingService().encode(msg);
        rpcService.broadcast(new RpcBroadcastMsg(ClusterAPIProtos.ClusterMessage
                .newBuilder()
                .setPayload(ByteString
                        .copyFrom(actorContext.getEncodingService().encode(msg)))
                .setMessageType(CLUSTER_ACTOR_MESSAGE)
                .build()));
        appActor.tell(msg, ActorRef.noSender());
    }

    private void broadcast(ClusterEventMsg msg) {
        this.appActor.tell(msg, ActorRef.noSender());
        this.sessionManagerActor.tell(msg, ActorRef.noSender());
        this.rpcManagerActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public void onReceivedMsg(ServerAddress source, ClusterAPIProtos.ClusterMessage msg) {
        ServerAddress serverAddress = new ServerAddress(source.getHost(), source.getPort());
        log.info("Received msg [{}] from [{}]", msg.getMessageType().name(), serverAddress);
        if (log.isDebugEnabled()) {
            log.info("MSG: ", msg);
        }
        switch (msg.getMessageType()) {
            case CLUSTER_ACTOR_MESSAGE:
                java.util.Optional<TbActorMsg> decodedMsg = actorContext.getEncodingService()
                        .decode(msg.getPayload().toByteArray());
                if (decodedMsg.isPresent()) {
                    appActor.tell(decodedMsg.get(), ActorRef.noSender());
                } else {
                    log.error("Error during decoding cluster proto message");
                }
                break;
            case TO_ALL_NODES_MSG:
                //TODO
                break;
            case CLUSTER_TELEMETRY_SUBSCRIPTION_CREATE_MESSAGE:
                actorContext.getTsSubService().onNewRemoteSubscription(serverAddress, msg.getPayload().toByteArray());
                break;
            case CLUSTER_TELEMETRY_SUBSCRIPTION_UPDATE_MESSAGE:
                actorContext.getTsSubService().onRemoteSubscriptionUpdate(serverAddress, msg.getPayload().toByteArray());
                break;
            case CLUSTER_TELEMETRY_SUBSCRIPTION_CLOSE_MESSAGE:
                actorContext.getTsSubService().onRemoteSubscriptionClose(serverAddress, msg.getPayload().toByteArray());
                break;
            case CLUSTER_TELEMETRY_SESSION_CLOSE_MESSAGE:
                actorContext.getTsSubService().onRemoteSessionClose(serverAddress, msg.getPayload().toByteArray());
                break;
            case CLUSTER_TELEMETRY_ATTR_UPDATE_MESSAGE:
                actorContext.getTsSubService().onRemoteAttributesUpdate(serverAddress, msg.getPayload().toByteArray());
                break;
            case CLUSTER_TELEMETRY_TS_UPDATE_MESSAGE:
                actorContext.getTsSubService().onRemoteTsUpdate(serverAddress, msg.getPayload().toByteArray());
                break;
            case CLUSTER_RPC_FROM_DEVICE_RESPONSE_MESSAGE:
                actorContext.getDeviceRpcService().processRemoteResponseFromDevice(serverAddress, msg.getPayload().toByteArray());
                break;
            case CLUSTER_DEVICE_STATE_SERVICE_MESSAGE:
                actorContext.getDeviceStateService().onRemoteMsg(serverAddress, msg.getPayload().toByteArray());
                break;
        }
    }

    @Override
    public void onSendMsg(ClusterAPIProtos.ClusterMessage msg) {
        rpcManagerActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public void onRpcSessionCreateRequestMsg(RpcSessionCreateRequestMsg msg) {
        rpcManagerActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public void onBroadcastMsg(RpcBroadcastMsg msg) {
        rpcManagerActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public void onDeviceAdded(Device device) {
        deviceStateService.onDeviceAdded(device);
    }
}
