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
package org.thingsboard.server.service.iot_hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemVersionResult {

    private boolean success;
    private boolean entityModified;
    private String errorMessage;
    private IotHubInstalledItemDescriptor descriptor;

    public static UpdateItemVersionResult success(IotHubInstalledItemDescriptor descriptor) {
        return new UpdateItemVersionResult(true, false, null, descriptor);
    }

    public static UpdateItemVersionResult entityModified() {
        return new UpdateItemVersionResult(false, true, null, null);
    }

    public static UpdateItemVersionResult error(String errorMessage) {
        return new UpdateItemVersionResult(false, false, errorMessage, null);
    }

}
