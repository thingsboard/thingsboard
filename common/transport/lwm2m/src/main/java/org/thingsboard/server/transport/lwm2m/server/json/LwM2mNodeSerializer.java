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

import java.lang.reflect.Type;
import java.util.Map.Entry;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.util.Hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LwM2mNodeSerializer implements JsonSerializer<LwM2mNode> {

    @Override
    public JsonElement serialize(LwM2mNode src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("id", src.getId());

        if (typeOfSrc == LwM2mObject.class) {
            element.add("instances", context.serialize(((LwM2mObject) src).getInstances().values()));
        } else if (typeOfSrc == LwM2mObjectInstance.class) {
            element.add("resources", context.serialize(((LwM2mObjectInstance) src).getResources().values()));
        } else if (LwM2mResource.class.isAssignableFrom((Class<?>) typeOfSrc)) {
            LwM2mResource rsc = (LwM2mResource) src;
            if (rsc.isMultiInstances()) {
                JsonObject values = new JsonObject();
                for (Entry<Integer, ?> entry : rsc.getValues().entrySet()) {
                    if (rsc.getType() == org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE) {
                        values.addProperty(entry.getKey().toString(),
                                new String(Hex.encodeHex((byte[]) entry.getValue())));
                    } else {
                        values.add(entry.getKey().toString(), context.serialize(entry.getValue()));
                    }
                }
                element.add("values", values);
            } else {
                if (rsc.getType() == org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE) {
                    element.addProperty("value", new String(Hex.encodeHex((byte[]) rsc.getValue())));
                } else {
                    element.add("value", context.serialize(rsc.getValue()));
                }
            }
        }

        return element;
    }
}

