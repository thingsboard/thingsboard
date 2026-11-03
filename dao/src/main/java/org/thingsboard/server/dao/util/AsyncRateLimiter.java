// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncRateLimiter {

    ListenableFuture<Void> acquireAsync();

    void release();
}
