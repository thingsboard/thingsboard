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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Builder;
import lombok.Data;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkRequest;

import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.DEFAULT_TIMEOUT;

@Data
public class ResultIds {
    @Builder.Default
    int objectId = -1;
    @Builder.Default
    int instanceId = -1;
    @Builder.Default
    int resourceId = -1;

    public ResultIds (String path) {
        String[] paths = path.split("/");
        if (paths != null && paths.length > 1) {
            this.objectId = (paths.length > 1) ? Integer.parseInt(paths[1]) :  this.objectId;
            this.instanceId = (paths.length > 2) ? Integer.parseInt(paths[2]) : this.instanceId;
            this.resourceId = (paths.length > 3) ? Integer.parseInt(paths[3]) : this.resourceId;
        }
    }
}
