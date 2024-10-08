/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.update;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.edge.EdgeUpdateMapping;
import org.thingsboard.server.common.data.edge.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.edge.EdgeVersionMapping;
import org.thingsboard.server.common.data.mobile.MobileVersionMapping;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.instructions.EdgeInstallInstructionsService;
import org.thingsboard.server.service.edge.instructions.EdgeUpgradeInstructionsService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@TbCoreComponent
public class VersionMappingService {

    private static final String GITHUB_URL = "";
    private static final String EDGE_FILE = "";
    private static final String MOBILE_FILE = "";
    private static final String GATEWAY_FILE = "";

    @Value("${edges.enabled:}")
    protected boolean edgesEnabled;

    @Value("${edges.latest-release-url:}")
    private String edgeLatestReleaseUrl;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired(required = false)
    private EdgeInstallInstructionsService edgeInstallInstructionsService;

    @Autowired(required = false)
    private EdgeUpgradeInstructionsService edgeUpgradeInstructionsService;

    private EdgeUpdateMapping edgeUpdateMapping;
    private MobileVersionMapping mobileVersionMapping;
    private Map<String, String> gatewayMapping;
    private WebClient webClient;
    private String appVersion;

    private volatile String latestEdgeVersion = "";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, ThingsBoardThreadFactory.forName("tb-compatible-service"));

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() {
        appVersion = buildProperties != null ? buildProperties.getVersion() : "unknown";
        webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        scheduler.scheduleAtFixedRate(this::scheduledCheckForUpdates, 0, 1, TimeUnit.HOURS);
    }

    @PreDestroy
    private void destroy() {
        try {
            scheduler.shutdownNow();
        } catch (Exception e) {
            //Do nothing
        }
    }

    private void scheduledCheckForUpdates() {
        refreshEdge();
        refreshMobile();
        refreshGateway();
    }

    private void refreshEdge() {
        if (edgesEnabled) {
            fetchEdgeLatest();
            fetchEdgeMapping();
            updateEdgeInstructions();
        }
    }

    private void fetchEdgeLatest() {
        try {
            String edgeVersion = webClient.get().uri(edgeLatestReleaseUrl).exchangeToMono(r -> r.bodyToMono(String.class)).block();
            if (edgeVersion != null) {
                if (edgeVersion.split("\\.").length == 2) {
                    edgeVersion += ".0";
                }
                latestEdgeVersion = edgeVersion.substring(1);
            }
        } catch (Exception e) {
            log.debug("Failed to fetch the edge version from GitHub", e);
        }
    }

    private void fetchEdgeMapping() {
        try {
            String fileUrl = GITHUB_URL + EDGE_FILE;
            String edgeUpdateFileContent = webClient.get().uri(fileUrl).retrieve().bodyToMono(String.class).block();

            if (edgeUpdateFileContent != null) {
                edgeUpdateMapping = JacksonUtil.fromString(edgeUpdateFileContent, EdgeUpdateMapping.class);
            } else {
                log.debug("Received null response from GitHub for file at {}", fileUrl);
            }
        } catch (Exception e) {
            log.debug("Failed to fetch the edge version from GitHub (File)", e);
        }
    }

    private void refreshMobile() {
        try {
            String supportedMobileVersions = webClient.get().uri(GITHUB_URL + MOBILE_FILE).exchangeToMono(r -> r.bodyToMono(String.class)).block();
            if (supportedMobileVersions != null) {
                mobileVersionMapping = JacksonUtil.fromString(supportedMobileVersions, new TypeReference<>() {
                });
            }
        } catch (Exception e) {
            log.debug("Failed to fetch the edge version from GitHub", e);
        }
    }

    private void refreshGateway() {
        try {
            String gatewayConfiguration = webClient.get().uri(GITHUB_URL + GATEWAY_FILE).exchangeToMono(r -> r.bodyToMono(String.class)).block();
            if (gatewayConfiguration != null) {
                gatewayMapping = JacksonUtil.fromString(gatewayConfiguration, new TypeReference<>() {
                });
            }
        } catch (Exception e) {
            log.debug("Failed to fetch the edge version from GitHub", e);
        }
    }

    private void updateEdgeInstructions() {
        String edgeInstallVersion = getEdgeInstallVersion(appVersion);
        edgeInstallInstructionsService.setAppVersion(edgeInstallVersion);
        edgeUpgradeInstructionsService.setAppVersion(edgeInstallVersion);

        EdgeUpdateMapping mapping = getEdgeUpgradeMapping(appVersion);
        edgeUpgradeInstructionsService.updateInstructionMap(mapping.getEdgeVersions());
    }

    private String getEdgeInstallVersion(String version) {
        String currentVersion = extractStartingDigits(version);

        for (EdgeVersionMapping install : edgeUpdateMapping.getVersionMapping()) {
            if (install.getTbVersion().equals(currentVersion)) {
                return install.getEdgeVersion();
            }
        }
        log.debug("TB version {} was not found in the mapping file. Returning the latest release version.", version);
        return latestEdgeVersion;
    }

    private EdgeUpdateMapping getEdgeUpgradeMapping(String version) {
        String currentVersion = extractStartingDigits(version);
        if (edgeUpdateMapping.getVersionMapping().stream().noneMatch(supported -> supported.getTbVersion().equals(currentVersion))) {
            return EdgeUpdateMapping.builder().edgeVersions(edgeUpdateMapping.getEdgeVersions()).build();
        }
        String supportedEdgeVersion = null;
        for (EdgeVersionMapping supported : edgeUpdateMapping.getVersionMapping()) {
            if (supported.getTbVersion().equals(currentVersion)) {
                supportedEdgeVersion = supported.getEdgeVersion();
            }
        }
        if (supportedEdgeVersion == null) {
            log.debug("Update request does not contain supported Thingsboard version {}, regexMatcher {}", version, currentVersion);
            return EdgeUpdateMapping.builder().edgeVersions(edgeUpdateMapping.getEdgeVersions()).build();
        }
        Map<String, EdgeUpgradeInfo> versionMap = new LinkedHashMap<>();
        for (String edgeVersion : edgeUpdateMapping.getEdgeVersions().keySet()) {
            EdgeUpgradeInfo edgeInfo = edgeUpdateMapping.getEdgeVersions().get(edgeVersion);
            if (supportedEdgeVersion.equals(edgeVersion)) {
                versionMap.put(edgeVersion, new EdgeUpgradeInfo(edgeInfo.isRequiresUpdateDb(), null));
                break;
            }
            versionMap.put(edgeVersion, new EdgeUpgradeInfo(edgeInfo.isRequiresUpdateDb(), edgeInfo.getNextEdgeVersion()));
        }
        return EdgeUpdateMapping.builder().edgeVersions(versionMap).build();
    }

    private static String extractStartingDigits(String input) {
        String regex = "(\\d+(?:\\.\\d+)*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return input;
    }

}
