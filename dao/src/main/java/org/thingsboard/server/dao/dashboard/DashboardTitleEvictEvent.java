// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.dashboard;

import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;

@Data
public class DashboardTitleEvictEvent {
    private final DashboardId key;
}
