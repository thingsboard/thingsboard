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

import com.fasterxml.jackson.annotation.JsonValue;

public class EdgeNodeDescriptor implements SparkplugDescriptor{

    private final String groupId;
    private final String edgeNodeId;
    private final String descriptorString;

    public EdgeNodeDescriptor(String groupId, String edgeNodeId) {
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.descriptorString = groupId + "/" + edgeNodeId;
    }

    /**
     * Creates and EdgeNodeDescriptor from a {@link String} of the form group_name/edge_node_name
     *
     * @param descriptorString the {@link String} representation of an EdgeNodeDescriptor
     */
    public EdgeNodeDescriptor(String descriptorString) {
        String[] tokens = descriptorString.split("/");
        this.groupId = tokens[0];
        this.edgeNodeId = tokens[1];
        this.descriptorString = descriptorString;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getEdgeNodeId() {
        return edgeNodeId;
    }

    /**
     * Returns a {@link String} representing the Edge Node's Descriptor of the form: "<groupId>/<edgeNodeId>".
     *
     * @return a {@link String} representing the Edge Node's Descriptor.
     */
    @Override
    public String getDescriptorString() {
        return descriptorString;
    }

    @Override
    public int hashCode() {
        return this.getDescriptorString().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof EdgeNodeDescriptor) {
            return this.getDescriptorString().equals(((EdgeNodeDescriptor) object).getDescriptorString());
        }
        return this.getDescriptorString().equals(object);
    }

    @Override
    @JsonValue
    public String toString() {
        return getDescriptorString();
    }
}
