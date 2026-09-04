// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import com.google.common.util.concurrent.AbstractFuture;

/**
 * Created by ashvayka on 21.02.17.
 */
public class SimpleListenableFuture<V> extends AbstractFuture<V> {

    public boolean set(V value) {
        return super.set(value);
    }

    public boolean setException(Throwable throwable) {
        return super.setException(throwable);
    }

}
