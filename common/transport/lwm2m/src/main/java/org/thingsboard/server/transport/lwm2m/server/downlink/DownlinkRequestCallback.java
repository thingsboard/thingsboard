// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

public interface DownlinkRequestCallback<R, T> {

    default boolean onSent(R request) {
        return true;
    }

    void onSuccess(R request, T response);

    void onValidationError(String params, String msg);

    void onError(String params, Exception e);

}
