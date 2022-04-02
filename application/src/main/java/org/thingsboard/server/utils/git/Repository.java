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
package org.thingsboard.server.utils.git;

import com.google.common.collect.Streams;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.RmCommand;
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
import org.thingsboard.server.utils.git.data.Commit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Repository {

    private final Git git;
    private final CredentialsProvider credentialsProvider;

    @Getter
    private final String directory;

    private Repository(Git git, CredentialsProvider credentialsProvider, String directory) {
        this.git = git;
        this.credentialsProvider = credentialsProvider;
        this.directory = directory;
    }

    public static Repository clone(String uri, String directory,
                                   String username, String password) throws GitAPIException {
        CredentialsProvider credentialsProvider = newCredentialsProvider(username, password);
        Git git = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(new java.io.File(directory))
                .setNoCheckout(true)
                .setCredentialsProvider(credentialsProvider)
                .call();
        return new Repository(git, credentialsProvider, directory);
    }

    public static Repository open(String directory, String username, String password) throws IOException {
        Git git = Git.open(new java.io.File(directory));
        return new Repository(git, newCredentialsProvider(username, password), directory);
    }


    public void fetch() throws GitAPIException {
        execute(git.fetch()
                .setRemoveDeletedRefs(true));
    }


    public List<String> listBranches() throws GitAPIException {
        return execute(git.branchList()
                .setListMode(ListBranchCommand.ListMode.ALL)).stream()
                .filter(ref -> !ref.getName().equals(Constants.HEAD))
                .map(ref -> org.eclipse.jgit.lib.Repository.shortenRefName(ref.getName()))
                .map(name -> StringUtils.removeStart(name, "origin/"))
                .distinct().collect(Collectors.toList());
    }


    public List<Commit> listCommits(String branchName, int limit) throws IOException, GitAPIException {
        return listCommits(branchName, null, limit);
    }

    public List<Commit> listCommits(String branchName, String path, int limit) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branchName);
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


    public List<String> listFilesAtCommit(Commit commit) throws IOException {
        return listFilesAtCommit(commit, null);
    }

    public List<String> listFilesAtCommit(Commit commit, String path) throws IOException {
        List<String> files = new ArrayList<>();
        RevCommit revCommit = resolveCommit(commit.getId());
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


    public void checkout(String branchName) throws GitAPIException {
        execute(git.checkout()
                .setName(branchName));
    }

    public void merge(String branchName) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branchName);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        execute(git.merge()
                .include(branchId));
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

    public void clean() throws GitAPIException {
        execute(git.clean().setCleanDirectories(true));
    }

    public Commit commit(String message, String filePattern, String author) throws GitAPIException {
        execute(git.add().addFilepattern(filePattern));
        RevCommit revCommit = execute(git.commit()
                .setMessage(message)
                .setAuthor(author, author));
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
        if (command instanceof TransportCommand) {
            ((TransportCommand<?, ?>) command).setCredentialsProvider(credentialsProvider);
//        SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance();
//        transportCommand.setTransportConfigCallback(transport -> {
//            ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);
//        });
        }
        return command.call();
    }

    private static CredentialsProvider newCredentialsProvider(String username, String password) {
        return new UsernamePasswordCredentialsProvider(username, password);
    }

}
