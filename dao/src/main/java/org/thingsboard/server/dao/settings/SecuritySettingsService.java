// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.settings;

import org.thingsboard.server.common.data.security.model.SecuritySettings;

public interface SecuritySettingsService {

    SecuritySettings getSecuritySettings();

    SecuritySettings saveSecuritySettings(SecuritySettings securitySettings);

}
