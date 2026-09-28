// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.EntityInfosConverter;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.USER_INFO_VIEW_TABLE_NAME)
public class UserInfoEntity extends AbstractUserEntity<UserInfo> {

    @Column(name = ModelConstants.OWNER_NAME_COLUMN)
    private String ownerName;

    @Convert(converter = EntityInfosConverter.class)
    @Column(name = ModelConstants.GROUPS_COLUMN)
    private List<EntityInfo> groups;

    public UserInfoEntity() {
        super();
    }

    @Override
    public UserInfo toData() {
        return new UserInfo(super.toUser(), this.ownerName, this.groups);
    }

}
