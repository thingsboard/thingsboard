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
public class EntityImportSettings {

    private boolean findExistingByName;
    private boolean updateRelations;
    private boolean saveAttributes;
    private boolean saveCredentials;
    private boolean saveCalculatedFields;
    private boolean saveUserGroupPermissions;
    private boolean autoGenerateIntegrationKey;

}
