// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc.request.load;

import lombok.Data;

@Data
public class VersionLoadConfig {

    private boolean loadRelations;
    private boolean loadAttributes;
    private boolean loadCredentials;
    private boolean loadCalculatedFields;

}
