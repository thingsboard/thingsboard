/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.DEVICE_INFO_VIEW_COLUMN_FAMILY_NAME)
public class DeviceInfoEntity extends AbstractDeviceEntity<DeviceInfo> {

    public static final Map<String, String> attrActiveColumnMap = new HashMap<>();
    public static final Map<String, String> tsActiveColumnMap = new HashMap<>();
    static {
        attrActiveColumnMap.put("active", "attributeActive");
        tsActiveColumnMap.put("active", "tsActive");
    }

    @Column(name = ModelConstants.DEVICE_CUSTOMER_TITLE_PROPERTY)
    private String customerTitle;
    @Column(name = ModelConstants.DEVICE_CUSTOMER_IS_PUBLIC_PROPERTY)
    private Boolean customerIsPublic;
    @Column(name = ModelConstants.DEVICE_DEVICE_PROFILE_NAME_PROPERTY)
    private String deviceProfileName;
    @Column(name = ModelConstants.DEVICE_ATTR_ACTIVE_PROPERTY)
    private Boolean attributeActive;
    @Column(name = ModelConstants.DEVICE_TS_ACTIVE_PROPERTY)
    private Boolean tsActive;

    public DeviceInfoEntity() {
        super();
    }


    @Override
    public DeviceInfo toData() {
        return toData(false);
    }

    @Override
    public DeviceInfo toData(Object... persistToTelemetry) {
        boolean attr = persistToTelemetry.length == 0 || !(Boolean) persistToTelemetry[0];
        return new DeviceInfo(super.toDevice(), customerTitle, Boolean.TRUE.equals(customerIsPublic), deviceProfileName,
                attr ? Boolean.TRUE.equals(attributeActive) : Boolean.TRUE.equals(tsActive));
    }
}
