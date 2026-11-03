// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.cf;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbCalculatedFieldService {

    CalculatedField save(CalculatedField calculatedField, SecurityUser user) throws ThingsboardException;

    CalculatedField findById(CalculatedFieldId calculatedFieldId, SecurityUser user);

    PageData<CalculatedField> findAllByTenantIdAndEntityId(EntityId entityId, SecurityUser user, PageLink pageLink);

    void delete(CalculatedField calculatedField, User user);

}
