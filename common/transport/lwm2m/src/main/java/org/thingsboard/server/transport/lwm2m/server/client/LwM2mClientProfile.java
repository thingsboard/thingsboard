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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class LwM2mClientProfile {
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

    public LwM2mClientProfile clone() {
        LwM2mClientProfile lwM2mClientProfile = new LwM2mClientProfile();
        lwM2mClientProfile.postClientLwM2mSettings = this.deepCopy(this.postClientLwM2mSettings, JsonObject.class);
        lwM2mClientProfile.postKeyNameProfile = this.deepCopy(this.postKeyNameProfile, JsonObject.class);
        lwM2mClientProfile.postAttributeProfile = this.deepCopy(this.postAttributeProfile, JsonArray.class);
        lwM2mClientProfile.postTelemetryProfile = this.deepCopy(this.postTelemetryProfile, JsonArray.class);
        lwM2mClientProfile.postObserveProfile = this.deepCopy(this.postObserveProfile, JsonArray.class);
        return lwM2mClientProfile;
    }


    private <T> T deepCopy(T elements, Class<T> type) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(gson.toJson(elements), type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
