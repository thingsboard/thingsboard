// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync;

import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.util.List;

public interface GitSyncService {

    default void registerSync(String key, String repoUri, String branch, long fetchFrequencyMs, Runnable onUpdate) {
        registerSync(key, repoUri, branch, fetchFrequencyMs, onUpdate, false);
    }

    void registerSync(String key, String repoUri, String branch, long fetchFrequencyMs, Runnable onUpdate, boolean notifyOnEveryFetch);

    List<RepoFile> listFiles(String key, String path, int depth, FileType type);

    byte[] getFileContent(String key, String path);

    String getGithubRawContentUrl(String key, String path);

}
