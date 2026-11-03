// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface SimpleTbEntityService<T> {

    default T save(T entity) throws Exception {
        return save(entity, null);
    }

    T save(T entity, SecurityUser user) throws Exception;

    void delete(T entity, User user);

}
