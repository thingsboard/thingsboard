// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;

public class RPKLwM2MBootstrapServerCredential extends AbstractLwM2MBootstrapServerCredential {

    private static final long serialVersionUID = 6692464656059120166L;

    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.RPK;
    }
}
