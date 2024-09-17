package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class ThingsBoardScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    public ThingsBoardScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    public ThingsBoardScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public ThingsBoardScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public ThingsBoardScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        logExceptionsAfterExecute(r, t);
    }

    private static void logExceptionsAfterExecute(Runnable r, Throwable directThrowable) {
        Throwable wrappedThrowable = extractThrowableWrappedInFuture(r);
        if (wrappedThrowable != null) {
            if (wrappedThrowable instanceof CancellationException) {
                log.debug("Task was cancelled.", wrappedThrowable);
            } else {
                log.error("Unexpected exception occurred during task execution!", wrappedThrowable);
            }
        }

        if (directThrowable != null) {
            log.error("Unexpected exception occurred during task execution!", directThrowable);
        }
    }

    private static Throwable extractThrowableWrappedInFuture(Runnable runnable) {
        if (runnable instanceof Future<?> && ((Future<?>) runnable).isDone()) {
            try {
                ((Future<?>) runnable).get();
            } catch (InterruptedException e) { // should not happen due to isDone() check
                throw new AssertionError(e);
            } catch (CancellationException e) {
                return e;
            } catch (ExecutionException e) {
                return e.getCause();
            }
        }
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("command is null");
        }
        return super.scheduleAtFixedRate(new SafeRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("command is null");
        }
        return super.scheduleWithFixedDelay(new SafeRunnable(command), initialDelay, delay, unit);
    }

    private record SafeRunnable(Runnable runnable) implements Runnable {

        public void run() {
            try {
                runnable.run();
            } catch (Exception ex) {
                log.error("Unexpected exception occurred while periodically executing task!", ex);
                // TODO: is calling uncaught execution handler here correct?
                if (Thread.getDefaultUncaughtExceptionHandler() != null) {
                    log.debug("Default exception handler is set, delegating exception handling to it");
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex);
                }
            }
            // Intentionally, no catch block for Throwable; uncaught Throwables will be handled in afterExecute()
        }

    }

}
