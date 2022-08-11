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
import com.google.common.collect.Ordering;
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
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GitRepository {

    private final Git git;
    @Getter
    private final RepositorySettings settings;
    private final CredentialsProvider credentialsProvider;
    private final SshdSessionFactory sshSessionFactory;

    @Getter
    private final String directory;

    private ObjectId headId;

    private GitRepository(Git git, RepositorySettings settings, CredentialsProvider credentialsProvider, SshdSessionFactory sshSessionFactory, String directory) {
        this.git = git;
        this.settings = settings;
        this.credentialsProvider = credentialsProvider;
        this.sshSessionFactory = sshSessionFactory;
        this.directory = directory;
    }

    public static GitRepository clone(RepositorySettings settings, File directory) throws GitAPIException {
        CredentialsProvider credentialsProvider = null;
        SshdSessionFactory sshSessionFactory = null;
        if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
            credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
        } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
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

    public static GitRepository open(File directory, RepositorySettings settings) throws IOException {
        Git git = Git.open(directory);
        CredentialsProvider credentialsProvider = null;
        SshdSessionFactory sshSessionFactory = null;
        if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
            credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
        } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
            sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
        }
        return new GitRepository(git, settings, credentialsProvider, sshSessionFactory, directory.getAbsolutePath());
    }

    public static void test(RepositorySettings settings, File directory) throws GitAPIException {
        CredentialsProvider credentialsProvider = null;
        SshdSessionFactory sshSessionFactory = null;
        if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
            credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
        } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
            sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
        }
        LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository().setRemote(settings.getRepositoryUri());
        configureTransportCommand(lsRemoteCommand, credentialsProvider, sshSessionFactory);
        lsRemoteCommand.call();
    }

    public void fetch() throws GitAPIException {
        FetchResult result = execute(git.fetch()
                .setRemoveDeletedRefs(true));
        Ref head = result.getAdvertisedRef(Constants.HEAD);
        if (head != null) {
            this.headId = head.getObjectId();
        }
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

    public List<BranchInfo> listRemoteBranches() throws GitAPIException {
        return execute(git.branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)).stream()
                .filter(ref -> !ref.getName().equals(Constants.HEAD))
                .map(this::toBranchInfo)
                .distinct().collect(Collectors.toList());
    }

    public PageData<Commit> listCommits(String branch, PageLink pageLink) throws IOException, GitAPIException {
        return listCommits(branch, null, pageLink);
    }

    public PageData<Commit> listCommits(String branch, String path, PageLink pageLink) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            return new PageData<>();
        }
        LogCommand command = git.log()
                .add(branchId);

        if (StringUtils.isNotEmpty(pageLink.getTextSearch())) {
            command.setRevFilter(new NoMergesAndCommitMessageFilter(pageLink.getTextSearch()));
        } else {
            command.setRevFilter(RevFilter.NO_MERGES);
        }

        if (StringUtils.isNotEmpty(path)) {
            command.addPath(path);
        }

        Iterable<RevCommit> commits = execute(command);
        return iterableToPageData(commits, this::toCommit, pageLink, revCommitComparatorFunction);
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
                throw new IllegalArgumentException("File not found");
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
        Set<String> modified = new HashSet<>();
        modified.addAll(status.getModified());
        modified.addAll(status.getChanged());
        return new Status(status.getAdded(), modified, status.getRemoved());
    }

    public Commit commit(String message, String authorName, String authorEmail) throws GitAPIException {
        RevCommit revCommit = execute(git.commit()
                .setAuthor(authorName, authorEmail)
                .setMessage(message));
        return toCommit(revCommit);
    }


    public void push(String localBranch, String remoteBranch) throws GitAPIException {
        execute(git.push()
                .setRefSpecs(new RefSpec(localBranch + ":" + remoteBranch)));
    }

    public String getContentsDiff(String content1, String content2) throws IOException {
        RawText rawContent1 = new RawText(content1.getBytes());
        RawText rawContent2 = new RawText(content2.getBytes());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(git.getRepository());

        EditList edits = new EditList();
        edits.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, rawContent1, rawContent2));
        diffFormatter.format(edits, rawContent1, rawContent2);
        return out.toString();
    }

    public List<Diff> getDiffList(String commit1, String commit2, String path) throws IOException {
        ObjectReader reader = git.getRepository().newObjectReader();

        CanonicalTreeParser tree1Iter = new CanonicalTreeParser();
        ObjectId tree1 = resolveCommit(commit1).getTree();
        tree1Iter.reset(reader, tree1);

        CanonicalTreeParser tree2Iter = new CanonicalTreeParser();
        ObjectId tree2 = resolveCommit(commit2).getTree();
        tree2Iter.reset(reader, tree2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(git.getRepository());
        if (StringUtils.isNotEmpty(path)) {
            diffFormatter.setPathFilter(PathFilter.create(path));
        }

        return diffFormatter.scan(tree1, tree2).stream()
                .map(diffEntry -> {
                    Diff diff = new Diff();
                    try {
                        out.reset();
                        diffFormatter.format(diffEntry);
                        diff.setDiffStringValue(out.toString());
                        diff.setFilePath(diffEntry.getChangeType() != DiffEntry.ChangeType.DELETE ? diffEntry.getNewPath() : diffEntry.getOldPath());
                        diff.setChangeType(diffEntry.getChangeType());
                        try {
                            diff.setFileContentAtCommit1(getFileContentAtCommit(diff.getFilePath(), commit1));
                        } catch (IllegalArgumentException ignored) {
                        }
                        try {
                            diff.setFileContentAtCommit2(getFileContentAtCommit(diff.getFilePath(), commit2));
                        } catch (IllegalArgumentException ignored) {
                        }
                        return diff;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private BranchInfo toBranchInfo(Ref ref) {
        String name = org.eclipse.jgit.lib.Repository.shortenRefName(ref.getName());
        String branchName = StringUtils.removeStart(name, "origin/");
        boolean isDefault = this.headId != null && this.headId.equals(ref.getObjectId());
        return new BranchInfo(branchName, isDefault);
    }

    private Commit toCommit(RevCommit revCommit) {
        return new Commit(revCommit.getCommitTime() * 1000l, revCommit.getName(),
                revCommit.getFullMessage(), revCommit.getAuthorIdent().getName(), revCommit.getAuthorIdent().getEmailAddress());
    }

    private RevCommit resolveCommit(String id) throws IOException {
        return git.getRepository().parseCommit(resolve(id));
    }

    private ObjectId resolve(String rev) throws IOException {
        ObjectId result = git.getRepository().resolve(rev);
        if (result == null) {
            throw new IllegalArgumentException("Failed to parse git revision string: \"" + rev + "\"");
        }
        return result;
    }

    private <C extends GitCommand<T>, T> T execute(C command) throws GitAPIException {
        if (command instanceof TransportCommand) {
            configureTransportCommand((TransportCommand) command, credentialsProvider, sshSessionFactory);
        }
        return command.call();
    }

    private static Function<PageLink, Comparator<RevCommit>> revCommitComparatorFunction = pageLink -> {
        SortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder != null
                && sortOrder.getProperty().equals("timestamp")
                && SortOrder.Direction.ASC.equals(sortOrder.getDirection())) {
            return Comparator.comparingInt(RevCommit::getCommitTime);
        }
        return null;
    };

    private static <T, R> PageData<R> iterableToPageData(Iterable<T> iterable,
                                                         Function<? super T, ? extends R> mapper,
                                                         PageLink pageLink,
                                                         Function<PageLink, Comparator<T>> comparatorFunction) {
        iterable = Streams.stream(iterable).collect(Collectors.toList());
        int totalElements = Iterables.size(iterable);
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        int limit = startIndex + pageLink.getPageSize();
        if (comparatorFunction != null) {
            Comparator<T> comparator = comparatorFunction.apply(pageLink);
            if (comparator != null) {
                iterable = Ordering.from(comparator).immutableSortedCopy(iterable);
            }
        }
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

    private static class NoMergesAndCommitMessageFilter extends RevFilter {

        private final String textSearch;

        NoMergesAndCommitMessageFilter(String textSearch) {
            this.textSearch = textSearch.toLowerCase();
        }

        @Override
        public boolean include(RevWalk walker, RevCommit c) {
            return c.getParentCount() < 2 && c.getFullMessage().toLowerCase().contains(this.textSearch);
        }

        @Override
        public RevFilter clone() {
            return this;
        }

        @Override
        public boolean requiresCommitBody() {
            return false;
        }

        @Override
        public String toString() {
            return "NO_MERGES_AND_COMMIT_MESSAGE";
        }
    }

    @Data
    public static class Commit {
        private final long timestamp;
        private final String id;
        private final String message;
        private final String authorName;
        private final String authorEmail;
    }

    @Data
    public static class Status {
        private final Set<String> added;
        private final Set<String> modified;
        private final Set<String> removed;
    }

    @Data
    public static class Diff {
        private String filePath;
        private DiffEntry.ChangeType changeType;
        private String fileContentAtCommit1;
        private String fileContentAtCommit2;
        private String diffStringValue;
    }

}
