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
package org.thingsboard.server.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.controller.plugin.PluginWebSocketMsgEndpoint;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

@Component
public class ActorSystemContext {
    private static final String AKKA_CONF_FILE_NAME = "actor-system.conf";

    protected final ObjectMapper mapper = new ObjectMapper();

    @Getter @Setter private ActorService actorService;

    @Autowired
    @Getter private DiscoveryService discoveryService;

    @Autowired
    @Getter @Setter private ComponentDiscoveryService componentService;

    @Autowired
    @Getter private ClusterRoutingService routingService;

    @Autowired
    @Getter private ClusterRpcService rpcService;

    @Autowired
    @Getter private DeviceAuthService deviceAuthService;

    @Autowired
    @Getter private DeviceService deviceService;

    @Autowired
    @Getter private AssetService assetService;

    @Autowired
    @Getter private TenantService tenantService;

    @Autowired
    @Getter private CustomerService customerService;

    @Autowired
    @Getter private RuleService ruleService;

    @Autowired
    @Getter private PluginService pluginService;

    @Autowired
    @Getter private TimeseriesService tsService;

    @Autowired
    @Getter private AttributesService attributesService;

    @Autowired
    @Getter private EventService eventService;

    @Autowired
    @Getter private AlarmService alarmService;

    @Autowired
    @Getter private RelationService relationService;

    @Autowired
    @Getter @Setter private PluginWebSocketMsgEndpoint wsMsgEndpoint;

    @Value("${actors.session.sync.timeout}")
    @Getter private long syncSessionTimeout;

    @Value("${actors.plugin.termination.delay}")
    @Getter private long pluginActorTerminationDelay;

    @Value("${actors.plugin.processing.timeout}")
    @Getter private long pluginProcessingTimeout;

    @Value("${actors.plugin.error_persist_frequency}")
    @Getter private long pluginErrorPersistFrequency;

    @Value("${actors.rule.termination.delay}")
    @Getter private long ruleActorTerminationDelay;

    @Value("${actors.rule.error_persist_frequency}")
    @Getter private long ruleErrorPersistFrequency;

    @Value("${actors.statistics.enabled}")
    @Getter private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency}")
    @Getter private long statisticsPersistFrequency;

    @Value("${actors.tenant.create_components_on_init}")
    @Getter private boolean tenantComponentsInitEnabled;

    @Getter @Setter private ActorSystem actorSystem;

    @Getter @Setter private ActorRef appActor;

    @Getter @Setter private ActorRef sessionManagerActor;

    @Getter @Setter private ActorRef statsActor;

    @Getter private final Config config;

    public ActorSystemContext() {
        config = ConfigFactory.parseResources(AKKA_CONF_FILE_NAME).withFallback(ConfigFactory.load());
    }

    public Scheduler getScheduler() {
        return actorSystem.scheduler();
    }

    public void persistError(TenantId tenantId, EntityId entityId, String method, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.ERROR);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), method, toString(e)));
        persistEvent(event);
    }

    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.LC_EVENT);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), lcEvent, Optional.ofNullable(e)));
        persistEvent(event);
    }

    private void persistEvent(Event event) {
        eventService.save(event);
    }

    private String toString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private JsonNode toBodyJson(ServerAddress server, ComponentLifecycleEvent event, Optional<Exception> e) {
        ObjectNode node = mapper.createObjectNode().put("server", server.toString()).put("event", event.name());
        if (e.isPresent()) {
            node = node.put("success", false);
            node = node.put("error", toString(e.get()));
        } else {
            node = node.put("success", true);
        }
        return node;
    }

    private JsonNode toBodyJson(ServerAddress server, String method, String body) {
        return mapper.createObjectNode().put("server", server.toString()).put("method", method).put("error", body);
    }
}
