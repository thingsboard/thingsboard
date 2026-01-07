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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class LwM2MLocationParams {

    private Float latitude;
    private Float longitude;
    private Float scaleFactor = 1.0F;
    private final String locationPos = "50.4501:30.5234";
    private final Float locationScaleFactor = 1.0F;


   protected void getPos() {
        this.latitude = null;
        this.longitude = null;
        int c = locationPos.indexOf(':');
        this.latitude = Float.valueOf(locationPos.substring(0, c));
        this.longitude = Float.valueOf(locationPos.substring(c + 1));
    }


}
