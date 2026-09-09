// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;

import java.util.Map;

@Data
public class ModelObject  implements Cloneable {
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

    public ModelObject clone() throws CloneNotSupportedException {
        return (ModelObject) super.clone();
    }
}
