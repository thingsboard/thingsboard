/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by nickAS21 on 12.12.22
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SparkplugTopic {

    /**
     * The Sparkplug namespace version.
     * For the Sparkplug™ B version of the specification, the UTF-8 string constant for the namespace element will be: “spBv1.0”
     */
    private String namespace;

    /**
     * The ID of the logical grouping of Edge of Network (EoN) Nodes and devices.
     */
    private String groupId;

    /**
     * The ID of the Edge of Network (EoN) Node.
     */
    private String edgeNodeId;

    /**
     * The ID of the device.
     */
    private String deviceId;

    /**
     * The message type.
     */
    private SparkplugMessageType type;

     /**
     * Constructor (device).
     *
     * @param namespace the namespace.
     * @param groupId the group ID.
     * @param edgeNodeId the edge node ID.
     * @param deviceId the device ID.
     * @param type the message type.
     */
    public SparkplugTopic(String namespace, String groupId, String edgeNodeId, String deviceId, SparkplugMessageType type) {
        super();
        this.namespace = namespace;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.deviceId = deviceId;
        this.type = type;
    }

    /**
     * Constructor (node).
     *
     * @param namespace the namespace.
     * @param groupId the group ID.
     * @param edgeNodeId the edge node ID.
     * @param type the message type.
     */
    public SparkplugTopic(String namespace, String groupId, String edgeNodeId, SparkplugMessageType type) {
        super();
        this.namespace = namespace;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.deviceId = null;
        this.type = type;
    }

    public SparkplugTopic(SparkplugTopic sparkplugTopic,  SparkplugMessageType type) {
        super();
        this.namespace = sparkplugTopic.namespace;
        this.groupId = sparkplugTopic.groupId;
        this.edgeNodeId = sparkplugTopic.edgeNodeId;
        this.deviceId = null;
        this.type = type;
    }
    public SparkplugTopic(SparkplugTopic sparkplugTopic,  SparkplugMessageType type, String deviceId) {
        super();
        this.namespace = sparkplugTopic.namespace;
        this.groupId = sparkplugTopic.groupId;
        this.edgeNodeId = sparkplugTopic.edgeNodeId;
        this.deviceId = deviceId;
        this.type = type;
    }

    /**
     * @return the Sparkplug namespace version
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the ID of the logical grouping of Edge of Network (EoN) Nodes and devices.
     *
     * @return the group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return  the ID of the Edge of Network (EoN) Node
     */
    public String getEdgeNodeId() {
        return edgeNodeId;
    }

    /**
     * @return the device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @return the message type
     */
    public SparkplugMessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getNamespace()).append("/")
                .append(getGroupId()).append("/")
                .append(getType()).append("/")
                .append(getEdgeNodeId());
        if (getDeviceId() != null) {
            sb.append("/").append(getDeviceId());
        }
        return sb.toString();
    }

    /**
     * @param type the type to check
     * @return true if this topic's type matches the passes in type, false otherwise
     */
    public boolean isType(SparkplugMessageType type) {
        return this.type != null && this.type.equals(type);
    }

    public boolean isNode() {
        return this.deviceId == null;
    }

    public String getNodeDeviceName() {
        return isNode() ? edgeNodeId : deviceId;
    }
}

