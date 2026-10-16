// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.secure.credentials;

import lombok.Data;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;

@Data
public class LwM2MClientCredentials {
    private LwM2MClientCredential client;
    private LwM2MBootstrapConfig bootstrap;
}
