// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;

@Schema
@Data
@AllArgsConstructor
public class EntityDataDiff {
    @Schema(implementation = EntityExportData.class)
    private EntityExportData<?> currentVersion;
    @Schema(implementation = EntityExportData.class)
    private EntityExportData<?> otherVersion;
}
