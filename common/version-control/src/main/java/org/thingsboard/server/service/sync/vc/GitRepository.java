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

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.VersionControlAuthMethod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GitRepository {

    private final Git git;
    @Getter
    private final EntitiesVersionControlSettings settings;
    private final CredentialsProvider credentialsProvider;
    private final SshdSessionFactory sshSessionFactory;

    @Getter
    private final String directory;

    private GitRepository(Git git, EntitiesVersionControlSettings settings, CredentialsProvider credentialsProvider, SshdSessionFactory sshSessionFactory, String directory) {
        this.git = git;
        this.settings = settings;
        this.credentialsProvider = credentialsProvider;
        this.sshSessionFactory = sshSessionFactory;
        this.directory = directory;
    }

    public static GitRepository clone(EntitiesVersionControlSettings settings, File directory) throws GitAPIException {
        CredentialsProvider credentialsProvider = null;
        SshdSessionFactory sshSessionFactory = null;
        if (VersionControlAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
            credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
        } else if (VersionControlAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
            sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
        }
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(settings.getRepositoryUri())
                .setDirectory(directory)
                .setNoCheckout(true);
        configureTransportCommand(cloneCommand, credentialsProvider, sshSessionFactory);
        Git git = cloneCommand.call();
        return new GitRepository(git, settings, credentialsProvider, sshSessionFactory, directory.getAbsolutePath());
    }

    public static GitRepository open(File directory, EntitiesVersionControlSettings settings) throws IOException {
        Git git = Git.open(directory);
        CredentialsProvider credentialsProvider = null;
        SshdSessionFactory sshSessionFactory = null;
        if (VersionControlAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
            credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
        } else if (VersionControlAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
            sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
        }
        return new GitRepository(git, settings, credentialsProvider, sshSessionFactory, directory.getAbsolutePath());
    }

    public static void test(EntitiesVersionControlSettings settings, File directory) throws GitAPIException {
        CredentialsProvider credentialsProvider = null;
        SshdSessionFactory sshSessionFactory = null;
        if (VersionControlAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
            credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
        } else if (VersionControlAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
            sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
        }
        LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository().setRemote(settings.getRepositoryUri());
        configureTransportCommand(lsRemoteCommand, credentialsProvider, sshSessionFactory);
        lsRemoteCommand.call();
    }

    public void fetch() throws GitAPIException {
        execute(git.fetch()
                .setRemoveDeletedRefs(true));
    }

    public void deleteLocalBranchIfExists(String branch) throws GitAPIException {
        execute(git.branchDelete()
                .setBranchNames(branch)
                .setForce(true));
    }

    public void resetAndClean() throws GitAPIException {
        execute(git.reset()
                .setMode(ResetCommand.ResetType.HARD));
        execute(git.clean()
                .setForce(true)
                .setCleanDirectories(true));
    }

    public void merge(String branch) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        execute(git.merge()
                .include(branchId));
    }


    public List<String> listRemoteBranches() throws GitAPIException {
        return execute(git.branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)).stream()
                .filter(ref -> !ref.getName().equals(Constants.HEAD))
                .map(ref -> org.eclipse.jgit.lib.Repository.shortenRefName(ref.getName()))
                .map(name -> StringUtils.removeStart(name, "origin/"))
                .distinct().collect(Collectors.toList());
    }

    public PageData<Commit> listCommits(String branch, PageLink pageLink) throws IOException, GitAPIException {
        return listCommits(branch, null, pageLink);
    }

    public PageData<Commit> listCommits(String branch, String path, PageLink pageLink) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        LogCommand command = git.log()
                .add(branchId)
                .setRevFilter(RevFilter.NO_MERGES);
        if (StringUtils.isNotEmpty(path)) {
            command.addPath(path);
        }
        Iterable<RevCommit> commits = execute(command);
        return iterableToPageData(commits, this::toCommit, pageLink);
    }

    public List<String> listFilesAtCommit(String commitId) throws IOException {
        return listFilesAtCommit(commitId, null);
    }

    public List<String> listFilesAtCommit(String commitId, String path) throws IOException {
        List<String> files = new ArrayList<>();
        RevCommit revCommit = resolveCommit(commitId);
        try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
            treeWalk.reset(revCommit.getTree().getId());
            if (StringUtils.isNotEmpty(path)) {
                treeWalk.setFilter(PathFilter.create(path));
            }
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                files.add(treeWalk.getPathString());
            }
        }
        return files;
    }


    public String getFileContentAtCommit(String file, String commitId) throws IOException {
        RevCommit revCommit = resolveCommit(commitId);
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), file, revCommit.getTree())) {
            if (treeWalk == null) {
                throw new IllegalArgumentException("Not found");
            }
            ObjectId blobId = treeWalk.getObjectId(0);
            try (ObjectReader objectReader = git.getRepository().newObjectReader()) {
                ObjectLoader objectLoader = objectReader.open(blobId);
                byte[] bytes = objectLoader.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }


    public void createAndCheckoutOrphanBranch(String name) throws GitAPIException {
        execute(git.checkout()
                .setOrphan(true)
                .setForced(true)
                .setName(name));
    }

    public void add(String filesPattern) throws GitAPIException {
        execute(git.add().setUpdate(true).addFilepattern(filesPattern));
        execute(git.add().addFilepattern(filesPattern));
    }

    public Status status() throws GitAPIException {
        org.eclipse.jgit.api.Status status = execute(git.status());
        return new Status(status.getAdded(), status.getModified(), status.getRemoved());
    }

    public Commit commit(String message) throws GitAPIException {
        RevCommit revCommit = execute(git.commit()
                .setMessage(message));
        return toCommit(revCommit);
    }


    public void push(String localBranch, String remoteBranch) throws GitAPIException {
        execute(git.push()
                .setRefSpecs(new RefSpec(localBranch + ":" + remoteBranch)));
    }


//    public List<Diff> getCommitChanges(Commit commit) throws IOException, GitAPIException {
//        RevCommit revCommit = resolveCommit(commit.getId());
//        if (revCommit.getParentCount() == 0) {
//            return null; // just takes the first parent of a commit, but should find a parent in branch provided
//        }
//        return execute(git.diff()
//                .setOldTree(prepareTreeParser(git.getRepository().parseCommit(revCommit.getParent(0))))
//                .setNewTree(prepareTreeParser(revCommit))).stream()
//                .map(diffEntry -> new Diff(diffEntry.getChangeType().name(), diffEntry.getOldPath(), diffEntry.getNewPath()))
//                .collect(Collectors.toList());
//    }
//
//
//    private AbstractTreeIterator prepareTreeParser(RevCommit revCommit) throws IOException {
//        // from the commit we can build the tree which allows us to construct the TreeParser
//        //noinspection Duplicates
//        org.eclipse.jgit.lib.Repository repository = git.getRepository();
//        try (RevWalk walk = new RevWalk(repository)) {
//            RevTree tree = walk.parseTree(revCommit.getTree().getId());
//
//            CanonicalTreeParser treeParser = new CanonicalTreeParser();
//            try (ObjectReader reader = repository.newObjectReader()) {
//                treeParser.reset(reader, tree.getId());
//            }
//
//            walk.dispose();
//
//            return treeParser;
//        }
//    }

    private Commit toCommit(RevCommit revCommit) {
        return new Commit(revCommit.getName(), revCommit.getFullMessage(), revCommit.getAuthorIdent().getName());
    }

    private RevCommit resolveCommit(String id) throws IOException {
        return git.getRepository().parseCommit(resolve(id));
    }

    private ObjectId resolve(String rev) throws IOException {
        return git.getRepository().resolve(rev);
    }

    private <C extends GitCommand<T>, T> T execute(C command) throws GitAPIException {
        if (command instanceof TransportCommand) {
            configureTransportCommand((TransportCommand) command, credentialsProvider, sshSessionFactory);
        }
        return command.call();
    }

    private static <T,R> PageData<R> iterableToPageData (Iterable<T> iterable, Function<? super T, ? extends R> mapper, PageLink pageLink) {
        int totalElements = Iterables.size(iterable);
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        int limit = startIndex + pageLink.getPageSize();
        iterable = Iterables.limit(iterable, limit);
        if (startIndex < totalElements) {
            iterable = Iterables.skip(iterable, startIndex);
        } else {
            iterable = Collections.emptyList();
        }
        List<R> data = Streams.stream(iterable).map(mapper)
                .collect(Collectors.toList());
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + data.size();
        return new PageData<>(data, totalPages, totalElements, hasNext);
    }

    private static void configureTransportCommand(TransportCommand transportCommand, CredentialsProvider credentialsProvider, SshdSessionFactory sshSessionFactory) {
        if (credentialsProvider != null) {
            transportCommand.setCredentialsProvider(credentialsProvider);
        }
        if (sshSessionFactory != null) {
            transportCommand.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport) {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        }
    }

    private static CredentialsProvider newCredentialsProvider(String username, String password) {
        return new UsernamePasswordCredentialsProvider(username, password == null ? "" : password);
    }

    private static SshdSessionFactory newSshdSessionFactory(String privateKey, String password, File directory) {
        SshdSessionFactory sshSessionFactory = null;
        if (StringUtils.isNotBlank(privateKey)) {
            Iterable<KeyPair> keyPairs = loadKeyPairs(privateKey, password);
            sshSessionFactory = new SshdSessionFactoryBuilder()
                    .setPreferredAuthentications("publickey")
                    .setDefaultKeysProvider(file -> keyPairs)
                    .setHomeDirectory(directory)
                    .setSshDirectory(directory)
                    .setServerKeyDatabase((file, file2) -> new ServerKeyDatabase() {
                        @Override
                        public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress, Configuration config) {
                            return Collections.emptyList();
                        }

                        @Override
                        public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey, Configuration config, CredentialsProvider provider) {
                            return true;
                        }
                    })
                    .build(new JGitKeyCache());
        }
        return sshSessionFactory;
    }

    private static Iterable<KeyPair> loadKeyPairs(String privateKeyContent, String password) {
        Iterable<KeyPair> keyPairs = null;
        try {
            keyPairs = SecurityUtils.loadKeyPairIdentities(null,
                    null, new ByteArrayInputStream(privateKeyContent.getBytes()), (session, resourceKey, retryIndex) -> password);
        } catch (Exception e) {}
        if (keyPairs == null) {
            throw new IllegalArgumentException("Failed to load ssh private key");
        }
        return keyPairs;
    }

    @Data
    public static class Commit {
        private final String id;
        private final String message;
        private final String authorName;
    }

    @Data
    public static class Status {
        private final Set<String> added;
        private final Set<String> modified;
        private final Set<String> removed;
    }

}
