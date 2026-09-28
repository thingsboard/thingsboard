// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.USER_PG_HIBERNATE_TABLE_NAME)
public final class UserEntity extends AbstractUserEntity<User> {

    public UserEntity() {
        super();
    }

    public UserEntity(User user) {
        super(user);
    }

    @Override
    public User toData() {
        return super.toUser();
    }

}
