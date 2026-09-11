// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.tenant;

import org.thingsboard.server.common.data.Tenant;

public interface TbTenantService {

    Tenant save(Tenant tenant) throws Exception;

    void delete(Tenant tenant) throws Exception;

}
