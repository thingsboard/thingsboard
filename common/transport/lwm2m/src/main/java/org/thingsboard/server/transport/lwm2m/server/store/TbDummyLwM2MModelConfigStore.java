// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfig;

import java.util.Collections;
import java.util.List;

public class TbDummyLwM2MModelConfigStore implements TbLwM2MModelConfigStore {
    @Override
    public List<LwM2MModelConfig> getAll() {
        return Collections.emptyList();
    }

    @Override
    public void put(LwM2MModelConfig modelConfig) {

    }

    @Override
    public void remove(String endpoint) {

    }
}
