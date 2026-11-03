// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.aspect;

import lombok.Data;

@Data
public class MethodCallStatsSnapshot {
    private final int executions;
    private final int failures;
    private final long timing;
}
