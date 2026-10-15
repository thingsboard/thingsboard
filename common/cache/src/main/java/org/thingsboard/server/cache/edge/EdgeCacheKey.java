// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.edge;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class EdgeCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = -2299543993746815287L;

    private final TenantId tenantId;
    private final String name;

    @Override
    public String toString() {
        return tenantId + "_" + name;
    }

}
