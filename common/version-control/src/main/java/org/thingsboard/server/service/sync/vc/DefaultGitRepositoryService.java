/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.vc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@ConditionalOnProperty(prefix = "vc", value = "git.service", havingValue = "local", matchIfMissing = true)
@Service
public class DefaultGitRepositoryService implements GitRepositoryService {

    @Value("${java.io.tmpdir}/repositories")
    private String defaultFolder;

    @Value("${vc.git.folder:${java.io.tmpdir}/repositories}")
    private String repositoriesFolder;

    @Value("${vc.git.repos-poll-interval:60}")
    private long reposPollInterval;

    private ScheduledExecutorService scheduler;
    private final Map<TenantId, GitRepository> repositories = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(repositoriesFolder)) {
            repositoriesFolder = defaultFolder;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            repositories.forEach((tenantId, repository) -> {
                try {
                    repository.fetch();
                    log.info("Fetching remote repository for tenant {}", tenantId);
                } catch (Exception e) {
                    log.warn("Failed to fetch repository for tenant {}", tenantId, e);
                }
            });
        }, reposPollInterval, reposPollInterval, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void prepareCommit(PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        String branch = commit.getBranch();
        try {
            repository.fetch();
            if (repository.listBranches().contains(branch)) {
                repository.checkout("origin/" + branch, false);
                try {
                    repository.checkout(branch, true);
                } catch (RefAlreadyExistsException e) {
                    repository.checkout(branch, false);
                }
                repository.merge(branch);
            } else { // TODO [viacheslav]: rollback orphan branch on failure
                try {
                    repository.createAndCheckoutOrphanBranch(branch); // FIXME [viacheslav]: Checkout returned unexpected result NO_CHANGE for master branch
                } catch (JGitInternalException e) {
                    if (!e.getMessage().contains("NO_CHANGE")) {
                        throw e;
                    }
                }
            }
        } catch (IOException | GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    @Override
    public void deleteFolderContent(PendingCommit commit, String relativePath) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        FileUtils.deleteDirectory(Path.of(repository.getDirectory(), relativePath).toFile());
    }

    @Override
    public void add(PendingCommit commit, String relativePath, String entityDataJson) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        FileUtils.write(Path.of(repository.getDirectory(), relativePath).toFile(), entityDataJson, StandardCharsets.UTF_8);
    }

    @Override
    public VersionCreationResult push(PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        try {
            repository.add(".");

            VersionCreationResult result = new VersionCreationResult();
            GitRepository.Status status = repository.status();
            result.setAdded(status.getAdded().size());
            result.setModified(status.getModified().size());
            result.setRemoved(status.getRemoved().size());

            GitRepository.Commit gitCommit = repository.commit(commit.getVersionName());
            repository.push();

            result.setVersion(toVersion(gitCommit));
            return result;
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    @Override
    public void abort(PendingCommit commit) {
        //TODO: implement;
    }

    @Override
    public String getFileContentAtCommit(TenantId tenantId, String relativePath, String versionId) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getFileContentAtCommit(relativePath, versionId);
    }

    @Override
    public List<String> listBranches(TenantId tenantId) {
        GitRepository repository = checkRepository(tenantId);
        try {
            return repository.listBranches();
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    private EntityVersion checkVersion(TenantId tenantId, String branch, String versionId) throws Exception {
        return listVersions(tenantId, branch, null).stream()
                .filter(version -> version.getId().equals(versionId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Version not found"));
    }

    private GitRepository checkRepository(TenantId tenantId) {
        return Optional.ofNullable(repositories.get(tenantId))
                .orElseThrow(() -> new IllegalStateException("Repository is not initialized"));
    }

    @Override
    public List<EntityVersion> listVersions(TenantId tenantId, String branch, String path) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listCommits(branch, path, Integer.MAX_VALUE).stream()
                .map(this::toVersion)
                .collect(Collectors.toList());
    }

    @Override
    public List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId, String path) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        checkVersion(tenantId, branch, versionId);
        return repository.listFilesAtCommit(versionId, path).stream()
                .map(filePath -> {
                    EntityId entityId = fromRelativePath(filePath);
                    VersionedEntityInfo info = new VersionedEntityInfo();
                    info.setExternalId(entityId);
                    return info;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void testRepository(TenantId tenantId, EntitiesVersionControlSettings settings) throws Exception {
        Path repositoryDirectory = Path.of(repositoriesFolder, tenantId.getId().toString());
        GitRepository.test(settings, repositoryDirectory.toFile());
    }

    @Override
    public void initRepository(TenantId tenantId, EntitiesVersionControlSettings settings) throws Exception {
        clearRepository(tenantId);
        Path repositoryDirectory = Path.of(repositoriesFolder, tenantId.getId().toString());
        GitRepository repository;
        if (Files.exists(repositoryDirectory)) {
            FileUtils.forceDelete(repositoryDirectory.toFile());
        }

        Files.createDirectories(repositoryDirectory);
        repository = GitRepository.clone(settings, repositoryDirectory.toFile());
        repositories.put(tenantId, repository);
    }

    @Override
    public EntitiesVersionControlSettings getRepositorySettings(TenantId tenantId) throws Exception {
        var gitRepository = repositories.get(tenantId);
        return gitRepository != null ? gitRepository.getSettings() : null;
    }

    @Override
    public void clearRepository(TenantId tenantId) throws IOException {
        GitRepository repository = repositories.get(tenantId);
        if (repository != null) {
            FileUtils.deleteDirectory(new File(repository.getDirectory()));
            repositories.remove(tenantId);
        }
    }

    private EntityVersion toVersion(GitRepository.Commit commit) {
        return new EntityVersion(commit.getId(), commit.getMessage());
    }

    private EntityId fromRelativePath(String path) {
        EntityType entityType = EntityType.valueOf(StringUtils.substringBefore(path, "/").toUpperCase());
        String entityId = StringUtils.substringBetween(path, "/", ".json");
        return EntityIdFactory.getByTypeAndUuid(entityType, entityId);
    }
}
