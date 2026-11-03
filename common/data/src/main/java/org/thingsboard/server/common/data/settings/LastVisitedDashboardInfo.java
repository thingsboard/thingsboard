// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Schema
@Data
public class LastVisitedDashboardInfo extends AbstractUserDashboardInfo implements Serializable {

    private static final long serialVersionUID = -6461562426034242608L;

    @Schema(description = "Starred flag")
    private boolean starred;
    @Schema(description = "Last visit timestamp")
    private long lastVisited;

}
