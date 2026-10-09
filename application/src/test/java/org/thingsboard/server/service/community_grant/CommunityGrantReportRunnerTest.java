// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityGrantReportRunnerTest {

    private static final String TOKEN = "token-one";
    private static final String REPORT = "-----BEGIN TB INSTANCE CHECK-----\nZmFrZS1yZXBvcnQtYm9keQ==\n-----END TB INSTANCE CHECK-----";
    private static final UUID CLUSTER_ID = UUID.randomUUID();
    private static final String CHECKER_INPUT_JSON = "{\"clusterId\":\"" + CLUSTER_ID + "\",\"keyId\":7,"
            + "\"publicKey\":\"cHVi\",\"challenge\":\"Y2hh\",\"referenceMillis\":1787788800000}";
    private static final CommunityGrantCheckerInput CHECKER_INPUT = new CommunityGrantCheckerInput(CHECKER_INPUT_JSON);
    private static final String TB_VERSION = "4.3.1.4";

    private static final String RELEASE_PUBLIC_KEY = CommunityGrantSigningFixtures.PUBLIC_KEY_A;
    private static final String TRUSTED_KEYS = CommunityGrantSigningFixtures.TRUSTS_A;
    private static final byte[] SIGNED_CHECKER = CommunityGrantSigningFixtures.CHECKER;
    private static final byte[] RELEASE_SIGNATURE = CommunityGrantSigningFixtures.SIGNATURE;

    @Mock
    private CommunityGrantPortalClient portalClient;

    /** Real, not stubbed, so the refusal assertions depend on actual verification. */
    @Spy
    private CommunityGrantReleaseSignatureVerifier releaseSignatureVerifier =
            new CommunityGrantReleaseSignatureVerifier();

    @Spy
    @InjectMocks
    private CommunityGrantReportRunner runner;

    private ListAppender<ILoggingEvent> testLogAppender;

    // No class-wide POSIX assumption: most tests never execute a script and must run on Windows too.
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(runner, "dbUrl", "jdbc:postgresql://localhost:5432/thingsboard");
        ReflectionTestUtils.setField(runner, "dbUserName", "postgres");
        ReflectionTestUtils.setField(runner, "dbPassword", "secret");
        ReflectionTestUtils.setField(runner, "checkerTimeoutSec", 30);
        ReflectionTestUtils.setField(runner, "maxReportSizeBytes", 1048576);
        ReflectionTestUtils.setField(runner, "maxCheckerSizeBytes", 33554432L);
        ReflectionTestUtils.setField(runner, "buildProperties", buildProperties(TB_VERSION));
        trustReleaseKeys(TRUSTED_KEYS);
        Logger logger = (Logger) LoggerFactory.getLogger(CommunityGrantReportRunner.class);
        testLogAppender = new ListAppender<>();
        testLogAppender.start();
        logger.addAppender(testLogAppender);
    }

    @AfterEach
    void tearDown() {
        if (testLogAppender != null) {
            testLogAppender.stop();
            Logger logger = (Logger) LoggerFactory.getLogger(CommunityGrantReportRunner.class);
            logger.detachAppender(testLogAppender);
        }
    }

    @Test
    void testRunAndUploadRunsTheCheckerAndUploadsItsOutput() throws IOException {
        Path checker = fakeChecker("#!/bin/sh\ncat <<'EOF'\n" + REPORT + "\nEOF\n");
        doReturn(checker).when(runner).fetchChecker(TOKEN);
        when(portalClient.uploadReport(TOKEN, REPORT)).thenReturn("DONE");

        String status = runner.runAndUpload(TOKEN, CHECKER_INPUT);

        assertThat(status).isEqualTo("DONE");
        verify(portalClient).uploadReport(TOKEN, REPORT);
    }

    @Test
    void testTheCheckerBinaryIsDeletedAfterTheRun() throws IOException {
        Path checker = fakeChecker("#!/bin/sh\ncat <<'EOF'\n" + REPORT + "\nEOF\n");
        doReturn(checker).when(runner).fetchChecker(TOKEN);
        when(portalClient.uploadReport(anyString(), anyString())).thenReturn("DONE");

        runner.runAndUpload(TOKEN, CHECKER_INPUT);

        assertThat(Files.exists(checker)).isFalse();
        assertThat(Files.exists(checker.getParent())).isFalse();
    }

    @Test
    void testTheDsnAndTheInputArePassedThroughTheEnvironmentAndOnlyTheVersionOnArgv() throws IOException {
        String report = runCheckerEchoingItsInvocation();

        assertThat(report).contains("env=postgres://postgres:secret@localhost:5432/thingsboard");
        assertThat(report).contains("input=" + CHECKER_INPUT_JSON);

        // The checker refuses a flag it does not know, so argv is pinned exactly.
        String argvLine = argvLine(report);
        assertThat(argvLine).isEqualTo("argv=--tb-version " + TB_VERSION);
        assertThat(argvLine).doesNotContain("secret");
    }

    @Test
    void testTheVersionIsLeftOffWhereTheBuildCarriesNone() throws IOException {
        ReflectionTestUtils.setField(runner, "buildProperties", null);

        String report = runCheckerEchoingItsInvocation();

        assertThat(argvLine(report)).isEqualTo("argv=");
    }

    // A constructed map: a spawned child would only see the PG* variables this host happens to export.
    @Test
    void testThePgEnvironmentIsStrippedWhileTheRestOfItSurvives() {
        Map<String, String> environment = new HashMap<>();
        environment.put("PGHOST", "attacker.example");
        environment.put("PGPASSWORD", "not-the-configured-one");
        environment.put("PGSERVICE", "redirected");
        environment.put("PGSSLMODE", "disable");
        // The Windows environment is case-insensitive, so this would reach the child as PGHOST.
        environment.put("Pghost", "attacker.example");
        environment.put("PATH", "/usr/bin");
        environment.put("HOME", "/home/thingsboard");
        environment.put("HTTPS_PROXY", "http://proxy.internal:3128");
        environment.put("TB_IC_INPUT", "{\"inherited\":true}");

        CommunityGrantReportRunner.prepareCheckerEnvironment(environment, "postgres://u:p@localhost:5432/tb",
                CHECKER_INPUT);

        assertThat(environment).doesNotContainKeys("PGHOST", "PGPASSWORD", "PGSERVICE", "PGSSLMODE", "Pghost");
        assertThat(environment)
                .containsEntry("PATH", "/usr/bin")
                .containsEntry("HOME", "/home/thingsboard")
                .containsEntry("HTTPS_PROXY", "http://proxy.internal:3128")
                .containsEntry("TB_IC_DSN", "postgres://u:p@localhost:5432/tb")
                .containsEntry("TB_IC_INPUT", CHECKER_INPUT_JSON);
    }

    @Test
    void testANonZeroExitIsAFailureAndNothingIsUploaded() throws IOException {
        Path checker = fakeChecker("#!/bin/sh\necho \"-----BEGIN TB INSTANCE CHECK-----\"\nexit 3\n");
        doReturn(checker).when(runner).fetchChecker(TOKEN);

        assertThatThrownBy(() -> runner.runAndUpload(TOKEN, CHECKER_INPUT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3");
        verify(portalClient, never()).uploadReport(anyString(), anyString());
        assertThat(Files.exists(checker.getParent())).isFalse();
    }

    @Test
    void testACheckerThatHangsIsKilledAtTheTimeout() throws IOException {
        ReflectionTestUtils.setField(runner, "checkerTimeoutSec", 1);
        Path checker = fakeChecker("#!/bin/sh\nsleep 30\n");
        doReturn(checker).when(runner).fetchChecker(TOKEN);

        assertThatThrownBy(() -> runner.runAndUpload(TOKEN, CHECKER_INPUT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not finish");
        verify(portalClient, never()).uploadReport(anyString(), anyString());
    }

    @Test
    void testOversizedOutputIsRefused() throws IOException {
        ReflectionTestUtils.setField(runner, "maxReportSizeBytes", 64);
        Path checker = fakeChecker("#!/bin/sh\necho \"-----BEGIN TB INSTANCE CHECK-----\"\n"
                + "i=0\nwhile [ $i -lt 5000 ]; do echo \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"; i=$((i+1)); done\n");
        doReturn(checker).when(runner).fetchChecker(TOKEN);

        assertThatThrownBy(() -> runner.runAndUpload(TOKEN, CHECKER_INPUT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("more output than the configured limit");
        verify(portalClient, never()).uploadReport(anyString(), anyString());
    }

    @Test
    void testOutputThatIsNotAReportIsRefused() throws IOException {
        Path checker = fakeChecker("#!/bin/sh\necho \"hello from somewhere else\"\n");
        doReturn(checker).when(runner).fetchChecker(TOKEN);

        assertThatThrownBy(() -> runner.runAndUpload(TOKEN, CHECKER_INPUT))
                .isInstanceOf(IllegalStateException.class);
        verify(portalClient, never()).uploadReport(anyString(), anyString());
    }

    @Test
    void testOutputThatIsMissingTheTrailerIsRefused() throws IOException {
        Path checker = fakeChecker("#!/bin/sh\necho \"-----BEGIN TB INSTANCE CHECK-----\"\n"
                + "echo \"ZmFrZS1yZXBvcnQtYm9keQ==\"\n");
        doReturn(checker).when(runner).fetchChecker(TOKEN);

        assertThatThrownBy(() -> runner.runAndUpload(TOKEN, CHECKER_INPUT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unexpected output");
        verify(portalClient, never()).uploadReport(anyString(), anyString());
    }

    @Test
    void testFetchCheckerWritesThePortalsBytesAsAnExecutableChecker() throws IOException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS, "The downloaded fixture is a POSIX shell script and is executed");
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        when(portalClient.downloadChecker(TOKEN, platform.os(), platform.arch())).thenReturn(SIGNED_CHECKER);
        when(portalClient.downloadCheckerSignature(TOKEN, platform.os(), platform.arch()))
                .thenReturn(RELEASE_SIGNATURE);

        Path checker = runner.fetchChecker(TOKEN);

        try {
            assertThat(checker).exists();
            assertThat(Files.isExecutable(checker)).isTrue();
            assertThat(runner.runChecker(checker, CHECKER_INPUT)).isEqualTo(REPORT);
        } finally {
            FileUtils.deleteQuietly(checker.getParent().toFile());
        }
    }

    @Test
    void testADownloadedCheckerThatDoesNotMatchItsReleaseSignatureIsRefusedAndNeverWritten() throws IOException {
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        when(portalClient.downloadChecker(TOKEN, platform.os(), platform.arch())).thenReturn(SIGNED_CHECKER);
        when(portalClient.downloadCheckerSignature(TOKEN, platform.os(), platform.arch()))
                .thenReturn(CommunityGrantSigningFixtures.OTHER_SIGNATURE);

        assertThatThrownBy(() -> runner.fetchChecker(TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match the release signature");
        verify(runner, never()).writeChecker(any(), anyString());
    }

    @Test
    void testADownloadedCheckerWithNoReleaseSignatureStagedIsRefusedAndNeverWritten() throws IOException {
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        when(portalClient.downloadChecker(TOKEN, platform.os(), platform.arch())).thenReturn(SIGNED_CHECKER);
        when(portalClient.downloadCheckerSignature(TOKEN, platform.os(), platform.arch()))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> runner.fetchChecker(TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no release signature");
        verify(runner, never()).writeChecker(any(), anyString());
    }

    @Test
    void testTheOnlineRouteRefusesToRunWhenNoReleaseKeyIsConfigured() throws IOException {
        for (String unusable : new String[]{"", "1:not base64!"}) {
            trustReleaseKeys(unusable);

            assertThatThrownBy(() -> runner.fetchChecker(TOKEN))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(CommunityGrantReleaseSignatureVerifier.PUBLIC_KEYS_PROPERTY)
                    .hasMessageContaining("was not run");
        }
        verify(portalClient, never()).downloadChecker(anyString(), anyString(), anyString());
        verify(runner, never()).writeChecker(any(), anyString());
    }

    @Test
    void testTheStagedCheckerIsOwnerOnlyAndNotWritable() throws IOException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS, "POSIX permissions");
        Path checker = runner.writeChecker("#!/bin/sh\n".getBytes(StandardCharsets.UTF_8), "tb-instance-check");

        try {
            assertThat(Files.getPosixFilePermissions(checker))
                    .isEqualTo(PosixFilePermissions.fromString("r-x------"));
            assertThat(Files.getPosixFilePermissions(checker.getParent()))
                    .isEqualTo(PosixFilePermissions.fromString("rwx------"));
        } finally {
            FileUtils.deleteQuietly(checker.getParent().toFile());
        }
    }

    @Test
    void testCurrentPlatformIsMappedToThePortalVocabulary() {
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        assertThat(platform.os()).isIn("linux", "darwin", "windows");
        assertThat(platform.arch()).isIn("amd64", "arm64");
    }

    @Test
    void testUploadedCheckerRunsWhenItsReleaseSignatureVerifies() {
        assumeFalse(SystemUtils.IS_OS_WINDOWS, "The signed fixture is a POSIX shell script and is actually executed");
        String report = runner.runUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE, null, CHECKER_INPUT);

        assertThat(report).isEqualTo(REPORT);
    }

    @Test
    void testATamperedUploadedCheckerIsRefusedAndNeverExecuted() throws IOException {
        byte[] tampered = SIGNED_CHECKER.clone();
        tampered[10] ^= 0x01;

        assertThatThrownBy(() -> runner.runUploadedChecker(tampered, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match its release signature");
        // Never written either, which pins the check ahead of writeChecker.
        verify(runner, never()).writeChecker(any(), anyString());
        verify(runner, never()).runChecker(any(), any());
        verify(portalClient, never()).uploadReport(anyString(), anyString());
    }

    @Test
    void testVerifyingAnUploadRefusesATamperedCheckerAndNeverWritesOrRunsOne() throws IOException {
        byte[] tampered = SIGNED_CHECKER.clone();
        tampered[10] ^= 0x01;

        assertThatThrownBy(() -> runner.verifyUploadedChecker(tampered, RELEASE_SIGNATURE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match its release signature");
        runner.verifyUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE, null);

        verify(runner, never()).writeChecker(any(), anyString());
        verify(runner, never()).runChecker(any(), any());
    }

    // Refusals ahead of the signature check do not log; the controller's audit row records them.
    @Test
    void testAnEmptyUploadIsRefusedWithoutWritingOrExecutingAnything() throws IOException {
        assertThatThrownBy(() -> runner.runUploadedChecker(new byte[0], RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is empty");

        assertNothingWasWrittenExecutedOrLogged();
    }

    @Test
    void testAnOversizedUploadIsRefusedWithoutWritingOrExecutingAnything() throws IOException {
        ReflectionTestUtils.setField(runner, "maxCheckerSizeBytes", (long) SIGNED_CHECKER.length - 1);

        assertThatThrownBy(() -> runner.runUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("larger than the configured limit");

        assertNothingWasWrittenExecutedOrLogged();
    }

    @Test
    void testAnUploadNamingAnotherPlatformIsRefusedWithoutWritingOrExecutingAnything() throws IOException {
        assertThatThrownBy(() -> runner.runUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE,
                anotherPlatformFileName(), CHECKER_INPUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match this server's platform");

        assertNothingWasWrittenExecutedOrLogged();
    }

    @Test
    void testAFileNameNamingNoOtherPlatformReachesTheSignatureCheck() {
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        byte[] tampered = SIGNED_CHECKER.clone();
        tampered[10] ^= 0x01;

        for (String fileName : List.of(platform.checkerFileName(),
                "tb-instance-check_" + platform.os() + "_" + platform.arch())) {
            assertThatThrownBy(() -> runner.runUploadedChecker(tampered, RELEASE_SIGNATURE, fileName, CHECKER_INPUT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not match its release signature");
        }
    }

    @Test
    void testAnUploadedCheckerWithNoSignatureIsRefusedAndNeverExecuted() throws IOException {
        assertThatThrownBy(() -> runner.runUploadedChecker(SIGNED_CHECKER, null, null, CHECKER_INPUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carries no release signature");
        verify(runner, never()).writeChecker(any(), anyString());
        verify(runner, never()).runChecker(any(), any());
    }

    @Test
    void testARefusedUploadIsLoggedAtWarnWithTheUploadedDigestAndNoSecrets() {
        byte[] tampered = SIGNED_CHECKER.clone();
        tampered[10] ^= 0x01;
        String uploadedDigest = sha256Hex(tampered);

        assertThatThrownBy(() -> runner.runUploadedChecker(tampered, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(testLogAppender.list).hasSize(1);
        ILoggingEvent event = testLogAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        String formatted = event.getFormattedMessage();
        assertThat(formatted).contains("Refused").contains(uploadedDigest);
        assertThat(formatted).doesNotContain(RELEASE_PUBLIC_KEY);
        assertNoSecretsLeaked(formatted);
    }

    @Test
    void testAnUploadedCheckerIsRefusedWhenNoReleaseKeyIsConfigured() throws IOException {
        trustReleaseKeys("");

        assertThatThrownBy(() -> runner.runUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("community-grant.checker-release-public-keys");
        verify(runner, never()).writeChecker(any(), anyString());
        verify(runner, never()).runChecker(any(), any());
    }

    @Test
    void testAnUnconfiguredAndAMisconfiguredDeploymentAreRefusedDifferently() throws IOException {
        trustReleaseKeys("");
        assertThatThrownBy(() -> runner.runUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .hasMessageContaining("refuses to run one")
                .hasMessageNotContaining("could not be parsed");

        trustReleaseKeys("1:" + RELEASE_PUBLIC_KEY + ",2:not base64!");
        assertThatThrownBy(() -> runner.runUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .hasMessageContaining("could not be parsed")
                .hasMessageNotContaining("refuses to run one");

        verify(runner, never()).writeChecker(any(), anyString());
        verify(runner, never()).runChecker(any(), any());
    }

    @Test
    void testTheRefusalMessagesNeverLeakTheDatasourcePasswordOrDsn() {
        byte[] tampered = SIGNED_CHECKER.clone();
        tampered[10] ^= 0x01;

        assertThatThrownBy(() -> runner.runUploadedChecker(tampered, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .satisfies(e -> assertNoSecretsLeaked(e.getMessage()));

        trustReleaseKeys("");
        assertThatThrownBy(() -> runner.runUploadedChecker(SIGNED_CHECKER, RELEASE_SIGNATURE, null, CHECKER_INPUT))
                .satisfies(e -> assertNoSecretsLeaked(e.getMessage()));
    }

    private static String anotherPlatformFileName() {
        CommunityGrantPlatform platform = CommunityGrantPlatform.current();
        return "tb-instance-check_" + ("linux".equals(platform.os()) ? "windows" : "linux") + "_" + platform.arch();
    }

    private void trustReleaseKeys(String configured) {
        ReflectionTestUtils.setField(releaseSignatureVerifier, "checkerReleasePublicKeys", configured);
        releaseSignatureVerifier.init();
    }

    private void assertNoSecretsLeaked(String message) {
        assertThat(message).doesNotContain("secret");
        assertThat(message).doesNotContain("postgres://");
        assertThat(message).doesNotContain(TOKEN);
    }

    private static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static BuildProperties buildProperties(String version) {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        return new BuildProperties(properties);
    }

    private String runCheckerEchoingItsInvocation() throws IOException {
        Path checker = fakeChecker("#!/bin/sh\n"
                + "echo \"-----BEGIN TB INSTANCE CHECK-----\"\n"
                + "echo \"env=$TB_IC_DSN\"\n"
                + "echo \"input=$TB_IC_INPUT\"\n"
                + "echo \"argv=$*\"\n"
                + "echo \"-----END TB INSTANCE CHECK-----\"\n");
        try {
            return runner.runChecker(checker, CHECKER_INPUT);
        } finally {
            FileUtils.deleteQuietly(checker.getParent().toFile());
        }
    }

    private static String argvLine(String report) {
        return Arrays.stream(report.split("\n"))
                .filter(line -> line.startsWith("argv="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The fake checker's output has no argv line"));
    }

    private Path fakeChecker(String script) throws IOException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS, "The fake checker is a POSIX shell script with POSIX permissions");
        return runner.writeChecker(script.getBytes(StandardCharsets.UTF_8), "tb-instance-check");
    }

    private void assertNothingWasWrittenExecutedOrLogged() throws IOException {
        verify(runner, never()).writeChecker(any(), anyString());
        verify(runner, never()).runChecker(any(), any());
        assertThat(testLogAppender.list).isEmpty();
    }

}
