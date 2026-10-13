// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.service.agent.template.AppTemplateMaterializer.AppVersionDescriptor;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.sync.GitSyncService;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void appendVersionSuffixRewritesBothVersionAndNextVersion() {
        List<AppVersionDescriptor> result = AgentAppTemplateSyncService.appendVersionSuffix(List.of(
                new AppVersionDescriptor("4.1.0", "4.2.0", true),
                new AppVersionDescriptor("4.2.0", null, false)), "EDGEPE");

        assertThat(result).extracting(AppVersionDescriptor::getVersion)
                .containsExactly("4.1.0EDGEPE", "4.2.0EDGEPE");
        assertThat(result.get(0).getNextVersion()).isEqualTo("4.2.0EDGEPE");
        assertThat(result.get(0).isRequiresUpdateDb()).isTrue();
        assertThat(result.get(1).getNextVersion()).isNull();
    }
}
