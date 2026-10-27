// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.component;

import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

public interface ComponentDescriptorInsertRepository {

    ComponentDescriptorEntity saveOrUpdate(ComponentDescriptorEntity entity);

}
