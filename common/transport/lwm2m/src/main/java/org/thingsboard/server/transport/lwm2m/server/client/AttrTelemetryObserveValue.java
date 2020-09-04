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
package org.thingsboard.server.transport.lwm2m.server.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class AttrTelemetryObserveValue {
    /**
     * postAttribute, postTelemetry
     *  {{name:value},
     *  "Time":"23:00" }}
     */
    JsonObject postAttribute;
    JsonArray postAttributeProfile;
    JsonObject postTelemetry;
    JsonArray postTelemetryProfile;
    /**
     * postObserve
     * [ "/2/0/0", "/2/0/1"]
     */
    Set<String> postObserve;
    JsonArray postObserveProfile;

    /**
     * Only All resourses: postAttribute&postTelemetry
     */
    Set<String> pathResAttrTelemetry;
}
