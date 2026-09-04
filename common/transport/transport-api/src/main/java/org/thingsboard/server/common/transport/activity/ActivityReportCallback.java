// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity;

public interface ActivityReportCallback<Key> {

    void onSuccess(Key key, long reportedTime);

    void onFailure(Key key, Throwable t);

}
