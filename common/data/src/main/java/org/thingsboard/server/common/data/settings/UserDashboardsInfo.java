// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Schema
@Data
@AllArgsConstructor
public class UserDashboardsInfo implements Serializable {

    private static final long serialVersionUID = 2628320657987010348L;
    public static final UserDashboardsInfo EMPTY = new UserDashboardsInfo(Collections.emptyList(), Collections.emptyList());

    @Schema(description = "List of last visited dashboards.", accessMode = Schema.AccessMode.READ_ONLY)
    private List<LastVisitedDashboardInfo> last;

    @Schema(description = "List of starred dashboards.", accessMode = Schema.AccessMode.READ_ONLY)
    private List<StarredDashboardInfo> starred;

    public UserDashboardsInfo() {
        this(new ArrayList<>(), new ArrayList<>());
    }
}
