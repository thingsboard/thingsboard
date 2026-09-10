// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import com.google.common.util.concurrent.FutureCallback;

public enum NoOpFutureCallback implements FutureCallback<Object> {

    INSTANCE;

    @Override
    public void onSuccess(Object result) {}

    @Override
    public void onFailure(Throwable t) {}

    @SuppressWarnings("unchecked")
    public static <T> FutureCallback<T> instance() {
        return (FutureCallback<T>) INSTANCE;
    }

}
