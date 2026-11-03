// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import java.util.UUID;

public interface ExportableEntityRepository<D> {

    D findByTenantIdAndExternalId(UUID tenantId, UUID externalId);

}
