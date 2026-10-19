// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Collections;
import java.util.List;

public interface SimpleTbEntityService<T> {

    default T save(T entity, EntityGroup entityGroup) throws Exception {
        return save(entity, entityGroup, null);
    }

    default T save(T entity, EntityGroup entityGroup, SecurityUser user) throws Exception {
        return save(entity, entityGroup != null ? Collections.singletonList(entityGroup) : null, user);
    }

    T save(T entity, List<EntityGroup> entityGroups, SecurityUser user) throws Exception;

    void delete(T entity, User user);

}
