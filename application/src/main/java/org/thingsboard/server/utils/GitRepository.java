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
package org.thingsboard.server.utils;

import com.google.common.collect.Streams;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class GitRepository {

    private final Git git;
    private final CredentialsProvider credentialsProvider;

    @Getter
    private final String directory;
    @Getter
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private GitRepository(Git git, CredentialsProvider credentialsProvider, String directory) {
        this.git = git;
        this.credentialsProvider = credentialsProvider;
        this.directory = directory;
    }

    public static GitRepository clone(String uri, String username, String password, File directory) throws GitAPIException {
        CredentialsProvider credentialsProvider = newCredentialsProvider(username, password);
        Git git = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(directory)
                .setNoCheckout(true)
                .setCredentialsProvider(credentialsProvider)
                .call();
        return new GitRepository(git, credentialsProvider, directory.getAbsolutePath());
    }

    public static GitRepository open(File directory, String username, String password) throws IOException {
        Git git = Git.open(directory);
        return new GitRepository(git, newCredentialsProvider(username, password), directory.getAbsolutePath());
    }


    public void fetch() throws GitAPIException {
        execute(git.fetch()
                .setRemoveDeletedRefs(true));
    }

    public void checkout(String branch) throws GitAPIException {
        execute(git.checkout()
                .setName(branch));
    }

    public void merge(String branch) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        execute(git.merge()
                .include(branchId));
    }


    public List<String> listBranches() throws GitAPIException {
        return execute(git.branchList()
                .setListMode(ListBranchCommand.ListMode.ALL)).stream()
                .filter(ref -> !ref.getName().equals(Constants.HEAD))
                .map(ref -> org.eclipse.jgit.lib.Repository.shortenRefName(ref.getName()))
                .map(name -> StringUtils.removeStart(name, "origin/"))
                .distinct().collect(Collectors.toList());
    }


    public List<Commit> listCommits(String branch, int limit) throws IOException, GitAPIException {
        return listCommits(branch, null, limit);
    }

    public List<Commit> listCommits(String branch, String path, int limit) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        LogCommand command = git.log()
                .add(branchId).setMaxCount(limit)
                .setRevFilter(RevFilter.NO_MERGES);
        if (StringUtils.isNotEmpty(path)) {
            command.addPath(path);
        }
        return Streams.stream(execute(command))
                .map(this::toCommit)
                .collect(Collectors.toList());
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
                .setName(name));
        Set<String> uncommittedChanges = git.status().call().getUncommittedChanges();
        if (!uncommittedChanges.isEmpty()) {
            RmCommand rm = git.rm();
            uncommittedChanges.forEach(rm::addFilepattern);
            execute(rm);
        }
        execute(git.clean());
    }

    public void add(String filesPattern) throws GitAPIException { // FIXME [viacheslav]
        execute(git.add().setUpdate(true).addFilepattern(filesPattern));
        execute(git.add().addFilepattern(filesPattern));
    }

    public Status status() throws GitAPIException {
        org.eclipse.jgit.api.Status status = execute(git.status());
        return new Status(status.getAdded(), status.getModified(), status.getRemoved());
    }

    public Commit commit(String message) throws GitAPIException {
        RevCommit revCommit = execute(git.commit()
                .setMessage(message)); // TODO [viacheslav]: set configurable author for commit
        return toCommit(revCommit);
    }


    public void push() throws GitAPIException {
        execute(git.push());
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
        if (command instanceof TransportCommand && credentialsProvider != null) {
            ((TransportCommand<?, ?>) command).setCredentialsProvider(credentialsProvider);
        }
        return command.call();
    }

    private static CredentialsProvider newCredentialsProvider(String username, String password) {
        return new UsernamePasswordCredentialsProvider(username, password);
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
