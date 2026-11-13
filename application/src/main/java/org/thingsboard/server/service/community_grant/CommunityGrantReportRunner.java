// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fetches or accepts the instance checker, verifies its release signature and runs it against this instance's
 * own database.
 */
@Component
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class CommunityGrantReportRunner {

    static final String REPORT_HEADER = "-----BEGIN TB INSTANCE CHECK-----";
    static final String REPORT_FOOTER = "-----END TB INSTANCE CHECK-----";

    private static final int TERMINATION_GRACE_SEC = 5;

    private static final int MAX_STDERR_CAPTURE_BYTES = 8192;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Getter
    @Value("${community-grant.checker-timeout-sec:600}")
    private int checkerTimeoutSec;

    @Value("${community-grant.max-report-size-bytes:1048576}")
    private int maxReportSizeBytes;

    @Value("${community-grant.max-checker-size-bytes:33554432}")
    private long maxCheckerSizeBytes;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    private final CommunityGrantPortalClient portalClient;
    private final CommunityGrantReleaseSignatureVerifier releaseSignatureVerifier;

    /**
     * @return the portal's status after it accepted the report, or {@code null} if the portal answered with
     * an empty body.
     */
    public String runAndUpload(String token, CommunityGrantCheckerInput checkerInput) {
        Path checker;
        try {
            checker = fetchChecker(token);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to obtain the instance checker", e);
        }
        return portalClient.uploadReport(token, runCheckerAndCleanUp(checker, checkerInput));
    }

    /**
     * The checks {@link #runUploadedChecker} runs before anything is executed, callable on their own so a
     * refusal can answer the upload request while the run itself happens later.
     *
     * @throws IllegalArgumentException on every refusal
     */
    public void verifyUploadedChecker(byte[] checkerData, byte[] signatureData, String fileName) {
        if (checkerData == null || checkerData.length == 0) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        if (checkerData.length > maxCheckerSizeBytes) {
            throw new IllegalArgumentException("The uploaded file is larger than the configured limit of "
                    + maxCheckerSizeBytes + " bytes");
        }
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        if (platform.namesAnotherPlatform(fileName)) {
            throw new IllegalArgumentException("The uploaded file does not match this server's platform ("
                    + platform.os() + "/" + platform.arch() + ")");
        }
        verifyOfflineCheckerSignature(checkerData, signatureData);
    }

    /** Repeats {@link #verifyUploadedChecker} so nothing unverified reaches disk however this is called. */
    public String runUploadedChecker(byte[] checkerData, byte[] signatureData, String fileName,
                                     CommunityGrantCheckerInput checkerInput) {
        verifyUploadedChecker(checkerData, signatureData, fileName);
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        log.info("Running an uploaded instance checker (uploaded file name {}, SHA-256 {})",
                fileName, HexFormat.of().formatHex(sha256(checkerData)));
        Path checker;
        try {
            checker = writeChecker(checkerData, platform.checkerFileName());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store the uploaded file", e);
        }
        return runCheckerAndCleanUp(checker, checkerInput);
    }

    private String runCheckerAndCleanUp(Path checker, CommunityGrantCheckerInput checkerInput) {
        try {
            return runChecker(checker, checkerInput);
        } finally {
            deleteQuietly(checker);
        }
    }

    private void verifyOfflineCheckerSignature(byte[] checkerData, byte[] signatureData) {
        try {
            releaseSignatureVerifier.verify(checkerData, signatureData);
        } catch (IllegalArgumentException e) {
            log.warn("Refused to run an uploaded instance checker (uploaded file SHA-256 {}): {}",
                    HexFormat.of().formatHex(sha256(checkerData)), e.getMessage());
            throw e;
        }
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    Path fetchChecker(String token) throws IOException {
        if (!releaseSignatureVerifier.hasTrustedKeys()) {
            throw new IllegalStateException("No trusted release-signing key is configured in "
                    + CommunityGrantReleaseSignatureVerifier.PUBLIC_KEYS_PROPERTY
                    + ", so the instance checker could not be verified and was not run");
        }
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        byte[] checkerData = portalClient.downloadChecker(token, platform.os(), platform.arch());
        verifyDownloadedCheckerSignature(token, platform, checkerData);
        return writeChecker(checkerData, platform.checkerFileName());
    }

    private void verifyDownloadedCheckerSignature(String token, CommunityGrantPlatform platform, byte[] checkerData) {
        byte[] signatureData;
        try {
            signatureData = portalClient.downloadCheckerSignature(token, platform.os(), platform.arch());
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("The portal served no release signature for the instance checker ("
                    + e.getStatusCode() + "), so it could not be verified and was not run", e);
        }
        try {
            releaseSignatureVerifier.verify(checkerData, signatureData);
        } catch (IllegalArgumentException e) {
            // The verifier's messages name an uploaded checker.
            throw new IllegalStateException("The instance checker downloaded from the portal does not match "
                    + "the release signature published for it, so it was not run", e);
        }
    }

    Path writeChecker(byte[] checkerData, String fileName) throws IOException {
        boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        Path dir = posix
                ? Files.createTempDirectory("tb-instance-check",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")))
                : Files.createTempDirectory("tb-instance-check");
        try {
            Path checker = dir.resolve(fileName);
            Files.write(checker, checkerData);
            if (posix) {
                Files.setPosixFilePermissions(checker, PosixFilePermissions.fromString("r-x------"));
            } else if (!checker.toFile().setExecutable(true, true)) {
                throw new IOException("Failed to make the instance checker executable");
            }
            return checker;
        } catch (IOException | RuntimeException e) {
            deleteRecursivelyQuietly(dir);
            throw e;
        }
    }

    /**
     * Secrets travel in the environment, never on the command line, where {@code ps} shows them to every
     * local user.
     */
    String runChecker(Path checker, CommunityGrantCheckerInput checkerInput) {
        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(checker));
        processBuilder.directory(checker.getParent().toFile());
        prepareCheckerEnvironment(processBuilder.environment(),
                CommunityGrantLibpqDsn.from(dbUrl, dbUserName, dbPassword), checkerInput);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start the instance check", e);
        }
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AtomicBoolean overflow = new AtomicBoolean(false);
        Thread stdoutDrain = drainCappedAsync(process.getInputStream(), stdout, overflow, process);
        Thread stderrDrain = drainBoundedAsync(process.getErrorStream(), stderr);
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            log.debug("Failed to close the instance check input stream", e);
        }
        boolean exited;
        try {
            exited = process.waitFor(checkerTimeoutSec, TimeUnit.SECONDS);
            if (!exited) {
                killTree(process);
                awaitTermination(process);
            }
            stdoutDrain.join(TimeUnit.SECONDS.toMillis(TERMINATION_GRACE_SEC));
            stderrDrain.join(TimeUnit.SECONDS.toMillis(TERMINATION_GRACE_SEC));
            // Ahead of the timeout: the overflow branch kills the process too.
            if (overflow.get()) {
                String ending = "produced more output than the configured limit of " + maxReportSizeBytes + " bytes";
                logCheckerDiagnostics(stderr, ending);
                throw new IllegalStateException("The instance check " + ending);
            }
            if (!exited) {
                String ending = "did not finish within " + checkerTimeoutSec + " seconds";
                logCheckerDiagnostics(stderr, ending);
                throw new IllegalStateException("The instance check " + ending);
            }
            if (stdoutDrain.isAlive()) {
                // Reading a buffer the drain is still filling could upload a truncated report. stderr is not
                // checked: a grandchild holding that pipe open would keep its drain alive indefinitely.
                String ending = "output could not be read back in time";
                logCheckerDiagnostics(stderr, ending);
                throw new IllegalStateException("The instance check " + ending);
            }
        } catch (InterruptedException e) {
            killTree(process);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running the instance check", e);
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String ending = "failed with exit code " + exitCode;
            logCheckerDiagnostics(stderr, ending);
            throw new IllegalStateException("The instance check " + ending);
        }
        return readReport(stdout);
    }

    /** Logged, never put in the exception: it is untrusted child output and the message is shown to the user. */
    private void logCheckerDiagnostics(ByteArrayOutputStream stderr, String ending) {
        String diagnostics = stderr.toString(StandardCharsets.UTF_8).trim();
        if (!diagnostics.isEmpty()) {
            log.warn("The instance check {}. It wrote to standard error: {}", ending, diagnostics);
        }
    }

    private List<String> buildCommand(Path checker) {
        List<String> command = new ArrayList<>(List.of(checker.toAbsolutePath().toString()));
        if (buildProperties != null) {
            command.add("--tb-version");
            command.add(buildProperties.getVersion());
        }
        return command;
    }

    private static String readReport(ByteArrayOutputStream stdout) {
        String report = stdout.toString(StandardCharsets.UTF_8).trim();
        if (!report.startsWith(REPORT_HEADER) || !report.endsWith(REPORT_FOOTER)) {
            throw new IllegalStateException("The instance check produced unexpected output");
        }
        return report;
    }

    /** Descendants are collected first: once the parent dies they are reparented and no longer found. */
    private static void killTree(Process process) {
        List<ProcessHandle> descendants = process.descendants().toList();
        process.destroyForcibly();
        descendants.forEach(ProcessHandle::destroyForcibly);
    }

    /**
     * Drops the inherited {@code PG*} variables, which could change how the DSN's connection is made.
     * Case-insensitive because {@link ProcessBuilder#environment()} is on Windows.
     */
    static void prepareCheckerEnvironment(Map<String, String> environment, String dsn,
                                          CommunityGrantCheckerInput checkerInput) {
        environment.keySet().removeIf(name -> name.toUpperCase(Locale.ROOT).startsWith("PG"));
        environment.put("TB_IC_DSN", dsn);
        environment.put("TB_IC_INPUT", checkerInput.json());
    }

    /** On Windows, deleting the binary of a still-exiting process fails. */
    private void awaitTermination(Process process) {
        try {
            if (!process.waitFor(TERMINATION_GRACE_SEC, TimeUnit.SECONDS)) {
                log.debug("The instance check process did not terminate within {} seconds of being killed",
                        TERMINATION_GRACE_SEC);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Thread drainCappedAsync(InputStream in, ByteArrayOutputStream sink, AtomicBoolean overflow,
                                    Process process) {
        return startDaemon("tb-instance-check-stdout", () -> {
            try (InputStream stream = in) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    if (sink.size() + read > maxReportSizeBytes) {
                        overflow.set(true);
                        killTree(process);
                        awaitTermination(process);
                        return;
                    }
                    sink.write(buffer, 0, read);
                }
            } catch (IOException e) {
                log.debug("Failed to read the instance check output", e);
            }
        });
    }

    /** Keeps reading past the cap so a chatty checker cannot deadlock on the pipe. */
    private Thread drainBoundedAsync(InputStream in, ByteArrayOutputStream sink) {
        return startDaemon("tb-instance-check-stderr", () -> {
            try (InputStream stream = in) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    int room = MAX_STDERR_CAPTURE_BYTES - sink.size();
                    if (room > 0) {
                        sink.write(buffer, 0, Math.min(read, room));
                    }
                }
            } catch (IOException e) {
                log.debug("Failed to read the instance check diagnostics", e);
            }
        });
    }

    private static Thread startDaemon(String name, Runnable body) {
        Thread thread = new Thread(body, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void deleteQuietly(Path checker) {
        deleteRecursivelyQuietly(checker.getParent());
    }

    private void deleteRecursivelyQuietly(Path dir) {
        if (!FileUtils.deleteQuietly(dir.toFile())) {
            log.debug("Failed to clean up the instance check directory {}", dir);
        }
    }

}
