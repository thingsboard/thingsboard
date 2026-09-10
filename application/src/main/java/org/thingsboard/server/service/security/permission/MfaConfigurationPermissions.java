// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.permission;

import org.springframework.stereotype.Component;

@Component
public class MfaConfigurationPermissions extends AbstractPermissions {

    public MfaConfigurationPermissions() {
        super();
        // for compatibility with PE
    }

}
