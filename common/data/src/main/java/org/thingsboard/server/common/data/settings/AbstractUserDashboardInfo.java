// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.HasTitle;

import java.io.Serializable;
import java.util.UUID;

@Schema
@Data
public abstract class AbstractUserDashboardInfo implements HasTitle, Serializable {

    private static final long serialVersionUID = -6461562426034242608L;

    @Schema(description = "JSON object with Dashboard id.", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;
    @Schema(description = "Title of the dashboard.")
    private String title;

}
