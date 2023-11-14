package org.thingsboard.common.util;

import com.google.api.gax.core.FixedExecutorProvider;

public interface PubSubExecutor {
    FixedExecutorProvider getExecutorProvider();
}
