// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModbusClientRetryDecoratorTest {

    private static final long MAX_DELAY_MS = 4000;

    private ModbusClientAdapter delegate;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduled;
    private ModbusClientRetryDecorator decorator;

    @BeforeEach
    void setUp() {
        delegate = mock(ModbusClientAdapter.class);
        scheduler = mock(ScheduledExecutorService.class);
        scheduled = mock(ScheduledFuture.class);
        doReturn(scheduled).when(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
        when(delegate.disconnect()).thenReturn(CompletableFuture.completedFuture(null));
        decorator = new ModbusClientRetryDecorator("serial:///dev/ttyUSB0", delegate, scheduler, MAX_DELAY_MS);
    }

    @Test
    void successfulConnectSchedulesNothing() {
        when(delegate.connect()).thenReturn(CompletableFuture.completedFuture(null));

        decorator.connect();

        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void delayDoublesUpToTheMaximum() {
        when(delegate.connect()).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("port is busy")));

        decorator.connect();
        List<Long> delays = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            delays.add(runScheduledRetry(i + 1));
        }

        assertThat(delays.get(0)).isEqualTo(500L);
        assertThat(delays.get(1)).isEqualTo(1000L);
        assertThat(delays.get(2)).isEqualTo(2000L);
        assertThat(delays.get(3)).isEqualTo(4000L);
        assertThat(delays.get(4)).isEqualTo(4000L);
        assertThat(delays.get(5)).isEqualTo(4000L);
        verify(delegate, times(7)).connect();
    }

    @Test
    void delayStartsOverAfterTheConnectionOpens() {
        when(delegate.connect())
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("no such port")))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("no such port")))
                .thenReturn(CompletableFuture.completedFuture(null))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("no such port")));

        decorator.connect();
        runScheduledRetry(1);
        runScheduledRetry(2);   // this attempt succeeds
        decorator.connect();    // fails again later

        ArgumentCaptor<Long> delay = ArgumentCaptor.forClass(Long.class);
        verify(scheduler, times(3)).schedule(any(Runnable.class), delay.capture(), eq(TimeUnit.MILLISECONDS));
        assertThat(delay.getAllValues().get(2)).isEqualTo(500L);
    }

    @Test
    void failuresWhileARetryIsPendingDoNotScheduleAnother() {
        when(delegate.connect()).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("port is busy")));

        decorator.connect();
        decorator.connect();

        verify(scheduler, times(1)).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void disconnectCancelsThePendingRetryAndStopsFurtherAttempts() {
        when(delegate.connect()).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("port is busy")));
        decorator.connect();
        ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(retry.capture(), anyLong(), eq(TimeUnit.MILLISECONDS));

        decorator.disconnect();
        retry.getValue().run();   // a task that had already started when it was cancelled

        verify(scheduled).cancel(false);
        verify(delegate).disconnect();
        verify(delegate, times(1)).connect();
        assertThat(decorator.connect().toCompletableFuture()).isCompletedExceptionally();
        verify(delegate, times(1)).connect();
    }

    @Test
    void connectionOpenedAfterDisconnectIsClosedAgain() {
        CompletableFuture<Void> slowOpen = new CompletableFuture<>();
        when(delegate.connect()).thenReturn(slowOpen);
        decorator.connect();

        decorator.disconnect();
        slowOpen.complete(null);

        verify(delegate, times(2)).disconnect();
    }

    @Test
    void failureOfTheConnectionDuringARequestReleasesItAndSchedulesAReconnect() {
        when(delegate.connect()).thenReturn(CompletableFuture.completedFuture(null));
        when(delegate.read(1, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2))
                .thenReturn(CompletableFuture.failedFuture(new CompletionException(new IllegalStateException("failed to write to port"))));
        decorator.connect();

        var read = decorator.read(1, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2);

        assertThat(read.toCompletableFuture()).isCompletedExceptionally();
        verify(delegate).disconnect();
        assertThat(runScheduledRetry(1)).isEqualTo(500L);
        verify(delegate, times(2)).connect();
    }

    @Test
    void timeoutAndExceptionResponseAreNotAConnectionFailure() {
        when(delegate.read(1, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("no response")));
        when(delegate.writeCoil(1, ModbusFunctionCode.WRITE_SINGLE_COIL, 0, true))
                .thenReturn(CompletableFuture.failedFuture(mock(ModbusResponseException.class)));

        decorator.read(1, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2);
        decorator.writeCoil(1, ModbusFunctionCode.WRITE_SINGLE_COIL, 0, true);

        verify(delegate, never()).disconnect();
        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void requestsFailingWhileAReconnectIsPendingDoNotScheduleAnother() {
        when(delegate.read(1, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("not connected")));

        decorator.read(1, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2);
        decorator.read(1, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2);

        verify(delegate, times(1)).disconnect();
        verify(scheduler, times(1)).schedule(any(Runnable.class), anyLong(), any());
    }

    /** Runs the retry scheduled by the {@code n}-th failure and returns the delay it was scheduled with. */
    private long runScheduledRetry(int n) {
        ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> delay = ArgumentCaptor.forClass(Long.class);
        verify(scheduler, times(n)).schedule(retry.capture(), delay.capture(), eq(TimeUnit.MILLISECONDS));
        retry.getAllValues().get(n - 1).run();
        return delay.getAllValues().get(n - 1);
    }

}
