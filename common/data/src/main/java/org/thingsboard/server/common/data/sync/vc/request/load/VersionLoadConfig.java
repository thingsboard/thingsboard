// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.sync.vc.request.load;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class VersionLoadConfig {

    private boolean loadRelations;
    private boolean loadAttributes;
    private boolean loadCredentials;
    private boolean loadCalculatedFields;
    private boolean loadPermissions;
    private boolean loadGroupEntities;
    private boolean autoGenerateIntegrationKey;

}
