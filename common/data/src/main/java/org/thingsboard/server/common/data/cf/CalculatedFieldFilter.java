/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf;

import lombok.Builder;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class CalculatedFieldFilter {

    @NonNull
    private final CalculatedFieldType type;
    @NonNull
    private final Set<EntityType> entityTypes;
    @Nullable
    private final List<UUID> entityIds;
    @Nullable
    private final String name;

}
