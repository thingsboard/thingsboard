/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.ProvisionRequestValidationStrategyType;

import java.util.Objects;

@Data
public class ProvisionDeviceConfiguration implements DeviceConfiguration {

    private String provisionDeviceKey;
    private String provisionDeviceSecret;

    @Override
    public DeviceProfileType getType() {
        return DeviceProfileType.PROVISION;
    }

    @JsonCreator
    public ProvisionDeviceConfiguration(@JsonProperty("provisionDeviceKey") String provisionProfileKey, @JsonProperty("provisionDeviceSecret") String provisionProfileSecret) {
        this.provisionDeviceKey = provisionProfileKey;
        this.provisionDeviceSecret = provisionProfileSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProvisionDeviceConfiguration that = (ProvisionDeviceConfiguration) o;
        return provisionDeviceKey.equals(that.provisionDeviceKey) &&
                provisionDeviceSecret.equals(that.provisionDeviceSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provisionDeviceKey, provisionDeviceSecret);
    }
}
