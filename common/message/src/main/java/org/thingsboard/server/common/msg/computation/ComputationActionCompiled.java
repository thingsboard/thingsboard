/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.computation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;

@ToString
public class ComputationActionCompiled implements ComputationMsg{

    private final String actionClazz;
    private final String descriptor;
    private final String name;
    private final JsonNode configurationDescriptor;

    public ComputationActionCompiled(String actionClazz,
                                     String descriptor,
                                     String name,
                                     JsonNode configurationDescriptor) {
        this.actionClazz = actionClazz;
        this.descriptor = descriptor;
        this.name = name;
        this.configurationDescriptor = configurationDescriptor;
    }

    public String getActionClazz() {
        return actionClazz;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return name;
    }

    public JsonNode getConfigurationDescriptor() {
        return configurationDescriptor;
    }
}
