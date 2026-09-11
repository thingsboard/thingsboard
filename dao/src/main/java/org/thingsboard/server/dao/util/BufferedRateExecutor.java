// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created by ashvayka on 24.10.18.
 */
public interface BufferedRateExecutor<T extends AsyncTask, F extends ListenableFuture> {

    F submit(T task);

}
