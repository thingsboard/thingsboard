// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;

@Data
public class DashboardLinkInfo {

    private final String name;
    private final EntityGroupId entityGroupId;
    private final DashboardId dashboardId;
    private final boolean isPublic;

}
