// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;

import java.util.UUID;

/**
 * Created by ashvayka on 24.10.18.
 */
@Data
public class AsyncTaskContext<T extends AsyncTask, V> {

    private final UUID id;
    private final T task;
    private final SettableFuture<V> future;
    private final long createTime;

}
