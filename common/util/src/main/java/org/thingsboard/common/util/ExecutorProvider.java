package org.thingsboard.common.util;

import java.util.concurrent.ScheduledExecutorService;

public interface ExecutorProvider {

    ScheduledExecutorService getExecutor();
}
