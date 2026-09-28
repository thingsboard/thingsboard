// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

public interface IntegrationCallback<T> {

    void onSuccess(T msg);

    void onError(Throwable e);

}
