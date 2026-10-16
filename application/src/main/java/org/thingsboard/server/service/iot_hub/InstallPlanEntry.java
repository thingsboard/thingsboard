// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallPlanEntry {

    public enum Status {
        WILL_INSTALL,
        ALREADY_INSTALLED,
        MISSING
    }

    private String itemId;
    private String versionId;
    private String name;
    private String type;
    private String version;
    private Status status;
    private boolean root;
    private String errorMessage;

}
