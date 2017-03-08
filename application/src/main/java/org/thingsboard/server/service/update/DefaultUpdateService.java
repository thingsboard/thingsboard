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

package org.thingsboard.server.service.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.service.update.model.UpdateMessage;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DefaultUpdateService implements UpdateService {

    private static final String INSTANCE_ID_FILE = ".instance_id";
    private static final String UPDATE_SERVER_BASE_URL = "https://updates.thingsboard.io";

    private static final String PLATFORM_PARAM = "platform";
    private static final String VERSION_PARAM = "version";
    private static final String INSTANCE_ID_PARAM = "instanceId";

    @Value("${updates.enabled}")
    private boolean updatesEnabled;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture checkUpdatesFuture = null;
    private RestTemplate restClient = new RestTemplate();

    private UpdateMessage updateMessage;

    private String platform;
    private String version;
    private UUID instanceId = null;

    @PostConstruct
    private void init() {
        updateMessage = new UpdateMessage("", false);
        if (updatesEnabled) {
            try {
                platform = System.getProperty("platform", "unknown");
                version = getClass().getPackage().getImplementationVersion();
                if (version == null) {
                    version = "unknown";
                }
                Path instanceIdPath = Paths.get(INSTANCE_ID_FILE);
                if (Files.exists(instanceIdPath)) {
                    byte[] data = Files.readAllBytes(instanceIdPath);
                    if (data != null && data.length > 0) {
                        try {
                            instanceId = UUID.fromString(new String(data));
                        } catch (IllegalArgumentException e) {
                        }
                    }
                }
                if (instanceId == null) {
                    instanceId = UUID.randomUUID();
                    Files.write(instanceIdPath, instanceId.toString().getBytes());
                }
                checkUpdatesFuture = scheduler.scheduleAtFixedRate(checkUpdatesRunnable, 0, 1, TimeUnit.HOURS);
            } catch (Exception e) {}
        }
    }

    @PreDestroy
    private void destroy() {
        try {
            if (checkUpdatesFuture != null) {
                checkUpdatesFuture.cancel(true);
            }
            scheduler.shutdownNow();
        } catch (Exception e) {}
    }

    Runnable checkUpdatesRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                log.trace("Executing check update method for instanceId [{}], platform [{}] and version [{}]", instanceId, platform, version);
                ObjectNode request = new ObjectMapper().createObjectNode();
                request.put(PLATFORM_PARAM, platform);
                request.put(VERSION_PARAM, version);
                request.put(INSTANCE_ID_PARAM, instanceId.toString());
                JsonNode response = restClient.postForObject(UPDATE_SERVER_BASE_URL+"/api/thingsboard/updates", request, JsonNode.class);
                updateMessage = new UpdateMessage(
                        response.get("message").asText(),
                        response.get("updateAvailable").asBoolean()
                );
            } catch (Exception e) {
                log.trace(e.getMessage());
            }
        }
    };

    @Override
    public UpdateMessage checkUpdates() {
        return updateMessage;
    }

}
