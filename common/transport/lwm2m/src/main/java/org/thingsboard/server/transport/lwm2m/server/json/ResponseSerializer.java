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

import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class ResponseSerializer implements JsonSerializer<LwM2mResponse> {

    @Override
    public JsonElement serialize(LwM2mResponse src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("status", src.getCode().toString());
        element.addProperty("valid", src.isValid());
        element.addProperty("success", src.isSuccess());
        element.addProperty("failure", src.isFailure());

        if (typeOfSrc instanceof Class<?>) {
            if (ReadResponse.class.isAssignableFrom((Class<?>) typeOfSrc)) {
                element.add("content", context.serialize(((ReadResponse) src).getContent()));
            } else if (DiscoverResponse.class.isAssignableFrom((Class<?>) typeOfSrc)) {
                element.add("objectLinks", context.serialize(((DiscoverResponse) src).getObjectLinks()));
            } else if (CreateResponse.class.isAssignableFrom((Class<?>) typeOfSrc)) {
                element.add("location", context.serialize(((CreateResponse) src).getLocation()));
            }
        }
        if (src.isFailure() && src.getErrorMessage() != null && !src.getErrorMessage().isEmpty())
            element.addProperty("errormessage", src.getErrorMessage());

        return element;
    }
}

