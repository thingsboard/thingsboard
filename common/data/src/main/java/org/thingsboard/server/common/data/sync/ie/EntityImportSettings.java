// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

}
