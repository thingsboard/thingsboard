// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class DashboardFields extends AbstractEntityFields {

    public DashboardFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version) {
        super(id, createdTime, tenantId, customerId, name, version);
    }
}
