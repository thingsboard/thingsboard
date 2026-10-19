// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Setter
public class DeviceTypeFilter implements EntityFilter {

    /**
     * Replaced by {@link DeviceTypeFilter#getDeviceTypes()} instead.
     */
    @Deprecated(since = "3.5", forRemoval = true)
    private String deviceType;

    private List<String> deviceTypes;

    public List<String> getDeviceTypes() {
        return !CollectionUtils.isEmpty(deviceTypes) ? deviceTypes : Collections.singletonList(deviceType);
    }

    @Getter
    private String deviceNameFilter;

    public DeviceTypeFilter(List<String> deviceTypes, String deviceNameFilter) {
        this.deviceTypes = deviceTypes;
        this.deviceNameFilter = deviceNameFilter;
    }

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.DEVICE_TYPE;
    }
}
