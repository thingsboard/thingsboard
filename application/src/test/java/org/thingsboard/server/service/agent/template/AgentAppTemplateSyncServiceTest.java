// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.EdgeUpgradeMessageV2;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.service.agent.template.AppTemplateMaterializer.AppVersionDescriptor;
import org.thingsboard.server.service.agent.template.AppTemplateMaterializer.MaterializationResult;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentAppTemplateSyncServiceTest {

    @Mock
    private GitSyncService gitSyncService;
    @Mock
    private AppTemplateRegistry templateRegistry;
    @Mock
    private AppTemplateMaterializer materializer;
    @Mock
    private ProjectInfo projectInfo;

    private AgentAppTemplateSyncService newService(String platformVersion) {
        when(projectInfo.getProjectVersion()).thenReturn(platformVersion);
        return new AgentAppTemplateSyncService(gitSyncService, templateRegistry, materializer, projectInfo);
    }

    @Test
    void initRegistersTheGitSyncWithoutTouchingTheRepository() throws Exception {
        AgentAppTemplateSyncService service = newService("4.4.1");
        ReflectionTestUtils.setField(service, "repoUri", "https://github.com/thingsboard/agent-app-templates");
        ReflectionTestUtils.setField(service, "branch", "main");
        ReflectionTestUtils.setField(service, "fetchFrequencyMs", 3600000L);
        try {
            service.init();

            verify(gitSyncService).registerSync(eq("agent-app-templates"), eq("https://github.com/thingsboard/agent-app-templates"),
                    eq("main"), eq(3600000L), any(Runnable.class));
            verify(gitSyncService, never()).listFiles(any(), any(), anyInt(), any());
        } finally {
            service.destroy();
        }
    }

    @Test
    void refreshSkipsUntilTheRepositoryIsSynced() {
        AgentAppTemplateSyncService service = serviceWithEdgeTemplate();

        service.refresh();

        verify(gitSyncService, never()).listFiles(any(), any(), anyInt(), any());
        verify(templateRegistry, never()).replace(any(), any(), any(), any());
    }

    @Test
    void repositoryUpdateRebuildsTheTemplatesOffTheGitSyncThread() {
        AgentAppTemplateSyncService service = serviceWithEdgeTemplate();
        stubEdgeVersionGraph(service, edgeGraph("4.4.0.2"));
        try {
            service.onRepositoryUpdate();

            verify(templateRegistry, timeout(5000)).replace(eq(AgentApplicationType.EDGE), eq(AgentAppConfigType.DOCKER_COMPOSE),
                    argThat(byVersion -> byVersion.containsKey("4.4.0.2EDGEPE")), any());
        } finally {
            service.destroy();
        }
    }

    @Test
    void refreshPicksUpVersionsAddedToTheUpdateServerWithoutARepositoryChange() {
        AgentAppTemplateSyncService service = serviceWithEdgeTemplate();
        stubEdgeVersionGraph(service, edgeGraph("4.4.0.2"), edgeGraph("4.4.0.2", "4.4.1"));
        ReflectionTestUtils.setField(service, "repositorySynced", true);

        service.refresh();
        service.refresh();

        ArgumentCaptor<Map<String, AgentAppTemplate>> byVersion = ArgumentCaptor.forClass(Map.class);
        verify(templateRegistry, times(2)).replace(eq(AgentApplicationType.EDGE), eq(AgentAppConfigType.DOCKER_COMPOSE),
                byVersion.capture(), any());
        assertThat(byVersion.getAllValues().get(0)).containsOnlyKeys("4.4.0.2EDGEPE");
        assertThat(byVersion.getAllValues().get(1)).containsOnlyKeys("4.4.0.2EDGEPE", "4.4.1EDGE");
    }

    @Test
    void filterDropsVersionsAboveThePlatform() {
        AgentAppTemplateSyncService service = newService("4.2.0PE");

        List<AppVersionDescriptor> result = service.filterCurrentlySupportedVersions(List.of(
                new AppVersionDescriptor("4.1.0", "4.2.0", false),
                new AppVersionDescriptor("4.2.0", "4.3.0", false),
                new AppVersionDescriptor("4.3.0", null, false)));

        assertThat(result).extracting(AppVersionDescriptor::getVersion).containsExactly("4.1.0", "4.2.0");
    }

    @Test
    void filterSeversNextVersionWhenSuccessorIsAbovePlatform() {
        AgentAppTemplateSyncService service = newService("4.2.0PE");

        List<AppVersionDescriptor> result = service.filterCurrentlySupportedVersions(List.of(
                new AppVersionDescriptor("4.1.0", "4.2.0", false),
                new AppVersionDescriptor("4.2.0", "4.3.0", true)));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNextVersion()).isEqualTo("4.2.0");
        assertThat(result.get(1).getNextVersion()).isNull();
        assertThat(result.get(1).isRequiresUpdateDb()).isTrue();
    }

    @Test
    void filterKeepsNullNextVersionAndEmptyInput() {
        AgentAppTemplateSyncService service = newService("4.2.0PE");

        assertThat(service.filterCurrentlySupportedVersions(List.of())).isEmpty();

        List<AppVersionDescriptor> result = service.filterCurrentlySupportedVersions(
                Arrays.asList(new AppVersionDescriptor("4.2.0", null, false)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNextVersion()).isNull();
    }

    @Test
    void filterDropsEverythingWhenPlatformIsOlderThanEveryVersion() {
        AgentAppTemplateSyncService service = newService("3.9.0PE");

        List<AppVersionDescriptor> result = service.filterCurrentlySupportedVersions(List.of(
                new AppVersionDescriptor("4.1.0", "4.2.0", false),
                new AppVersionDescriptor("4.2.0", null, false)));

        assertThat(result).isEmpty();
    }

    @Test
    void filterVersionsWithoutTemplatesDropsVersionsBelowMinSupported() {
        List<AppVersionDescriptor> result = AgentAppTemplateSyncService.filterVersionsWithoutTemplates(List.of(
                new AppVersionDescriptor("3.8.0", "3.9.0", false),
                new AppVersionDescriptor("3.9.0", "3.9.1", false),
                new AppVersionDescriptor("3.9.1", null, false)), "3.9", AgentApplicationType.EDGE);

        assertThat(result).extracting(AppVersionDescriptor::getVersion).containsExactly("3.9.0", "3.9.1");
    }

    @Test
    void filterVersionsWithoutTemplatesKeepsNonNumericVersions() {
        List<AppVersionDescriptor> result = AgentAppTemplateSyncService.filterVersionsWithoutTemplates(List.of(
                new AppVersionDescriptor("3.2", "3.3", false),
                new AppVersionDescriptor("3.3", "3.8-stable", false),
                new AppVersionDescriptor("3.8-stable", null, false),
                new AppVersionDescriptor("latest", null, false)), "3.3", AgentApplicationType.GATEWAY);

        assertThat(result).extracting(AppVersionDescriptor::getVersion)
                .containsExactly("3.3", "3.8-stable", "latest");
    }

    @Test
    void filterVersionsWithoutTemplatesKeepsEverythingWhenMinVersionIsBlank() {
        List<AppVersionDescriptor> descriptors = List.of(
                new AppVersionDescriptor("3.0", "3.1", false),
                new AppVersionDescriptor("3.1", null, false));

        assertThat(AgentAppTemplateSyncService.filterVersionsWithoutTemplates(descriptors, "", AgentApplicationType.GATEWAY))
                .isEqualTo(descriptors);
        assertThat(AgentAppTemplateSyncService.filterVersionsWithoutTemplates(descriptors, null, AgentApplicationType.GATEWAY))
                .isEqualTo(descriptors);
    }

    @Test
    void filterVersionsWithoutTemplatesSeversNextVersionPointingAtDroppedVersion() {
        List<AppVersionDescriptor> result = AgentAppTemplateSyncService.filterVersionsWithoutTemplates(List.of(
                new AppVersionDescriptor("3.9.0", "3.8.0", true),
                new AppVersionDescriptor("3.8.0", null, false)), "3.9", AgentApplicationType.EDGE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo("3.9.0");
        assertThat(result.get(0).getNextVersion()).isNull();
        assertThat(result.get(0).isRequiresUpdateDb()).isTrue();
    }

    @Test
    void appendEdgeVersionSuffixRewritesBothVersionAndNextVersion() {
        List<AppVersionDescriptor> result = AgentAppTemplateSyncService.appendEdgeVersionSuffix(List.of(
                new AppVersionDescriptor("4.1.0", "4.2.0", true),
                new AppVersionDescriptor("4.2.0", null, false)));

        assertThat(result).extracting(AppVersionDescriptor::getVersion)
                .containsExactly("4.1.0EDGEPE", "4.2.0EDGEPE");
        assertThat(result.get(0).getNextVersion()).isEqualTo("4.2.0EDGEPE");
        assertThat(result.get(0).isRequiresUpdateDb()).isTrue();
        assertThat(result.get(1).getNextVersion()).isNull();
    }

    @Test
    void appendEdgeVersionSuffixSwitchesToCeStyleFrom441() {
        List<AppVersionDescriptor> result = AgentAppTemplateSyncService.appendEdgeVersionSuffix(List.of(
                new AppVersionDescriptor("4.3.1.6", "4.4.0", true),
                new AppVersionDescriptor("4.4.0", "4.4.0.1", false),
                new AppVersionDescriptor("4.4.0.1", "4.4.1", false),
                new AppVersionDescriptor("4.4.1", null, false)));

        assertThat(result).extracting(AppVersionDescriptor::getVersion)
                .containsExactly("4.3.1.6EDGEPE", "4.4.0EDGEPE", "4.4.0.1EDGEPE", "4.4.1EDGE");
        assertThat(result).extracting(AppVersionDescriptor::getNextVersion)
                .containsExactly("4.4.0EDGEPE", "4.4.0.1EDGEPE", "4.4.1EDGE", null);
    }

    @Test
    void baseTemplatesAreUsedBelowEverySinceFolder() {
        AgentAppTemplateSyncService service = serviceWithTemplateFiles("4.4.0PE");

        assertThat(service.getTemplateFiles()).extracting(RepoFile::path).containsExactlyInAnyOrder(
                "templates/template-EDGE-DOCKER_COMPOSE.json",
                "templates/template-GATEWAY-DOCKER_COMPOSE.json");
    }

    @Test
    void sinceFolderReplacesOnlyTheTemplatesItContains() {
        AgentAppTemplateSyncService service = serviceWithTemplateFiles("4.4.1PE-SNAPSHOT");

        assertThat(service.getTemplateFiles()).extracting(RepoFile::path).containsExactlyInAnyOrder(
                "templates/since/4.4.1/template-EDGE-DOCKER_COMPOSE.json",
                "templates/template-GATEWAY-DOCKER_COMPOSE.json");
    }

    @Test
    void versionWithoutItsOwnSinceFolderUsesTheNewestLowerOne() {
        AgentAppTemplateSyncService service = serviceWithTemplateFiles("4.4.2");

        assertThat(service.getTemplateFiles()).extracting(RepoFile::path).containsExactlyInAnyOrder(
                "templates/since/4.4.1/template-EDGE-DOCKER_COMPOSE.json",
                "templates/template-GATEWAY-DOCKER_COMPOSE.json");
    }

    @Test
    void newestApplicableSinceFolderWinsPerTemplate() {
        AgentAppTemplateSyncService service = serviceWithTemplateFiles("4.10.0");

        assertThat(service.getTemplateFiles()).extracting(RepoFile::path).containsExactlyInAnyOrder(
                "templates/since/4.9/template-EDGE-DOCKER_COMPOSE.json",
                "templates/since/4.5/template-GATEWAY-DOCKER_COMPOSE.json");
    }

    private AgentAppTemplateSyncService serviceWithTemplateFiles(String platformVersion) {
        AgentAppTemplateSyncService service = newService(platformVersion);
        ReflectionTestUtils.setField(service, "basePath", "templates");
        when(gitSyncService.listFiles("agent-app-templates", "templates", 1, FileType.FILE)).thenReturn(List.of(
                file("templates/template-EDGE-DOCKER_COMPOSE.json"),
                file("templates/template-GATEWAY-DOCKER_COMPOSE.json"),
                file("templates/README.md")));
        when(gitSyncService.listFiles("agent-app-templates", "templates/since", 3, FileType.FILE)).thenReturn(List.of(
                file("templates/since/4.9/template-EDGE-DOCKER_COMPOSE.json"),
                file("templates/since/4.4.1/template-EDGE-DOCKER_COMPOSE.json"),
                file("templates/since/4.5/template-GATEWAY-DOCKER_COMPOSE.json"),
                file("templates/since/4.11/template-GATEWAY-DOCKER_COMPOSE.json"),
                file("templates/since/draft/template-EDGE-DOCKER_COMPOSE.json")));
        return service;
    }

    private AgentAppTemplateSyncService serviceWithEdgeTemplate() {
        AgentAppTemplateSyncService service = newService("4.4.1");
        ReflectionTestUtils.setField(service, "basePath", "templates");
        ReflectionTestUtils.setField(service, "minSupportedEdgeVersion", "3.9");
        when(gitSyncService.listFiles("agent-app-templates", "templates", 1, FileType.FILE))
                .thenReturn(List.of(file("templates/template-EDGE-DOCKER_COMPOSE.json")));
        when(gitSyncService.getFileContent("agent-app-templates", "templates/template-EDGE-DOCKER_COMPOSE.json"))
                .thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        when(materializer.materialize(any(), eq(AgentApplicationType.EDGE), eq(AgentAppConfigType.DOCKER_COMPOSE), anyList(), any(), any()))
                .thenAnswer(invocation -> {
                    List<AppVersionDescriptor> descriptors = invocation.getArgument(3);
                    Map<String, AgentAppTemplate> byVersion = new LinkedHashMap<>();
                    descriptors.forEach(d -> byVersion.put(d.getVersion(), new AgentAppTemplate()));
                    return new MaterializationResult(byVersion, null);
                });
        return service;
    }

    private static void stubEdgeVersionGraph(AgentAppTemplateSyncService service, EdgeUpgradeMessageV2 first, EdgeUpgradeMessageV2... next) {
        RestTemplate restClient = mock(RestTemplate.class);
        when(restClient.getForObject("https://updates.thingsboard.io/api/v2/edge/upgradeMapping", EdgeUpgradeMessageV2.class))
                .thenReturn(first, next);
        ReflectionTestUtils.setField(service, "restClient", restClient);
        ReflectionTestUtils.setField(service, "updateServerBaseUrl", "https://updates.thingsboard.io");
    }

    private static EdgeUpgradeMessageV2 edgeGraph(String... versions) {
        Map<String, List<EdgeUpgradeInfo>> graph = new LinkedHashMap<>();
        for (int i = 0; i < versions.length; i++) {
            String next = i + 1 < versions.length ? versions[i + 1] : null;
            graph.put(versions[i], List.of(new EdgeUpgradeInfo(false, next)));
        }
        return new EdgeUpgradeMessageV2(graph);
    }

    private static RepoFile file(String path) {
        return new RepoFile(path, path.substring(path.lastIndexOf('/') + 1), FileType.FILE);
    }
}
