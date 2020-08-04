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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ModelObject {
    private Map<Integer, LwM2mObjectInstance> instances;
    private Integer id;
    private ObjectModel objectModel;
    private Set<String> objectObserves;


    public ModelObject(Integer id, Map<Integer, LwM2mObjectInstance> instances, ObjectModel objectModel, HashSet<String> objectObserves) {
        this.id = id;
        this.instances = instances;
        this.objectModel = objectModel;
        this.objectObserves = (objectObserves != null && objectObserves.size() > 0) ? objectObserves : new HashSet<String>();
    }
}
