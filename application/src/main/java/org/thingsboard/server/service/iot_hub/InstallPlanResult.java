// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallPlanResult {

    private boolean success;
    private boolean rolledBack;
    private String errorMessage;
    private IotHubInstalledItemDescriptor rootDescriptor;
    private List<InstallPlanEntry> entries = new ArrayList<>();
    private List<String> missingItemIds = new ArrayList<>();

}
