// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import java.util.concurrent.ScheduledExecutorService;

public interface ExecutorProvider {

    ScheduledExecutorService getExecutor();
}
