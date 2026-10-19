// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonType;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.EntityInfosConverter;

import java.util.List;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.DEVICE_INFO_VIEW_TABLE_NAME)
public class DeviceInfoEntity extends AbstractDeviceEntity<DeviceInfo> {

    @Column(name = ModelConstants.OWNER_NAME_COLUMN)
    private String ownerName;

    @Convert(converter = EntityInfosConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonType.class)
    @Column(name = ModelConstants.GROUPS_COLUMN)
    private List<EntityInfo> groups;

    @Column(name = ModelConstants.DEVICE_ACTIVE_PROPERTY)
    private boolean active;

    public DeviceInfoEntity() {
        super();
    }

    @Override
    public DeviceInfo toData() {
        return new DeviceInfo(super.toDevice(), this.ownerName, this.groups, this.active);
    }

}
