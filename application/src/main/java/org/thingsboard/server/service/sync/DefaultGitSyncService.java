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
package org.thingsboard.server.service.sync;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.GitRepository;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
public class DefaultGitSyncService implements GitSyncService {

    @Value("${vc.git.repositories-folder:${java.io.tmpdir}/repositories}")
    private String repositoriesFolder;

    private final ScheduledExecutorService executor = ThingsBoardExecutors.newSingleThreadScheduledExecutor("git-sync");
    private final Map<String, GitRepository> repositories = new ConcurrentHashMap<>();
    private final Map<String, Runnable> updateListeners = new ConcurrentHashMap<>();

    @Override
    public void registerSync(String key, String repoUri, String branch, long fetchFrequencyMs, Runnable onUpdate) {
        RepositorySettings settings = new RepositorySettings();
        settings.setRepositoryUri(repoUri);
        settings.setDefaultBranch(branch);
        if (onUpdate != null) {
            updateListeners.put(key, onUpdate);
        }

        executor.execute(() -> {
            initRepository(key, settings);
        });

        executor.scheduleWithFixedDelay(() -> {
            GitRepository repository = repositories.get(key);
            if (repository == null || !GitRepository.exists(repository.getDirectory())) {
                initRepository(key, settings);
                return;
            }

            try {
                log.debug("[{}] Fetching repository", key);
                boolean updated = repository.fetch();
                if (updated) {
                    onUpdate(key);
                } else {
                    log.debug("[{}] No changes in the repository", key);
                }
            } catch (Throwable e) {
                log.error("[{}] Failed to fetch repository", key, e);
            }
        }, fetchFrequencyMs, fetchFrequencyMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<RepoFile> listFiles(String key, String path, int depth, FileType type) {
        GitRepository repository = getRepository(key);
        return repository.listFilesAtCommit(getBranchRef(repository), path, depth).stream()
                .filter(file -> type == null || file.type() == type)
                .toList();
    }


    @Override
    public byte[] getFileContent(String key, String path) {
        GitRepository repository = getRepository(key);
        return repository.getFileContentAtCommit(path, getBranchRef(repository));
    }

    @Override
    public String getGithubRawContentUrl(String key, String path) {
        if (path == null) {
            return "";
        }
        RepositorySettings settings = getRepository(key).getSettings();
        return StringUtils.removeEnd(settings.getRepositoryUri(), ".git") + "/blob/" + settings.getDefaultBranch() + "/" + path + "?raw=true";
    }

    private GitRepository getRepository(String key) {
        GitRepository repository = repositories.get(key);
        if (repository != null) {
            if (!GitRepository.exists(repository.getDirectory())) {
                // reinitializing the repository because folder was deleted
                initRepository(key, repository.getSettings());
            }
        }

        repository = repositories.get(key);
        if (repository == null) {
            throw new IllegalStateException(key + " repository is not initialized");
        }
        return repository;
    }

    private void initRepository(String key, RepositorySettings settings) {
        try {
            repositories.remove(key);
            Path directory = getRepoDirectory(settings);

            GitRepository repository = GitRepository.openOrClone(directory, settings, true);
            repositories.put(key, repository);
            log.info("[{}] Initialized repository", key);

            onUpdate(key);
        } catch (Throwable e) {
            log.error("[{}] Failed to initialize repository with settings {}", key, settings, e);
        }
    }

    private void onUpdate(String key) {
        Runnable listener = updateListeners.get(key);
        if (listener != null) {
            log.debug("[{}] Handling repository update", key);
            try {
                listener.run();
            } catch (Throwable e) {
                log.error("[{}] Failed to handle repository update", key, e);
            }
        }
    }

    private Path getRepoDirectory(RepositorySettings settings) {
        // using uri to define folder name in case repo url is changed
        String name = URI.create(settings.getRepositoryUri()).getPath().replaceAll("[^a-zA-Z]", "");
        return Path.of(repositoriesFolder, name);
    }

    private String getBranchRef(GitRepository repository) {
        return "refs/remotes/origin/" + repository.getSettings().getDefaultBranch();
    }

    @PreDestroy
    private void preDestroy() {
        executor.shutdownNow();
    }

}
