// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.WriteRequest.Mode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Data
public class ResourceValue {

    private LwM2mResource lwM2mResource;
    private ResourceModel resourceModel;

    public ResourceValue(LwM2mResource lwM2mResource, ResourceModel resourceModel) {
        this.resourceModel = resourceModel;
        updateLwM2mResource(lwM2mResource, Mode.UPDATE);
    }

    public void updateLwM2mResource(LwM2mResource lwM2mResource, Mode mode) {
        if (lwM2mResource instanceof LwM2mSingleResource) {
            this.lwM2mResource = LwM2mSingleResource.newResource(lwM2mResource.getId(), lwM2mResource.getValue(), lwM2mResource.getType());
        } else if (lwM2mResource instanceof LwM2mMultipleResource) {
            if (lwM2mResource.getInstances().values().size() > 0) {
                Set<LwM2mResourceInstance> instancesSet = new HashSet<>(lwM2mResource.getInstances().values());
                if (Mode.REPLACE.equals(mode) && this.lwM2mResource != null) {
                    Map<Integer, LwM2mResourceInstance> oldInstances = this.lwM2mResource.getInstances();
                    oldInstances.values().forEach(v -> {
                        if (instancesSet.stream().noneMatch(vIns -> v.getId() == vIns.getId())) {
                            instancesSet.add(v);
                        }
                    });
                }
                LwM2mResourceInstance[] instances = instancesSet.toArray(new LwM2mResourceInstance[0]);
                this.lwM2mResource = new LwM2mMultipleResource(lwM2mResource.getId(), lwM2mResource.getType(), instances);
            }
        }
    }
}
