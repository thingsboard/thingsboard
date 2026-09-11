// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;

@Data
public class DashboardUserDetailsDefinition {

    private String name;
    private boolean fullScreen;

}
