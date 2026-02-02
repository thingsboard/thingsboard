/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "transport.gateway.dashboard.sync.enabled", havingValue = "true")
public class DashboardSyncService {

    private final GitSyncService gitSyncService;
    private final ResourceService resourceService;
    private final ImageService imageService;
    private final WidgetsBundleService widgetsBundleService;
    private final PartitionService partitionService;
    private final ProjectInfo projectInfo;

    @Value("${transport.gateway.dashboard.sync.repository_url:}")
    private String repoUrl;
    @Value("${transport.gateway.dashboard.sync.branch:}")
    private String branch;
    @Value("${transport.gateway.dashboard.sync.fetch_frequency:24}")
    private int fetchFrequencyHours;

    private static final String REPO_KEY = "gateways-dashboard";
    private static final String GATEWAYS_DASHBOARD_KEY = "gateways_dashboard.json";

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() throws Exception {
        if (StringUtils.isBlank(branch)) {
            branch = "release/" + projectInfo.getProjectVersion();
        }
        gitSyncService.registerSync(REPO_KEY, repoUrl, branch, TimeUnit.HOURS.toMillis(fetchFrequencyHours), this::update);
    }

    private void update() {
        if (!partitionService.isMyPartition(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)) {
            return;
        }

        List<RepoFile> resources = listFiles("resources");
        for (RepoFile resourceFile : resources) {
            byte[] data = getFileContent(resourceFile.path());
            resourceService.createOrUpdateSystemResource(ResourceType.JS_MODULE, ResourceSubType.EXTENSION, resourceFile.name(), data);
        }
        List<RepoFile> images = listFiles("images");
        for (RepoFile imageFile : images) {
            byte[] data = getFileContent(imageFile.path());
            imageService.createOrUpdateSystemImage(imageFile.name(), data);
        }

        Stream<String> widgetsBundles = listFiles("widget_bundles").stream()
                .map(widgetsBundleFile -> new String(getFileContent(widgetsBundleFile.path()), StandardCharsets.UTF_8));
        Stream<String> widgetTypes = listFiles("widget_types").stream()
                .map(widgetTypeFile -> new String(getFileContent(widgetTypeFile.path()), StandardCharsets.UTF_8));
        widgetsBundleService.updateSystemWidgets(widgetsBundles, widgetTypes);

        RepoFile dashboardFile = listFiles("dashboards").get(0);
        resourceService.createOrUpdateSystemResource(ResourceType.DASHBOARD, null, GATEWAYS_DASHBOARD_KEY, getFileContent(dashboardFile.path()));

        log.info("Gateways dashboard sync completed");
    }

    private List<RepoFile> listFiles(String path) {
        return gitSyncService.listFiles(REPO_KEY, path, 1, FileType.FILE);
    }

    private byte[] getFileContent(String path) {
        return gitSyncService.getFileContent(REPO_KEY, path);
    }

}
