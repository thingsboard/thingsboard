// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;

public class TbDummyLwM2MClientOtaInfoStore implements TbLwM2MClientOtaInfoStore {

    @Override
    public LwM2MClientFwOtaInfo getFw(String endpoint) {
        return null;
    }

    @Override
    public LwM2MClientSwOtaInfo getSw(String endpoint) {
        return null;
    }

    @Override
    public void putFw(LwM2MClientFwOtaInfo info) {

    }

    @Override
    public void putSw(LwM2MClientSwOtaInfo info) {

    }
}
