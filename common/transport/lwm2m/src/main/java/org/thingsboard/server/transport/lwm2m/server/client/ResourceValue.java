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

import lombok.Data;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;

import java.io.Serializable;

@Data
public class ResourceValue implements Serializable {

    private static final long serialVersionUID = -228268906779089402L;

    private TbLwM2MResource lwM2mResource;
    private TbResourceModel resourceModel;

    public ResourceValue(LwM2mResource lwM2mResource, ResourceModel resourceModel) {
        this.lwM2mResource = toTbLwM2MResource(lwM2mResource);
        this.resourceModel = toTbResourceModel(resourceModel);
    }

    public void setLwM2mResource(LwM2mResource lwM2mResource) {
        this.lwM2mResource = toTbLwM2MResource(lwM2mResource);
    }

    public void setResourceModel(ResourceModel resourceModel) {
        this.resourceModel = toTbResourceModel(resourceModel);
    }

    private static TbLwM2MResource toTbLwM2MResource(LwM2mResource lwM2mResource) {
        if (lwM2mResource.isMultiInstances()) {
            TbLwM2MResourceInstance[] instances = (TbLwM2MResourceInstance[]) lwM2mResource.getInstances().values().stream().map(ResourceValue::toTbLwM2MResourceInstance).toArray();
            return new TbLwM2MMultipleResource(lwM2mResource.getId(), lwM2mResource.getType(), instances);
        } else {
            return new TbLwM2MSingleResource(lwM2mResource.getId(), lwM2mResource.getValue(), lwM2mResource.getType());
        }
    }

    private static TbLwM2MResourceInstance toTbLwM2MResourceInstance(LwM2mResourceInstance instance) {
        return new TbLwM2MResourceInstance(instance.getId(), instance.getValue(), instance.getType());
    }

    private static TbResourceModel toTbResourceModel(ResourceModel resourceModel) {
        return new TbResourceModel(resourceModel.id, resourceModel.name, resourceModel.operations, resourceModel.multiple,
                resourceModel.mandatory, resourceModel.type, resourceModel.rangeEnumeration, resourceModel.units, resourceModel.description);
    }
}
