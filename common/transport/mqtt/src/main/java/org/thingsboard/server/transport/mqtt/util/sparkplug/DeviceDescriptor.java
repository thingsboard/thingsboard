/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

public class DeviceDescriptor extends EdgeNodeDescriptor {

    private final String deviceId;
    private final String descriptorString;

    public DeviceDescriptor(String groupId, String edgeNodeId, String deviceId) {
        super(groupId, edgeNodeId);
        this.deviceId = deviceId;
        this.descriptorString = groupId + "/" + edgeNodeId + "/" + deviceId;
    }

    public DeviceDescriptor(String descriptorString) {
        super(descriptorString.substring(0, descriptorString.lastIndexOf("/")));
        this.deviceId = descriptorString.substring(descriptorString.lastIndexOf("/") + 1);
        this.descriptorString = descriptorString;
    }

    public DeviceDescriptor(EdgeNodeDescriptor edgeNodeDescriptor, String deviceId) {
        super(edgeNodeDescriptor.getGroupId(), edgeNodeDescriptor.getEdgeNodeId());
        this.deviceId = deviceId;
        this.descriptorString = edgeNodeDescriptor.getDescriptorString() + "/" + deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Returns a {@link String} representing the Device's Descriptor of the form:
     * "<groupName>/<edgeNodeName>/<deviceId>".
     *
     * @return a {@link String} representing the Device's Descriptor.
     */
    @Override
    public String getDescriptorString() {
        return descriptorString;
    }

    public String getEdgeNodeDescriptorString() {
        return super.getDescriptorString();
    }

    @Override
    public int hashCode() {
        return this.getDescriptorString().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DeviceDescriptor) {
            return this.getDescriptorString().equals(((DeviceDescriptor) object).getDescriptorString());
        }
        return this.getDescriptorString().equals(object);
    }

    @Override
    public String toString() {
        return getDescriptorString();
    }
}
