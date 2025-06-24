/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;

import java.util.HashMap;
import java.util.Map;

@Data
public class Lwm2mDeviceTransportConfiguration extends PowerSavingConfiguration implements DeviceTransportConfiguration {

    @JsonIgnore
    private Map<String, Object> properties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> properties() {
        return this.properties;
    }

    @JsonAnySetter
    public void put(String name, Object value) {
        this.properties.put(name, value);
    }

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.LWM2M;
    }

}
