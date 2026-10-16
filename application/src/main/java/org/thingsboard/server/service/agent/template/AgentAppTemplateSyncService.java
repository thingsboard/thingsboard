// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.common.util.TbVersionUtils;
import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.EdgeUpgradeMessageV2;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.edge.EdgeEditionStyle;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.template.AppTemplateMaterializer.AppVersionDescriptor;
import org.thingsboard.server.service.agent.template.AppTemplateMaterializer.MaterializationResult;
import org.thingsboard.server.service.edge.instructions.EdgeVersionGraphResolver;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Syncs abstract agent-app templates from a git repo and materializes concrete per-version templates into the
 * in-memory {@link AppTemplateRegistry}. One abstract template per app type ({@code template-<APP>-<CONFIG>.json}) is
 * expanded against the version graph served by the update server ({@code /api/v2/edge/upgradeMapping},
 * {@code /api/v1/gateway/upgradeMapping}); GENERIC has no graph and yields a single unversioned template.
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "agents.appTemplates.sync.enabled", havingValue = "true")
public class AgentAppTemplateSyncService {

    private static final Pattern TEMPLATE_FILE_PATTERN = Pattern.compile(
            "^template-(?<appType>[A-Z0-9_]+)-(?<configType>[A-Z0-9_]+)\\.json$"
    );
    private static final int APP_TYPE_GROUP_NUM = 1;
    private static final int CONFIG_TYPE_GROUP_NUM = 2;
    private static final String REPO_KEY = "agent-app-templates";
    // <basePath>/since/<version>/ holds templates that replace the same-named base ones from that platform version on
    private static final String SINCE_DIR = "since";
    private static final Duration UPDATE_SERVER_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration UPDATE_SERVER_READ_TIMEOUT = Duration.ofSeconds(30);

    private final GitSyncService gitSyncService;
    private final AppTemplateRegistry templateRegistry;
    private final AppTemplateMaterializer materializer;
    private final ProjectInfo projectInfo;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final RestTemplate restClient = newUpdateServerRestTemplate();

    @Value("${agents.appTemplates.sync.repoUri:}")
    private String repoUri;
    @Value("${agents.appTemplates.sync.branch:}")
    private String branch;
    @Value("${agents.appTemplates.sync.basePath:templates}")
    private String basePath;
    @Value("${agents.appTemplates.sync.fetchFrequencyMs:3600000}")
    private long fetchFrequencyMs;
    @Value("${agents.appTemplates.updateServerBaseUrl:https://updates.thingsboard.io}")
    private String updateServerBaseUrl;
    @Value("${agents.appTemplates.minSupportedEdgeVersion:3.9}")
    private String minSupportedEdgeVersion;
    @Value("${agents.appTemplates.minSupportedGatewayVersion:3.3}")
    private String minSupportedGatewayVersion;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() throws Exception {
        if (StringUtils.isBlank(repoUri)) {
            log.warn("Agent app template sync is enabled but repoUri is empty");
            return;
        }
        if (StringUtils.isBlank(branch)) {
            branch = "main";
        }
        gitSyncService.registerSync(REPO_KEY, repoUri, branch, fetchFrequencyMs, this::update);
    }

    private void update() {
        Set<AppTemplateRegistry.Key> presentKeys = new HashSet<>();
        for (RepoFile file : getTemplateFiles()) {
            try {
                AgentApplicationType appType = AgentApplicationType.valueOf(
                        RegexUtils.getMatch(file.name(), TEMPLATE_FILE_PATTERN, APP_TYPE_GROUP_NUM));
                AgentAppConfigType configType = AgentAppConfigType.valueOf(
                        RegexUtils.getMatch(file.name(), TEMPLATE_FILE_PATTERN, CONFIG_TYPE_GROUP_NUM));
                presentKeys.add(new AppTemplateRegistry.Key(appType, configType));

                JsonNode rawTemplate = JacksonUtil.fromBytes(gitSyncService.getFileContent(REPO_KEY, file.path()));
                List<AppVersionDescriptor> descriptors = resolveDescriptors(appType);
                if (CollectionsUtil.isEmpty(descriptors)) {
                    log.warn("Resolved no versions for {}-{} (the version graph is empty or unreachable); " +
                            "keeping the previously registered templates", appType, configType);
                    continue;
                }
                MaterializationResult result = materializer.materialize(
                        rawTemplate, appType, configType, descriptors, composeLoader(), folderLister());
                if (result.getByVersion().isEmpty()) {
                    log.warn("Materialized no versions for {}-{} (the version graph is empty or unreachable); " +
                            "keeping the previously registered templates", appType, configType);
                    continue;
                }
                templateRegistry.replace(appType, configType, result.getByVersion(), result.getLatest());
            } catch (Exception e) {
                log.error("Failed to sync agent app template file: {}", file.name(), e);
            }
        }
        templateRegistry.retainOnly(presentKeys);
        log.info("Agent app template sync completed");
    }

    private static RestTemplate newUpdateServerRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(UPDATE_SERVER_CONNECT_TIMEOUT);
        factory.setReadTimeout(UPDATE_SERVER_READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    /**
     * Base templates overridden per file name by {@code since/<version>/} ones, taking the newest folder whose version
     * is not above the platform version. Servers older than a {@code since} folder only list the base folder, so the
     * templates there are free to rely on newer materialization variables.
     */
    List<RepoFile> getTemplateFiles() {
        String platform = TbVersionUtils.extractStartingDigits(projectInfo.getProjectVersion());
        Map<String, List<RepoFile>> sinceFilesByVersion = listTemplateFiles(basePath + "/" + SINCE_DIR, 2).stream()
                .collect(Collectors.groupingBy(AgentAppTemplateSyncService::sinceVersion));
        Map<String, RepoFile> byName = new LinkedHashMap<>();
        for (String version : VersionFolders.applicableTo(sinceFilesByVersion.keySet(), platform)) {
            sinceFilesByVersion.get(version).forEach(file -> byName.putIfAbsent(file.name(), file));
        }
        listTemplateFiles(basePath, 1).forEach(file -> byName.putIfAbsent(file.name(), file));
        return new ArrayList<>(byName.values());
    }
    // Template files the given number of folders below path
    private List<RepoFile> listTemplateFiles(String path, int levels) {
        return gitSyncService.listFiles(REPO_KEY, path, depthBelow(path, levels), FileType.FILE).stream()
                .filter(file -> RegexUtils.matches(file.name(), TEMPLATE_FILE_PATTERN))
                .collect(Collectors.toList());
    }

    // Name of the folder holding the file, e.g. templates/since/4.4.1/template-EDGE-DOCKER_COMPOSE.json -> 4.4.1
    // Tree depth of the entries the given number of folders below path (root entries are at depth 0)
    private static int depthBelow(String path, int levels) {
        return path.split("/").length - 1 + levels;
    }

    private static String sinceVersion(RepoFile file) {
        String folder = file.path().substring(0, file.path().lastIndexOf('/'));
        return folder.substring(folder.lastIndexOf('/') + 1);
    }

    // Names of the folders directly under a repo path, cached per template since every version lists the same folders
    private Function<String, List<String>> folderLister() {
        Map<String, List<String>> cache = new HashMap<>();
        return path -> cache.computeIfAbsent(path, p -> gitSyncService.listFiles(REPO_KEY, p, depthBelow(p, 1), FileType.DIRECTORY)
                .stream().map(RepoFile::name).toList());
    }

    private Function<String, JsonNode> composeLoader() {
        // Returns null when the file is absent so the materializer can probe more/less specific
        // version subfolders (e.g. compose/edge/4.3/4.3.1 before compose/edge/4.3) and pick the closest match.
        return path -> {
            byte[] data;
            try {
                data = gitSyncService.getFileContent(REPO_KEY, path);
            } catch (Exception e) {
                return null;
            }
            return readComposeYaml(new String(data, StandardCharsets.UTF_8));
        };
    }

    private List<AppVersionDescriptor> resolveDescriptors(AgentApplicationType appType) {
        return switch (appType) {
            case EDGE -> appendEdgeVersionSuffix(filterCurrentlySupportedVersions(
                    filterVersionsWithoutTemplates(edgeDescriptors(), minSupportedEdgeVersion, appType)));
            case GATEWAY -> filterVersionsWithoutTemplates(
                    buildDescriptors(gatewayVersionGraph(), "gatewayVersions", "nextGatewayVersion", false),
                    minSupportedGatewayVersion, appType);
            case GENERIC -> Collections.singletonList(new AppVersionDescriptor(appType.getDefaultVersion(), null, false));
        };
    }

    // edge image tags carry an edition suffix that depends on the version itself (e.g. 4.4.0EDGEPE vs 4.4.1EDGE)
    static List<AppVersionDescriptor> appendEdgeVersionSuffix(List<AppVersionDescriptor> descriptors) {
        if (CollectionsUtil.isEmpty(descriptors)) {
            return descriptors;
        }
        return descriptors.stream()
                .map(d -> new AppVersionDescriptor(
                        withEdgeVersionSuffix(d.getVersion()),
                        withEdgeVersionSuffix(d.getNextVersion()),
                        d.isRequiresUpdateDb()))
                .collect(Collectors.toList());
    }

    private static String withEdgeVersionSuffix(String version) {
        return version == null ? null : EdgeEditionStyle.withVersionSuffix(version);
    }

    List<AppVersionDescriptor> filterCurrentlySupportedVersions(List<AppVersionDescriptor> descriptors) {
        if (CollectionsUtil.isEmpty(descriptors)) {
            return descriptors;
        }
        String platform = TbVersionUtils.extractStartingDigits(projectInfo.getProjectVersion());
        return retainVersions(descriptors,
                version -> TbVersionUtils.compare(TbVersionUtils.extractStartingDigits(version), platform) <= 0);
    }

    /**
     * Drops the version-graph nodes older than {@code minVersion}, the oldest release the templates repo publishes
     * compose files for, so that the materializer is never asked for a template that cannot exist. Versions with no
     * numeric part (the gateway 'latest' tag) are not comparable and are always kept.
     */
    static List<AppVersionDescriptor> filterVersionsWithoutTemplates(List<AppVersionDescriptor> descriptors,
                                                                    String minVersion, AgentApplicationType appType) {
        if (StringUtils.isBlank(minVersion)) {
            return descriptors;
        }
        List<AppVersionDescriptor> result = retainVersions(descriptors, version -> {
            String numericVersion = TbVersionUtils.extractStartingDigits(version);
            return numericVersion.isEmpty() || TbVersionUtils.compare(numericVersion, minVersion) >= 0;
        });
        if (result.size() < descriptors.size()) {
            log.debug("Skipped {} {} version(s) below the min supported version {}",
                    descriptors.size() - result.size(), appType, minVersion);
        }
        return result;
    }

    private static List<AppVersionDescriptor> retainVersions(List<AppVersionDescriptor> descriptors,
                                                             Predicate<String> supported) {
        Set<String> retained = descriptors.stream()
                .map(AppVersionDescriptor::getVersion)
                .filter(supported)
                .collect(Collectors.toSet());

        return descriptors.stream()
                .filter(d -> retained.contains(d.getVersion()))
                .map(d -> new AppVersionDescriptor(d.getVersion(),
                        retained.contains(d.getNextVersion()) ? d.getNextVersion() : null,
                        d.isRequiresUpdateDb()))
                .collect(Collectors.toList());
    }

    private List<AppVersionDescriptor> edgeDescriptors() {
        EdgeUpgradeMessageV2 graph;
        try {
            graph = restClient.getForObject(updateServerBaseUrl + "/api/v2/edge/upgradeMapping", EdgeUpgradeMessageV2.class);
        } catch (Exception e) {
            log.warn("Failed to fetch edge install/upgrade mapping from the update server: {}", e.getMessage());
            return Collections.emptyList();
        }
        if (graph == null || graph.getEdgeVersions() == null) {
            return new ArrayList<>();
        }
        Map<String, EdgeUpgradeInfo> resolved = EdgeVersionGraphResolver.resolve(graph.getEdgeVersions(), projectInfo.getProjectVersion());
        return resolved.entrySet().stream()
                .map(e -> new AppVersionDescriptor(
                        e.getKey(), e.getValue().getNextEdgeVersion(), e.getValue().isRequiresUpdateDb()))
                .collect(Collectors.toList());
    }

    private JsonNode gatewayVersionGraph() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = JacksonUtil.newObjectNode().put("version", "").toString();

        try {
            return restClient.postForObject(updateServerBaseUrl + "/api/v1/gateway/upgradeMapping",
                    new HttpEntity<>(body, headers), JsonNode.class);
        } catch (Exception e) {
            log.warn("Failed to fetch gateway version graph from the update server: {}", e.getMessage());
            return null;
        }
    }

    private List<AppVersionDescriptor> buildDescriptors(JsonNode graph, String mapField, String nextField, boolean hasDbFlag) {
        List<AppVersionDescriptor> descriptors = new ArrayList<>();
        if (graph == null || !graph.hasNonNull(mapField)) {
            return descriptors;
        }
        Iterator<Map.Entry<String, JsonNode>> it = graph.get(mapField).fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode info = entry.getValue();
            String nextVersion = getNextVersion(nextField, info);
            boolean requiresUpdateDb = isRequiresUpdateDb(hasDbFlag, info);

            descriptors.add(new AppVersionDescriptor(entry.getKey(), nextVersion, requiresUpdateDb));
        }
        return descriptors;
    }

    private static boolean isRequiresUpdateDb(boolean hasDbFlag, JsonNode info) {
        return hasDbFlag && info != null && info.path("requiresUpdateDb").asBoolean(false);
    }

    private static String getNextVersion(String nextField, JsonNode info) {
        return info != null && info.hasNonNull(nextField) ? info.get(nextField).asText() : null;
    }

    private JsonNode readComposeYaml(String composeYaml) {
        if (StringUtils.isBlank(composeYaml)) {
            return null;
        }
        try {
            return yamlMapper.readTree(composeYaml);
        } catch (Exception e) {
            log.warn("Failed to parse compose yaml to json: {}", composeYaml, e);
            return null;
        }
    }
}
