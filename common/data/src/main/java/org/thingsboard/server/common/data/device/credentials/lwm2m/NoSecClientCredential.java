// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials.lwm2m;

public class NoSecClientCredential extends AbstractLwM2MClientCredential {

    @Override
    public LwM2MSecurityMode getSecurityConfigClientMode() {
        return LwM2MSecurityMode.NO_SEC;
    }
}
