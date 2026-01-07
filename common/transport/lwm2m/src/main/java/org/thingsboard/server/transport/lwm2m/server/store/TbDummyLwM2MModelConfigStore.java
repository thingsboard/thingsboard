/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
