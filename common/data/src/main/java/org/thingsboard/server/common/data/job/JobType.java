// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum JobType {

    CF_REPROCESSING("Calculated field reprocessing"),
    REPORT("Report generation"),
    DUMMY("Dummy job");

    private final String title;

    public String getTasksTopic() {
        return "tasks." + name().toLowerCase();
    }

}
