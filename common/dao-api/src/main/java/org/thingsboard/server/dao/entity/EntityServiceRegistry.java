// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity;

import org.thingsboard.server.common.data.EntityType;

public interface EntityServiceRegistry {

    EntityDaoService getServiceByEntityType(EntityType entityType);

}
