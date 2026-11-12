// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultGitRepositoryServiceTest {

    @TempDir
    Path tempDir;

    private DefaultGitRepositoryService service;
    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @BeforeEach
    public void setUp() {
        service = new DefaultGitRepositoryService();
        ReflectionTestUtils.setField(service, "repositoriesFolder", tempDir.toString());
    }

    @Test
    public void localRepositoryIsCreatedDirectlyUnderRepositoriesFolder() throws Exception {
        RepositorySettings settings = localSettings("repo1");

        service.initRepository(tenantId, settings, false);

        assertThat(tempDir.resolve("local_repo1").resolve(".git")).isDirectory();
        assertThat(service.getRepositorySettings(tenantId)).isEqualTo(settings);
    }

    @Test
    public void localRepositoryNameWithMultipleSegmentsIsRejected() throws Exception {
        Path marker = tempDir.resolve("keep").resolve("marker.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "content");

        assertThatThrownBy(() -> service.initRepository(tenantId, localSettings("repo1/main"), false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(tempDir.resolve("local_repo1")).doesNotExist();
        assertThat(marker).exists();
        assertThat(service.getRepositorySettings(tenantId)).isNull();
    }

    private RepositorySettings localSettings(String repositoryUri) {
        RepositorySettings settings = new RepositorySettings();
        settings.setRepositoryUri(repositoryUri);
        settings.setDefaultBranch("main");
        settings.setLocalOnly(true);
        return settings;
    }

}
