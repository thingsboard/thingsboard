// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntityVersionsDiff {
    private EntityId externalId;
    private EntityExportData<?> entityDataAtVersion1;
    private EntityExportData<?> entityDataAtVersion2;
    private String rawDiff;
}
