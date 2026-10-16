// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;

import java.io.Serial;

public class NoSecLwM2MBootstrapServerCredential extends AbstractLwM2MBootstrapServerCredential {

    @Serial
    private static final long serialVersionUID = 5540417758424747066L;

    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.NO_SEC;
    }
}
