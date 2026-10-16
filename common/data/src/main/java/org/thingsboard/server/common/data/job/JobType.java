// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum JobType {

    DUMMY("Dummy job");

    private final String title;

    public String getTasksTopic() {
        return "tasks." + name().toLowerCase();
    }

}
