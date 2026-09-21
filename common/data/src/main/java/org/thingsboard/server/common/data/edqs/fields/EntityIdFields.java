// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@SuperBuilder
public class EntityIdFields implements EntityFields {

    private UUID id;
    private Long version;

    public EntityIdFields(UUID id, Long version) {
        this.id = id;
        this.version = version;
    }
}
