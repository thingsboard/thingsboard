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
package org.thingsboard.server.dao.timeseries;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serial;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class TsLatestCacheKey implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 2024369077925351881L;

    private final EntityId entityId;
    private final String key;

    @Override
    public String toString() {
        return "{" + entityId + "}" + key;
    }

    @Override
    public boolean isVersioned() {
        return true;
    }

}
