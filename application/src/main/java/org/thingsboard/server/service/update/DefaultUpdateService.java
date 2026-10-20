// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.update;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.EdgeUpgradeMessageV2;
import org.thingsboard.server.common.data.agent.AgentUpgradeMessage;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.notification.rule.trigger.NewPlatformVersionTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.instructions.EdgeInstallInstructionsService;
import org.thingsboard.server.service.agent.upgrade.AgentUpgradeVersionService;
import org.thingsboard.server.service.edge.instructions.EdgeUpgradeInstructionsService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
public class DefaultUpdateService implements UpdateService {

    private static final String INSTANCE_ID_FILE = ".instance_id";
    private static final String UPDATE_SERVER_BASE_URL = "https://updates.thingsboard.io";

    private static final String PLATFORM_PARAM = "platform";
    private static final String VERSION_PARAM = "version";
    private static final String INSTANCE_ID_PARAM = "instanceId";

    @Value("${updates.enabled}")
    private boolean updatesEnabled;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    @Autowired(required = false)
    private EdgeInstallInstructionsService edgeInstallInstructionsService;

    @Autowired(required = false)
    private EdgeUpgradeInstructionsService edgeUpgradeInstructionsService;

    @Autowired(required = false)
    private AgentUpgradeVersionService agentUpgradeVersionService;

    private final ScheduledExecutorService scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("tb-update-service");

    private ScheduledFuture<?> checkUpdatesFuture = null;
    private final RestTemplate restClient = new RestTemplate();

    private UpdateMessage updateMessage;

    private String platform;
    private String version;
    private UUID instanceId = null;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() {
        version = buildProperties != null ? buildProperties.getVersion() : "unknown";
        updateMessage = new UpdateMessage(false, version, "", "",
                "https://thingsboard.io/docs/pe/reference/releases",
                "https://thingsboard.io/docs/pe/reference/releases");
        if (updatesEnabled) {
            try {
                platform = System.getProperty("platform", "unknown");
                instanceId = parseInstanceId();
                checkUpdatesFuture = scheduler.scheduleAtFixedRate(checkUpdatesRunnable, 0, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                //Do nothing
            }
        }
    }

    private UUID parseInstanceId() throws IOException {
        UUID result = null;
        Path instanceIdPath = Paths.get(INSTANCE_ID_FILE);
        if (instanceIdPath.toFile().exists()) {
            byte[] data = Files.readAllBytes(instanceIdPath);
            if (data.length > 0) {
                try {
                    result = UUID.fromString(new String(data));
                } catch (IllegalArgumentException e) {
                    //Do nothing
                }
            }
        }
        if (result == null) {
            result = UUID.randomUUID();
            Files.write(instanceIdPath, result.toString().getBytes());
        }
        return result;
    }

    @PreDestroy
    private void destroy() {
        try {
            if (checkUpdatesFuture != null) {
                checkUpdatesFuture.cancel(true);
            }
            scheduler.shutdownNow();
        } catch (Exception e) {
            //Do nothing
        }
    }

    Runnable checkUpdatesRunnable = () -> {
        try {
            log.trace("Executing check update method for instanceId [{}], platform [{}] and version [{}]", instanceId, platform, version);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectNode request = JacksonUtil.newObjectNode();
            request.put(PLATFORM_PARAM, platform);
            request.put(VERSION_PARAM, version);
            request.put(INSTANCE_ID_PARAM, instanceId.toString());
            UpdateMessage prevUpdateMessage = updateMessage;
            updateMessage = restClient.postForObject(UPDATE_SERVER_BASE_URL + "/api/v2/thingsboard/updates", new HttpEntity<>(request.toString(), headers), UpdateMessage.class);
            if (updateMessage != null && updateMessage.isUpdateAvailable() && !updateMessage.equals(prevUpdateMessage)) {
                notificationRuleProcessor.process(NewPlatformVersionTrigger.builder()
                        .updateInfo(updateMessage)
                        .build());
            }
        } catch (Exception e) {
            log.trace(e.getMessage());
        }
        updateEdgeVersions();
        updateAgentVersions();
    };

    private void updateEdgeVersions() {
        if (edgeInstallInstructionsService == null || edgeUpgradeInstructionsService == null) {
            return;
        }
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectNode edgeRequest = JacksonUtil.newObjectNode().put(VERSION_PARAM, version);
            String edgePlatformVersion = restClient.postForObject(UPDATE_SERVER_BASE_URL + "/api/v2/edge/installMapping", new HttpEntity<>(edgeRequest.toString(), headers), String.class);
            if (edgePlatformVersion != null) {
                edgeInstallInstructionsService.setPlatformEdgeVersion(edgePlatformVersion);
                edgeUpgradeInstructionsService.setPlatformEdgeVersion(edgePlatformVersion);
            }
            EdgeUpgradeMessageV2 edgeUpgradeMessage = restClient.getForObject(UPDATE_SERVER_BASE_URL + "/api/v2/edge/upgradeMapping", EdgeUpgradeMessageV2.class);
            if (edgeUpgradeMessage != null) {
                edgeUpgradeInstructionsService.updateVersionGraph(edgeUpgradeMessage.getEdgeVersions());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch edge install/upgrade mapping from the update server", e);
        }
    }

    private void updateAgentVersions() {
        if (agentUpgradeVersionService == null) {
            return;
        }
        try {
            AgentUpgradeMessage agentUpgradeMessage = restClient.getForObject(UPDATE_SERVER_BASE_URL + "/api/v1/agent/upgradeMapping", AgentUpgradeMessage.class);
            if (agentUpgradeMessage != null) {
                agentUpgradeVersionService.updateVersionGraph(agentUpgradeMessage.getAgentVersions());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch agent upgrade mapping from the update server", e);
        }
    }

    @Override
    public UpdateMessage checkUpdates() {
        return updateMessage;
    }
}
