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
package org.thingsboard.server.transport.lwm2m.server.json;

import org.eclipse.leshan.server.queue.PresenceService;
import org.eclipse.leshan.server.registration.Registration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class RegistrationSerializer implements JsonSerializer<Registration> {

    private final PresenceService presenceService;

    public RegistrationSerializer(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public JsonElement serialize(Registration src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("endpoint", src.getEndpoint());
        element.addProperty("registrationId", src.getId());
        element.add("registrationDate", context.serialize(src.getRegistrationDate()));
        element.add("lastUpdate", context.serialize(src.getLastUpdate()));
        element.addProperty("address", src.getAddress().getHostAddress() + ":" + src.getPort());
        element.addProperty("smsNumber", src.getSmsNumber());
        element.addProperty("lwM2mVersion", src.getLwM2mVersion());
        element.addProperty("lifetime", src.getLifeTimeInSec());
        element.addProperty("bindingMode", src.getBindingMode().toString());
        element.add("rootPath", context.serialize(src.getRootPath()));
        element.add("objectLinks", context.serialize(src.getSortedObjectLinks()));
        element.add("secure", context.serialize(src.getIdentity().isSecure()));
        element.add("additionalRegistrationAttributes", context.serialize(src.getAdditionalRegistrationAttributes()));

        if (src.usesQueueMode()) {
            element.add("sleeping", context.serialize(!presenceService.isClientAwake(src)));
        }

        return element;
    }
}

