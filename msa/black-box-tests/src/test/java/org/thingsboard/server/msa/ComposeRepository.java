// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;

/**
 * Checks out the compose stack the black-box suite runs against. Replaces the former {@code docker} git submodule.
 */
@Slf4j
public final class ComposeRepository {

    static final String REF_PROPERTY = "tb.compose.ref";
    static final String REPOSITORY_PROPERTY = "tb.compose.repository";
    static final String CACHE_DIR_PROPERTY = "tb.compose.cacheDir";

    private static final String DEFAULT_REPOSITORY = "https://github.com/thingsboard/thingsboard-pe-docker-compose.git";
    private static final String REMOTE = "origin";
    // JGit takes its transport timeout in seconds, and defaults to none. Without one, a black-holed network - packets
    // dropped rather than refused - hangs the suite inside @BeforeSuite until CI kills the job, instead of raising the
    // TransportException the offline fallback below waits for.
    private static final int TRANSPORT_TIMEOUT_SECONDS = 60;
    /**
     * Names the repository a cache was cloned from. Kept inside {@code .git}, because {@link #refresh} cleans the
     * working tree of everything untracked - a marker there would not survive the first refresh.
     */
    private static final String REPOSITORY_MARKER = "tb-compose-repository";

    private ComposeRepository() {
    }

    /**
     * Clones or refreshes the local cache and returns the working tree of the pinned compose branch. The caller must
     * close the result once it has finished reading the tree.
     */
    public static Checkout checkout() {
        String ref = pinnedRef();
        String repository = System.getProperty(REPOSITORY_PROPERTY, DEFAULT_REPOSITORY);
        Path cacheDir = cacheDir(ref, repository).toAbsolutePath();
        Path lockFile = cacheDir.resolveSibling(cacheDir.getFileName() + ".lock");
        FileChannel channel = null;
        FileLock lock;
        try {
            Files.createDirectories(lockFile.getParent());
            channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            log.info("Acquiring the compose cache lock {}", lockFile);
            lock = channel.lock();
        } catch (IOException e) {
            releaseQuietly(channel);
            throw new IllegalStateException("Failed to acquire the compose cache lock " + lockFile + ".", e);
        }
        try {
            if (!Files.isDirectory(cacheDir.resolve(".git")) || !refresh(cacheDir, repository, ref)) {
                cloneShallow(cacheDir, repository, ref);
            }
        } catch (RuntimeException e) {
            releaseQuietly(channel);
            throw e;
        }
        log.info("Compose stack of {} at branch '{}' is checked out in {}", repository, ref, cacheDir);
        return new Checkout(cacheDir, lock, channel);
    }

    private static void releaseQuietly(FileChannel channel) {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
        } catch (IOException e) {
            log.warn("Failed to release the compose cache lock", e);
        }
    }

    /**
     * A checked-out compose tree and the lock that keeps concurrent runs from resetting it mid-read. The cache is the
     * only state the suite shares between runs, so the lock must outlive the checkout itself and span the caller's copy.
     */
    public static final class Checkout implements Closeable {

        private final Path path;
        private final FileLock lock;
        private final FileChannel channel;

        private Checkout(Path path, FileLock lock, FileChannel channel) {
            this.path = path;
            this.lock = lock;
            this.channel = channel;
        }

        public Path getPath() {
            return path;
        }

        @Override
        public void close() throws IOException {
            try {
                lock.release();
            } finally {
                channel.close();
            }
        }
    }

    private static String pinnedRef() {
        String ref = System.getProperty(REF_PROPERTY);
        if (StringUtils.isBlank(ref)) {
            throw new IllegalStateException("System property " + REF_PROPERTY + " is not set. It is declared as the " +
                    "<tb.compose.ref> Maven property in msa/black-box-tests/pom.xml and handed to the test JVM by " +
                    "surefire; pass -D" + REF_PROPERTY + "=<branch> explicitly when running outside Maven.");
        }
        return ref;
    }

    /**
     * Keyed on the repository as well as the ref, so a run against a fork does not reuse - or overwrite - the cache of
     * the default repository. A configured directory is used verbatim; what protects that one is the marker file every
     * cache carries, which {@link #refresh} compares against the configured repository.
     */
    private static Path cacheDir(String ref, String repository) {
        String configured = System.getProperty(CACHE_DIR_PROPERTY);
        if (StringUtils.isNotBlank(configured)) {
            return Path.of(configured);
        }
        // User-level rather than under target/, so that the clone survives mvn clean.
        return Path.of(System.getProperty("user.home"), ".cache", "thingsboard",
                "compose-" + ref.replace('/', '_') + "-" + shortHash(repository));
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static Path repositoryMarker(Path cacheDir) {
        return cacheDir.resolve(".git").resolve(REPOSITORY_MARKER);
    }

    private static void recordRepository(Path cacheDir, String repository) {
        Path marker = repositoryMarker(cacheDir);
        try {
            Files.writeString(marker, repository, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to record the compose repository URL in " + marker + ".", e);
        }
    }

    /** The repository a cache was cloned from, or {@code null} when it predates the marker or cannot be read. */
    private static String recordedRepository(Path cacheDir) {
        Path marker = repositoryMarker(cacheDir);
        try {
            return Files.isRegularFile(marker) ? Files.readString(marker, StandardCharsets.UTF_8).trim() : null;
        } catch (IOException e) {
            log.warn("Failed to read {}; treating the cache as one of an unknown repository", marker, e);
            return null;
        }
    }

    private static void cloneShallow(Path cacheDir, String repository, String ref) {
        verifyRemoteBranch(repository, ref, cacheDir);
        try {
            discardCloneTarget(cacheDir);
            log.info("Cloning compose repository {} at branch '{}' into {}", repository, ref, cacheDir);
            // Unlike GitRepository.clone() in common/version-control, the checkout must happen: the suite copies the working tree.
            try (Git ignored = Git.cloneRepository()
                    .setURI(repository)
                    .setDirectory(cacheDir.toFile())
                    .setRemote(REMOTE)
                    .setBranch(ref)
                    .setBranchesToClone(List.of("refs/heads/" + ref))
                    .setCloneAllBranches(false)
                    .setNoTags()
                    .setDepth(1)
                    .setTimeout(TRANSPORT_TIMEOUT_SECONDS)
                    .call()) {
                log.debug("Cloned {} into {}", repository, cacheDir);
            }
            recordRepository(cacheDir, repository);
        } catch (IOException | GitAPIException | JGitInternalException e) {
            throw new IllegalStateException("Failed to clone the compose repository " + repository + " at branch '" +
                    ref + "' into " + cacheDir + ", and no usable cached checkout is present there to fall back to.", e);
        }
    }

    /**
     * Clears the way for a fresh clone, refusing to delete a path that holds something other than a previous cache.
     */
    private static void discardCloneTarget(Path cacheDir) throws IOException {
        if (!Files.exists(cacheDir) || (Files.isDirectory(cacheDir) && isEmptyDirectory(cacheDir))) {
            return;
        }
        if (!Files.isDirectory(cacheDir) || !Files.exists(cacheDir.resolve(".git"))) {
            throw new IllegalStateException(cacheDir + " already exists and is not a compose cache: a cache is a " +
                    "directory holding a .git entry. It will not be deleted - remove it by hand, or point the " +
                    CACHE_DIR_PROPERTY + " property at another path.");
        }
        log.info("Discarding the unusable compose cache in {} before cloning into it", cacheDir);
        FileUtils.forceDelete(cacheDir.toFile());
    }

    private static boolean isEmptyDirectory(Path dir) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            return !entries.iterator().hasNext();
        }
    }

    /**
     * Brings the cached checkout back in line with the pinned branch. Returns {@code false} when the cache belongs to
     * another repository, or is past repairing - a {@code .git} left behind by a killed clone, a truncated object
     * store - and the caller should throw it away and clone again. A network failure is never reported that way: the
     * cache is the very thing the offline fallback runs from, so an outage must not be what deletes it.
     */
    private static boolean refresh(Path cacheDir, String repository, String ref) {
        String cachedRepository = recordedRepository(cacheDir);
        if (!repository.equals(cachedRepository)) {
            // Discarded and cloned again, in that order: if the configured remote cannot be reached the clone then
            // fails outright with the cache already gone, which is the loud failure. Silently reusing another
            // repository's compose stack is the outcome this exists to prevent.
            log.warn("The compose cache in {} was cloned from {} rather than from the configured {}; discarding it " +
                     "before cloning again", cacheDir, cachedRepository, repository);
            return false;
        }
        try (Git git = Git.open(cacheDir.toFile())) {
            if (verifyRemoteBranch(repository, ref, cacheDir)) {
                try {
                    git.fetch()
                            .setRemote(repository)
                            .setRefSpecs(new RefSpec("+refs/heads/" + ref + ":refs/remotes/" + REMOTE + "/" + ref))
                            .setDepth(1)
                            .setTimeout(TRANSPORT_TIMEOUT_SECONDS)
                            .call();
                } catch (TransportException e) {
                    log.warn("Failed to fetch branch '{}' from {}, reusing the cached checkout in {} as is", ref, repository, cacheDir, e);
                }
            } else {
                log.warn("Falling back to the cached checkout of branch '{}' in {} as is", ref, cacheDir);
            }
            // Any local drift is discarded rather than merged: the cache is a mirror of the pinned branch, not a work area.
            git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef("refs/remotes/" + REMOTE + "/" + ref)
                    .call();
            git.clean()
                    .setCleanDirectories(true)
                    .setForce(true)
                    .setIgnore(false)
                    .call();
            return true;
        } catch (IOException | GitAPIException | JGitInternalException e) {
            if (isTransportFailure(e)) {
                throw new IllegalStateException("Failed to refresh the cached checkout of the compose repository " +
                        repository + " at branch '" + ref + "' in " + cacheDir + " because the remote could not be reached.", e);
            }
            log.warn("The compose cache in {} is unusable and will be cloned again from {}", cacheDir, repository, e);
            return false;
        }
    }

    /**
     * Tells a remote that answered and refused - no such repository, or credentials rejected - from one that was never
     * reached. Only the latter may fall back to the cache; the former means the configured repository is wrong.
     */
    private static boolean isRemoteRefusal(Throwable failure) {
        return ExceptionUtils.getThrowableList(failure).stream()
                .anyMatch(cause -> cause instanceof NoRemoteRepositoryException
                                   || (cause instanceof org.eclipse.jgit.errors.TransportException
                                       && StringUtils.contains(cause.getMessage(), JGitText.get().notAuthorized)));
    }

    private static boolean isTransportFailure(Throwable failure) {
        // Both TransportException classes: the porcelain one normally wraps the transport one, but the transport one
        // is an IOException and can reach us unwrapped.
        return ExceptionUtils.getThrowableList(failure).stream()
                .anyMatch(cause -> cause instanceof TransportException
                                   || cause instanceof org.eclipse.jgit.errors.TransportException);
    }

    /**
     * Fails unless the pinned branch exists in the remote. Returns {@code false} only when the remote could not be
     * reached at all and an existing cache could stand in for it; what to do with that is the caller's decision.
     */
    private static boolean verifyRemoteBranch(String repository, String ref, Path cacheDir) {
        Collection<Ref> remoteRefs;
        try {
            remoteRefs = Git.lsRemoteRepository()
                    .setRemote(repository)
                    .setHeads(true)
                    .setTimeout(TRANSPORT_TIMEOUT_SECONDS)
                    .call();
        } catch (TransportException e) {
            if (isRemoteRefusal(e)) {
                throw new IllegalStateException("The compose repository " + repository + " refused the request: it " +
                        "does not exist there, or the credentials were rejected. Check the " + REPOSITORY_PROPERTY +
                        " property - a stale cache must not stand in for a misconfigured remote.", e);
            }
            if (Files.isDirectory(cacheDir.resolve(".git"))) {
                log.warn("Cannot reach {} to verify branch '{}'", repository, ref, e);
                return false;
            }
            throw new IllegalStateException("Cannot reach the compose repository " + repository + " to check out branch '" +
                    ref + "', and no cached checkout is present in " + cacheDir + ".", e);
        } catch (GitAPIException e) {
            throw new IllegalStateException("Failed to list the branches of the compose repository " + repository + ".", e);
        }
        boolean present = remoteRefs.stream().anyMatch(remoteRef -> remoteRef.getName().equals("refs/heads/" + ref));
        if (!present) {
            throw new IllegalStateException("Branch '" + ref + "' does not exist in the compose repository " + repository +
                    ". The black-box suite runs against the compose stack on that branch: create it, or point the " +
                    REF_PROPERTY + " property at an existing branch. Cache directory: " + cacheDir + ".");
        }
        return true;
    }

}
