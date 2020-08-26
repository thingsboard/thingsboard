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

import lombok.Data;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import java.util.Map;


@Data
public class ModelObject {
    /**
     * model one on all instance
     * for each instance only id resource with parameters of resources (observe, attr, telemetry)
     */
    private ObjectModel objectModel;
    private Map<Integer, LwM2mObjectInstance> instances;

     public ModelObject(ObjectModel objectModel, Map<Integer, LwM2mObjectInstance> instances) {
        this.objectModel = objectModel;
        this.instances = instances;
    }

    public boolean removeInstance (int id ) {
        LwM2mObjectInstance instance = this.instances.get(id);
         return this.instances.remove(id, instance);
    }
}
