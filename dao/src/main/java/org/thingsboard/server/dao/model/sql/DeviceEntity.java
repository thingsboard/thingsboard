// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.DEVICE_TABLE_NAME)
public final class DeviceEntity extends AbstractDeviceEntity<Device> {

    public DeviceEntity() {
        super();
    }

    public DeviceEntity(Device device) {
        super(device);
    }

    @Override
    public Device toData() {
        return super.toDevice();
    }
}
