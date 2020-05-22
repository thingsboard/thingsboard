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
package org.thingsboard.server.transport.lwm2m.server.model;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.json.JsonSerDes;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class ResourceModelSerDes extends JsonSerDes<ResourceModel> {

    @Override
    public JsonObject jSerialize(ResourceModel m) {
        final JsonObject o = Json.object();
        o.add("id", m.id);
        o.add("name", m.name);
        o.add("operations", m.operations.toString());
        o.add("instancetype", m.multiple ? "multiple" : "single");
        o.add("mandatory", m.mandatory);
        o.add("type", m.type == null ? "none" : m.type.toString().toLowerCase());
        o.add("range", m.rangeEnumeration);
        o.add("units", m.units);
        o.add("description", m.description);
        return o;
    }

    @Override
    public ResourceModel deserialize(JsonObject o) {
        if (o == null)
            return null;

        if (!o.isObject())
            return null;

        int id = o.getInt("id", -1);
        if (id < 0)
            return null;

        String name = o.getString("name", null);
        Operations operations = Operations.valueOf(o.getString("operations", null));
        String instancetype = o.getString("instancetype", null);
        boolean mandatory = o.getBoolean("mandatory", false);
        Type type = Type.valueOf(o.getString("type", "").toUpperCase());
        String range = o.getString("range", null);
        String units = o.getString("units", null);
        String description = o.getString("description", null);

        return new ResourceModel(id, name, operations, "multiple".equals(instancetype), mandatory, type, range, units,
                description);
    }
}
