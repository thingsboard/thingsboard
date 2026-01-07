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
package org.thingsboard.server.service.sync.vc;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
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
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
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
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
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
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.jgit.api.ListBranchCommand.ListMode;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NODELETE;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED;

@Slf4j
public class GitRepository {

    private final Git git;
    private final AuthHandler authHandler;
    @Getter
    private final RepositorySettings settings;

    @Getter
    private final String directory;

    private ObjectId headId;

    private GitRepository(Git git, RepositorySettings settings, AuthHandler authHandler, String directory) {
        this.git = git;
        this.settings = settings;
        this.authHandler = authHandler;
        this.directory = directory;
    }

    public static GitRepository create(RepositorySettings settings, File directory) throws GitAPIException {
        log.debug("Executing create [{}]", directory);
        Git git = Git.init()
                .setDirectory(directory)
                .call();
        return new GitRepository(git, settings, null, directory.getAbsolutePath());
    }

    public static GitRepository clone(RepositorySettings settings, File directory) throws GitAPIException {
        log.debug("Executing clone [{}]", settings.getRepositoryUri());
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(settings.getRepositoryUri())
                .setDirectory(directory)
                .setNoCheckout(true);
        AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        authHandler.configureCommand(cloneCommand);
        Git git = cloneCommand.call();
        return new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
    }

    public static GitRepository open(File directory, RepositorySettings settings) throws IOException {
        log.debug("Executing open [{}][{}]", settings.getRepositoryUri(), directory);
        Git git = Git.open(directory);
        AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        return new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
    }

    public static GitRepository openOrClone(Path directory, RepositorySettings settings, boolean fetch) throws IOException, GitAPIException {
        if (GitRepository.exists(directory.toString())) {
            try {
                GitRepository repository = GitRepository.open(directory.toFile(), settings);
                if (fetch) {
                    repository.fetch();
                }
                return repository;
            } catch (RepositoryNotFoundException e) {
                log.warn("{} not a git repository, reinitializing", directory);
            } catch (org.eclipse.jgit.errors.TransportException | org.eclipse.jgit.api.errors.TransportException e) {
                if (StringUtils.containsIgnoreCase(e.getMessage(), "missing commit")) {
                    log.warn("Couldn't fetch {} due to {}, reinitializing", directory, e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        FileUtils.deleteDirectory(directory.toFile());
        Files.createDirectories(directory);
        if (settings.isLocalOnly()) {
            return GitRepository.create(settings, directory.toFile());
        } else {
            return GitRepository.clone(settings, directory.toFile());
        }
    }

    public static void test(RepositorySettings settings, File directory) throws Exception {
        if (settings.isLocalOnly()) {
            return;
        }
        log.debug("Executing test [{}]", settings.getRepositoryUri());
        AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        if (settings.isReadOnly()) {
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository().setRemote(settings.getRepositoryUri());
            authHandler.configureCommand(lsRemoteCommand);
            lsRemoteCommand.call();
        } else {
            Files.createDirectories(directory.toPath());
            try {
                Git git = Git.init().setDirectory(directory).call();
                GitRepository repository = new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
                repository.execute(repository.git.remoteAdd()
                        .setName("origin")
                        .setUri(new URIish(settings.getRepositoryUri())));
                repository.push("", UUID.randomUUID().toString()); // trying to delete non-existing branch on remote repo
            } finally {
                try {
                    FileUtils.forceDelete(directory);
                } catch (Exception ignored) {}
            }
        }
    }

    public boolean fetch() throws GitAPIException {
        if (settings.isLocalOnly()) {
            return false;
        }
        log.debug("Executing fetch [{}]", settings.getRepositoryUri());
        FetchResult result = execute(git.fetch()
                .setRemoveDeletedRefs(true));
        Ref head = result.getAdvertisedRef(Constants.HEAD);
        if (head != null) {
            this.headId = head.getObjectId();
        }
        return CollectionsUtil.isNotEmpty(result.getTrackingRefUpdates());
    }

    public void deleteLocalBranchIfExists(String branch) throws GitAPIException {
        log.debug("Executing deleteLocalBranchIfExists [{}][{}]", settings.getRepositoryUri(), branch);
        execute(git.branchDelete()
                .setBranchNames(branch)
                .setForce(true));
    }

    public void resetAndClean() throws GitAPIException {
        log.debug("Executing resetAndClean [{}]", settings.getRepositoryUri());
        execute(git.reset()
                .setMode(ResetCommand.ResetType.HARD));
        execute(git.clean()
                .setForce(true)
                .setCleanDirectories(true));
    }

    public void merge(String branch) throws IOException, GitAPIException {
        log.debug("Executing merge [{}][{}]", settings.getRepositoryUri(), branch);
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        execute(git.merge()
                .include(branchId));
    }

    public List<BranchInfo> listBranches() throws GitAPIException {
        log.debug("Executing listBranches [{}]", settings.getRepositoryUri());
        return execute(git.branchList()
                .setListMode(settings.isLocalOnly() ? ListMode.ALL : ListMode.REMOTE)).stream()
                .filter(ref -> !ref.getName().equals(Constants.HEAD))
                .map(this::toBranchInfo)
                .distinct().collect(Collectors.toList());
    }

    public PageData<Commit> listCommits(String branch, PageLink pageLink) throws IOException, GitAPIException {
        return listCommits(branch, null, pageLink);
    }

    public PageData<Commit> listCommits(String branch, String path, PageLink pageLink) throws IOException, GitAPIException {
        log.debug("Executing listCommits [{}][{}][{}]", settings.getRepositoryUri(), branch, path);
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            return new PageData<>();
        }
        LogCommand command = git.log()
                .add(branchId);

        command.setRevFilter(new CommitFilter(pageLink.getTextSearch(), settings.isShowMergeCommits()));
        if (StringUtils.isNotEmpty(path)) {
            command.addPath(path);
        }

        Iterable<RevCommit> commits = execute(command);
        return iterableToPageData(commits, this::toCommit, pageLink, revCommitComparatorFunction);
    }

    public List<String> listFilesAtCommit(String commitId, String path) {
        return listFilesAtCommit(commitId, path, -1).stream().map(RepoFile::path).toList();
    }

    @SneakyThrows
    public List<RepoFile> listFilesAtCommit(String commitId, String path, int depth) {
        log.debug("Executing listFilesAtCommit [{}][{}][{}]", settings.getRepositoryUri(), commitId, path);
        List<RepoFile> files = new ArrayList<>();
        RevCommit revCommit = resolveCommit(commitId);
        try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
            treeWalk.reset(revCommit.getTree().getId());
            if (StringUtils.isNotEmpty(path)) {
                treeWalk.setFilter(PathFilter.create(path));
            }
            boolean fixedDepth = depth != -1;
            treeWalk.setRecursive(!fixedDepth);
            while (treeWalk.next()) {
                if (!fixedDepth || treeWalk.getDepth() == depth) {
                    files.add(new RepoFile(treeWalk.getPathString(), treeWalk.getNameString(), treeWalk.isSubtree() ? FileType.DIRECTORY : FileType.FILE));
                }
                if (fixedDepth && treeWalk.getDepth() < depth) {
                    treeWalk.enterSubtree();
                }
            }
        }
        return files;
    }

    @SneakyThrows
    public byte[] getFileContentAtCommit(String file, String commitId) {
        log.debug("Executing getFileContentAtCommit [{}][{}][{}]", settings.getRepositoryUri(), commitId, file);
        RevCommit revCommit = resolveCommit(commitId);
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), file, revCommit.getTree())) {
            if (treeWalk == null) {
                throw new IllegalArgumentException("File not found");
            }
            ObjectId blobId = treeWalk.getObjectId(0);
            try (ObjectReader objectReader = git.getRepository().newObjectReader()) {
                ObjectLoader objectLoader = objectReader.open(blobId);
                try {
                    return objectLoader.getBytes();
                } catch (LargeObjectException e) {
                    throw new RuntimeException("File " + file + " is too big to load");
                }
            }
        }
    }


    public void createAndCheckoutOrphanBranch(String name) throws GitAPIException {
        log.debug("Executing createAndCheckoutOrphanBranch [{}][{}]", settings.getRepositoryUri(), name);
        execute(git.checkout()
                .setOrphan(true)
                .setForced(true)
                .setName(name));
    }

    public void checkoutBranch(String name) throws GitAPIException {
        log.debug("Executing checkoutBranch [{}][{}]", settings.getRepositoryUri(), name);
        git.checkout()
                .setForced(true)
                .setName(name)
                .call();
    }

    public void add(String filesPattern) throws GitAPIException {
        log.debug("Executing add [{}][{}]", settings.getRepositoryUri(), filesPattern);
        execute(git.add().setUpdate(true).addFilepattern(filesPattern));
        execute(git.add().addFilepattern(filesPattern));
    }

    public Status status() throws GitAPIException {
        log.debug("Executing status [{}]", settings.getRepositoryUri());
        org.eclipse.jgit.api.Status status = execute(git.status());
        Set<String> modified = new HashSet<>();
        modified.addAll(status.getModified());
        modified.addAll(status.getChanged());
        return new Status(status.getAdded(), modified, status.getRemoved());
    }

    public Commit commit(String message, String authorName, String authorEmail) throws GitAPIException {
        log.debug("Executing commit [{}][{}]", settings.getRepositoryUri(), message);
        RevCommit revCommit = execute(git.commit()
                .setAuthor(authorName, authorEmail)
                .setMessage(message));
        return toCommit(revCommit);
    }


    public void push(String localBranch, String remoteBranch) throws GitAPIException {
        if (settings.isLocalOnly()) {
            return;
        }
        log.debug("Executing push [{}][{}]", settings.getRepositoryUri(), remoteBranch);
        Iterable<PushResult> result = execute(git.push()
                .setRefSpecs(new RefSpec(localBranch + ":" + remoteBranch)));
        result.forEach(pushResult -> {
            for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
                RemoteRefUpdate.Status status = update.getStatus();
                if (status == REJECTED_NONFASTFORWARD || status == REJECTED_NODELETE ||
                        status == REJECTED_REMOTE_CHANGED || status == REJECTED_OTHER_REASON) {
                    throw new RuntimeException("Remote repository answered with error: " +
                            Optional.ofNullable(update.getMessage()).orElseGet(status::name));
                }
            }
        });
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
                            diff.setFileContentAtCommit1(new String(getFileContentAtCommit(diff.getFilePath(), commit1), StandardCharsets.UTF_8));
                        } catch (IllegalArgumentException ignored) {
                        }
                        try {
                            diff.setFileContentAtCommit2(new String(getFileContentAtCommit(diff.getFilePath(), commit2), StandardCharsets.UTF_8));
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
        if (settings.isLocalOnly()) {
            rev = StringUtils.removeStart(rev, "origin/");
        }
        ObjectId result = git.getRepository().resolve(rev);
        if (result == null) {
            throw new IllegalArgumentException("Failed to resolve '" + rev + "'");
        }
        return result;
    }

    @SneakyThrows
    public static boolean exists(String directory) {
        File gitDirectory = Path.of(directory, ".git").toFile();
        return FileUtils.isDirectory(gitDirectory) && !FileUtils.isEmptyDirectory(gitDirectory);
    }

    private <C extends GitCommand<T>, T> T execute(C command) throws GitAPIException {
        if (command instanceof TransportCommand transportCommand && authHandler != null) {
            authHandler.configureCommand(transportCommand);
        }
        return command.call();
    }

    private static final Function<PageLink, Comparator<RevCommit>> revCommitComparatorFunction = pageLink -> {
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

    @RequiredArgsConstructor
    private static class AuthHandler {
        private final CredentialsProvider credentialsProvider;
        private final SshdSessionFactory sshSessionFactory;

        protected static AuthHandler createFor(RepositorySettings settings, File directory) {
            if (settings.isLocalOnly()) {
                return null;
            }
            CredentialsProvider credentialsProvider = null;
            SshdSessionFactory sshSessionFactory = null;
            if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
                credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
            } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
                if (StringUtils.startsWith(settings.getRepositoryUri(), "https://")) {
                    throw new IllegalArgumentException("Invalid URI format for private key authentication");
                }
                sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
            }
            return new AuthHandler(credentialsProvider, sshSessionFactory);
        }

        protected void configureCommand(TransportCommand command) {
            if (credentialsProvider != null) {
                command.setCredentialsProvider(credentialsProvider);
            }
            if (sshSessionFactory != null) {
                command.setTransportConfigCallback(transport -> {
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
    }

    private static class CommitFilter extends RevFilter {

        private final String textSearch;
        private final boolean showMergeCommits;

        CommitFilter(String textSearch, boolean showMergeCommits) {
            this.textSearch = textSearch.toLowerCase();
            this.showMergeCommits = showMergeCommits;
        }

        @Override
        public boolean include(RevWalk walker, RevCommit c) {
            return (showMergeCommits || c.getParentCount() < 2) && (StringUtils.isEmpty(textSearch)
                    || c.getFullMessage().toLowerCase().contains(textSearch));
        }

        @Override
        public RevFilter clone() {
            return this;
        }

        @Override
        public boolean requiresCommitBody() {
            return false;
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

    public record RepoFile(String path, String name, FileType type) {}

    public enum FileType {
        FILE, DIRECTORY
    }

}
