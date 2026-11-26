// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class DefaultGitSyncServiceTest {

    private static final long FETCH_FREQUENCY_MS = 100;

    @TempDir
    Path tempDir;

    private DefaultGitSyncService service;
    private String remoteUri;

    @BeforeEach
    public void setUp() throws Exception {
        service = new DefaultGitSyncService();
        ReflectionTestUtils.setField(service, "repositoriesFolder", tempDir.resolve("repositories").toString());

        Path remote = tempDir.resolve("remote");
        try (Git git = Git.init().setDirectory(remote.toFile()).setInitialBranch("main").call()) {
            Files.writeString(remote.resolve("file.txt"), "content");
            git.add().addFilepattern("file.txt").call();
            git.commit().setMessage("initial").setSign(false).call();
        }
        remoteUri = remote.toUri().toString();
    }

    @AfterEach
    public void tearDown() {
        ReflectionTestUtils.invokeMethod(service, "preDestroy");
    }

    @Test
    public void listenerIsNotifiedOnlyOnInitWhenTheRepositoryDoesNotChange() throws Exception {
        AtomicInteger notifications = new AtomicInteger();

        service.registerSync("repo", remoteUri, "main", FETCH_FREQUENCY_MS, notifications::incrementAndGet);

        await().atMost(10, TimeUnit.SECONDS).until(() -> notifications.get() == 1);
        Thread.sleep(FETCH_FREQUENCY_MS * 5);
        assertThat(notifications.get()).isEqualTo(1);
    }

    @Test
    public void listenerIsNotifiedOnEveryFetchWhenRequested() {
        AtomicInteger notifications = new AtomicInteger();

        service.registerSync("repo", remoteUri, "main", FETCH_FREQUENCY_MS, notifications::incrementAndGet, true);

        await().atMost(10, TimeUnit.SECONDS).until(() -> notifications.get() >= 3);
    }

}
