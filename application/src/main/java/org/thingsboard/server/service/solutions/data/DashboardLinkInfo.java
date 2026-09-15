// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;

@Data
public class DashboardLinkInfo {

    private final String name;
    private final DashboardId dashboardId;
    private final boolean isPublic;

}
