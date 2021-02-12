/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class LwM2MClientProfile {
    /**
     * {"clientLwM2mSettings": {
     *      clientUpdateValueAfterConnect: false;
     *       }
    **/
    JsonObject postClientLwM2mSettings;

    /**
     * {"keyName": {
     *       "/3/0/1": "modelNumber",
     *       "/3/0/0": "manufacturer",
     *       "/3/0/2": "serialNumber"
     *       }
    **/
    JsonObject postKeyNameProfile;

    /**
     * [ "/2/0/0", "/2/0/1"]
     */
    JsonArray postAttributeProfile;

    /**
     * [ "/2/0/0", "/2/0/1"]
     */
    JsonArray postTelemetryProfile;

    /**
     * [ "/2/0/0", "/2/0/1"]
     */
    JsonArray postObserveProfile;
}
