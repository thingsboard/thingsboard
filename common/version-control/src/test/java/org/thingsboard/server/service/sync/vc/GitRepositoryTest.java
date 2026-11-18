// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GitRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    public void directoryIsResolvedDirectlyUnderRepositoriesFolder() {
        assertThat(GitRepository.resolveDirectory(tempDir.toString(), "repo1"))
                .isEqualTo(tempDir.toAbsolutePath().normalize().resolve("repo1"));
    }

    @Test
    public void emptyDirectoryNameIsRejected() {
        assertThatThrownBy(() -> GitRepository.resolveDirectory(tempDir.toString(), ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void directoryNameThatIsNotNormalizedIsRejected() {
        assertThatThrownBy(() -> GitRepository.resolveDirectory(tempDir.toString(), "./repo1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
