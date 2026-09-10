// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;

public interface TbLwM2MClientOtaInfoStore {

    LwM2MClientFwOtaInfo getFw(String endpoint);

    LwM2MClientSwOtaInfo getSw(String endpoint);

    void putFw(LwM2MClientFwOtaInfo info);

    void putSw(LwM2MClientSwOtaInfo info);
}
