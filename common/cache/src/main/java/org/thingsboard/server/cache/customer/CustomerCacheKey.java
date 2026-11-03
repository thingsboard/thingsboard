// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.customer;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode
@RequiredArgsConstructor
public class CustomerCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 5706958428811356925L;

    @NonNull
    private final TenantId tenantId;
    private final String title;

    @Override
    public String toString() {
        return tenantId.getId() + "_" + title;
    }

}
