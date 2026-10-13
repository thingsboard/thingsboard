// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.sync.ie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntityExportSettings {

    private boolean exportRelations;
    private boolean exportAttributes;
    private boolean exportCredentials;
    private boolean exportCalculatedFields;
    private boolean exportPermissions;
    private boolean exportGroupEntities;
    private boolean embedGroupMembers;

}
