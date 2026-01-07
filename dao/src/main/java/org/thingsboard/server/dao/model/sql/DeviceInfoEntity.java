/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.DEVICE_INFO_VIEW_TABLE_NAME)
public class DeviceInfoEntity extends AbstractDeviceEntity<DeviceInfo> {

    @Column(name = ModelConstants.DEVICE_CUSTOMER_TITLE_PROPERTY)
    private String customerTitle;
    @Column(name = ModelConstants.DEVICE_CUSTOMER_IS_PUBLIC_PROPERTY)
    private boolean customerIsPublic;
    @Column(name = ModelConstants.DEVICE_DEVICE_PROFILE_NAME_PROPERTY)
    private String deviceProfileName;
    @Column(name = ModelConstants.DEVICE_ACTIVE_PROPERTY)
    private boolean active;

    public DeviceInfoEntity() {
        super();
    }


    @Override
    public DeviceInfo toData() {
        return new DeviceInfo(super.toDevice(), customerTitle, customerIsPublic, deviceProfileName, active);
    }

}
