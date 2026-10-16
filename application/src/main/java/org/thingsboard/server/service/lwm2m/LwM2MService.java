// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.lwm2m;

import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;

public interface LwM2MService {

    LwM2MServerSecurityConfigDefault getServerSecurityInfo(boolean bootstrapServer);

}
