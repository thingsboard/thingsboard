// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity;

import lombok.Data;

@Data
public class ActivityState<Metadata> {

    private volatile long lastRecordedTime;
    private volatile Metadata metadata;

}
