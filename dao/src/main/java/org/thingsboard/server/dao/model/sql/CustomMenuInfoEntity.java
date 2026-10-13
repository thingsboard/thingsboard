// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_TABLE_NAME;


@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CUSTOM_MENU_TABLE_NAME)
public final class CustomMenuInfoEntity extends AbstractCustomMenuEntity<CustomMenuInfo> {

    public CustomMenuInfoEntity() {
        super();
    }

    public CustomMenuInfoEntity(CustomMenuInfo customMenuInfo) {
        super(customMenuInfo);
    }

    @Override
    public CustomMenuInfo toData() {
        return super.toCustomMenuInfo();
    }

}
