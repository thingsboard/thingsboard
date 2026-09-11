// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

public record InstallReport(String tenantHash, String userHash, String tbVersion, String edition) {
}
