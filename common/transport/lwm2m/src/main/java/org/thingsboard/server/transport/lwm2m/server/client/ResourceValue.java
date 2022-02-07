/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.WriteRequest.Mode;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Data
public class ResourceValue implements Serializable {

    private static final long serialVersionUID = -228268906779089402L;

    private TbLwM2MResource lwM2mResource;
    private TbResourceModel resourceModel;

    public ResourceValue(LwM2mResource lwM2mResource, ResourceModel resourceModel) {
        this.resourceModel = toTbResourceModel(resourceModel);
        updateLwM2mResource(lwM2mResource, Mode.UPDATE);
    }

    public void updateLwM2mResource(LwM2mResource lwM2mResource, Mode mode) {
        if (lwM2mResource instanceof LwM2mSingleResource) {
            this.lwM2mResource = new TbLwM2MSingleResource(lwM2mResource.getId(), lwM2mResource.getValue(), lwM2mResource.getType());
        } else if (lwM2mResource instanceof LwM2mMultipleResource) {
            if (lwM2mResource.getInstances().values().size() > 0) {
                Set <TbLwM2MResourceInstance> instancesSet = lwM2mResource.getInstances().values().stream().map(ResourceValue::toTbLwM2MResourceInstance).collect(Collectors.toSet());
                if (Mode.REPLACE.equals(mode) && this.lwM2mResource != null) {
                    Map<Integer, LwM2mResourceInstance> oldInstances = this.lwM2mResource.getInstances();
                    oldInstances.values().forEach(v -> {
                       if (instancesSet.stream().noneMatch(vIns -> v.getId() == vIns.getId())){
                           instancesSet.add(toTbLwM2MResourceInstance(v));
                       }
                    });
                }
                TbLwM2MResourceInstance[] instances = instancesSet.toArray(new TbLwM2MResourceInstance[0]);
                this.lwM2mResource = new TbLwM2mMultipleResource(lwM2mResource.getId(), lwM2mResource.getType(), instances);
            }
        }
    }

    public void setResourceModel(ResourceModel resourceModel) {
        this.resourceModel = toTbResourceModel(resourceModel);
    }

    private static TbLwM2MResourceInstance toTbLwM2MResourceInstance(LwM2mResourceInstance instance) {
        return new TbLwM2MResourceInstance(instance.getId(), instance.getValue(), instance.getType());
    }

    private static TbResourceModel toTbResourceModel(ResourceModel resourceModel) {
        return new TbResourceModel(resourceModel.id, resourceModel.name, resourceModel.operations, resourceModel.multiple,
                resourceModel.mandatory, resourceModel.type, resourceModel.rangeEnumeration, resourceModel.units, resourceModel.description);
    }
}
