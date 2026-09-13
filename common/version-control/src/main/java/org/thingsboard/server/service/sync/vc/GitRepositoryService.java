// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.GitRepository.Diff;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface GitRepositoryService {

    Set<TenantId> getActiveRepositoryTenants();

    void prepareCommit(PendingCommit pendingCommit);

    PageData<EntityVersion> listVersions(TenantId tenantId, String branch, String path, PageLink pageLink) throws Exception;

    List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String versionId, String path) throws Exception;

    void testRepository(TenantId tenantId, RepositorySettings settings) throws Exception;

    void initRepository(TenantId tenantId, RepositorySettings settings, boolean fetch) throws Exception;

    RepositorySettings getRepositorySettings(TenantId tenantId) throws Exception;

    void clearRepository(TenantId tenantId) throws IOException;

    void add(PendingCommit commit, String relativePath, String entityDataJson) throws IOException;

    void deleteFolderContent(PendingCommit commit, String relativePath) throws IOException;

    VersionCreationResult push(PendingCommit commit);

    void cleanUp(PendingCommit commit);

    void abort(PendingCommit commit);

    List<BranchInfo> listBranches(TenantId tenantId);

    String getFileContentAtCommit(TenantId tenantId, String relativePath, String versionId) throws IOException;

    List<Diff> getVersionsDiffList(TenantId tenantId, String path, String versionId1, String versionId2) throws IOException;

    String getContentsDiff(TenantId tenantId, String content1, String content2) throws IOException;

    void fetch(TenantId tenantId) throws GitAPIException;

}
