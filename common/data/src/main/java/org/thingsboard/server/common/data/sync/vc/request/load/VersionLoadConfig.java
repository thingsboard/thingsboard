// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

}
