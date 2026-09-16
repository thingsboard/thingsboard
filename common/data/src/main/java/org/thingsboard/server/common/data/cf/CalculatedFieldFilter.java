// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf;

import lombok.Builder;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.server.common.data.EntityType;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class CalculatedFieldFilter {

    @NonNull
    private final Set<CalculatedFieldType> types;
    @NonNull
    private final Set<EntityType> entityTypes;
    @Nullable
    private final Set<UUID> entityIds;
    @Nullable
    private final Set<String> names;

}
