// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.exception;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;

public class TenantProfileNotFoundException extends RuntimeException {

    @Getter
    private final TenantId tenantId;

    public TenantProfileNotFoundException(TenantId tenantId) {
        super("Profile for tenant with id " + tenantId + " not found");
        this.tenantId = tenantId;
    }

}
