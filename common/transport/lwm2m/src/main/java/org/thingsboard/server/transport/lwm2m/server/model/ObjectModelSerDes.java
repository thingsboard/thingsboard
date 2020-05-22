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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.util.json.JsonException;
import org.eclipse.leshan.core.util.json.JsonSerDes;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class ObjectModelSerDes extends JsonSerDes<ObjectModel> {

    ResourceModelSerDes resourceModelSerDes = new ResourceModelSerDes();

    @Override
    public JsonObject jSerialize(ObjectModel m) {
        final JsonObject o = Json.object();
        o.add("name", m.name);
        o.add("id", m.id);
        o.add("instancetype", m.multiple ? "multiple" : "single");
        o.add("mandatory", m.mandatory);
        if (!ObjectModel.DEFAULT_VERSION.equals(m.version))
            o.add("version", m.version);
        o.add("description", m.description);

        // sort resources value
        List<ResourceModel> resourceSpecs = new ArrayList<>(m.resources.values());
        Collections.sort(resourceSpecs, new Comparator<ResourceModel>() {
            @Override
            public int compare(ResourceModel r1, ResourceModel r2) {
                return r1.id - r2.id;
            }
        });

        JsonArray rs = new JsonArray();
        for (ResourceModel rm : resourceSpecs) {
            rs.add(resourceModelSerDes.jSerialize(rm));
        }
        o.add("resourcedefs", rs);

        return o;
    }

    @Override
    public ObjectModel deserialize(JsonObject o) throws JsonException {
        if (o == null)
            return null;

        if (!o.isObject())
            return null;

        int id = o.getInt("id", -1);
        if (id < 0)
            return null;

        String name = o.getString("name", null);
        String instancetype = o.getString("instancetype", null);
        boolean mandatory = o.getBoolean("mandatory", false);
        String description = o.getString("description", null);
        String version = o.getString("version", ObjectModel.DEFAULT_VERSION);
        List<ResourceModel> resourceSpecs = resourceModelSerDes.deserialize(o.get("resourcedefs").asArray());

        return new ObjectModel(id, name, description, version, "multiple".equals(instancetype), mandatory,
                resourceSpecs);
    }
}
