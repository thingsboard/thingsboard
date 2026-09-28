// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.license.client.InstanceRegistry;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = ModelConstants.INSTANCE_REGISTRY_TABLE_NAME)
@NoArgsConstructor
public class InstanceRegistryEntity implements ToData<InstanceRegistry> {

    @Id
    @Column(name = ModelConstants.INSTANCE_REGISTRY_SERVICE_ID_PROPERTY)
    private String serviceId;
    @Column(name = ModelConstants.CREATED_TIME_PROPERTY)
    private long createdTime;
    @Column(name = ModelConstants.INSTANCE_REGISTRY_LAST_ACTIVITY_TS_PROPERTY)
    private long lastActivityTs;

    public InstanceRegistryEntity(InstanceRegistry instanceRegistry) {
        this.serviceId = instanceRegistry.getServiceId();
        this.createdTime = instanceRegistry.getCreatedTime();
        this.lastActivityTs = instanceRegistry.getLastActivityTs();
    }

    @Override
    public InstanceRegistry toData() {
        InstanceRegistry instanceRegistry = new InstanceRegistry();
        instanceRegistry.setServiceId(serviceId);
        instanceRegistry.setCreatedTime(createdTime);
        instanceRegistry.setLastActivityTs(lastActivityTs);
        return instanceRegistry;
    }

}
