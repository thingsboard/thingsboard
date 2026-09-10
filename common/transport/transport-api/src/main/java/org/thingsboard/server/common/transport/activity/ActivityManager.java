// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity;

public interface ActivityManager<Key, Metadata> {

    void onActivity(Key key, Metadata metadata, long activityTimeMillis);

    void onReportingPeriodEnd();

    long getLastRecordedTime(Key key);

}
