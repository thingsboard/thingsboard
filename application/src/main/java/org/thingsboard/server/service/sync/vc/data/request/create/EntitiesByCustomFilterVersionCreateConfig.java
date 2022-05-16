/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.vc.data.request.create;

import lombok.Data;
import org.thingsboard.server.common.data.query.EntityFilter;

import java.util.UUID;

@Data
public class EntitiesByCustomFilterVersionCreateConfig extends VersionCreateConfig {

    private EntityFilter filter;
    private UUID customerId;

    private boolean removeOtherRemoteEntitiesOfType;

    @Override
    public VersionCreateConfigType getType() {
        return VersionCreateConfigType.CUSTOM_ENTITY_FILTER;
    }

}
