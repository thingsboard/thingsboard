// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync;

import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.util.List;

public interface GitSyncService {

    void registerSync(String key, String repoUri, String branch, long fetchFrequencyMs, Runnable onUpdate);

    List<RepoFile> listFiles(String key, String path, int depth, FileType type);

    byte[] getFileContent(String key, String path);

    String getGithubRawContentUrl(String key, String path);

}
