package org.thingsboard.common.util;

import org.awaitility.Awaitility;
import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DonAsynchronTest {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Executor callbackExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    private final long timeout = 1000;
    private final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    @Test
    public void testSubmitWithTimeout() throws Exception {
        var task1 = DonAsynchron.submitWithTimeout(
                () -> {
                    timeUnit.sleep(timeout / 2);
                    return true;
                },
                timeout, timeUnit,
                executor, timeoutExecutor
        );
        Awaitility.await()
                .atMost(timeout, timeUnit)
                .until(task1::get);
        assertThat(task1.get()).isTrue();

        AtomicBoolean successCallbackFlag = new AtomicBoolean(false);
        DonAsynchron.submitWithTimeout(
                () -> {
                    timeUnit.sleep(timeout / 2);
                    return null;
                },
                (b) -> successCallbackFlag.set(true),
                (e) -> {},
                timeout, timeUnit,
                executor, callbackExecutor, timeoutExecutor
        );
        Awaitility.await()
                .atMost(timeout, timeUnit)
                .until(successCallbackFlag::get);
        assertThat(successCallbackFlag.get()).isTrue();


        var task2 = DonAsynchron.submitWithTimeout(
                () -> {
                    timeUnit.sleep(timeout + timeout);
                    return true;
                },
                timeout, timeUnit,
                executor, timeoutExecutor
        );
        Awaitility.await()
                .atLeast(timeout, timeUnit)
                .until(task2::isCancelled);
        assertThatThrownBy(task2::get)
                .isInstanceOf(CancellationException.class)
                .hasMessage("Task was cancelled.");

        AtomicReference<Throwable> throwableCaptor = new AtomicReference<>();
        DonAsynchron.submitWithTimeout(
                () -> {
                    timeUnit.sleep(timeout + timeout);
                    return null;
                },
                (b) -> {},
                (e) -> {
                    successCallbackFlag.set(false);
                    throwableCaptor.set(e);
                },
                timeout, timeUnit,
                executor, callbackExecutor, timeoutExecutor
        );
        Awaitility.await()
                .atLeast(timeout, timeUnit)
                .until(() -> !successCallbackFlag.get());
        assertThat(successCallbackFlag.get()).isFalse();
        assertThat(throwableCaptor.get())
                .isExactlyInstanceOf(CancellationException.class)
                .hasMessage("Task was cancelled.");
    }
}
