// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataUtils {

    public static List<ObjectNode> getChildObjects(String propertyName, JsonNode configuration) {
        return Optional.ofNullable(configuration)
                .map(config -> config.get(propertyName))
                .filter(node -> !node.isEmpty() && (node.isObject() || node.isArray()))
                .map(node -> Streams.stream(node.elements())
                        .filter(JsonNode::isObject)
                        .map(jsonNode -> (ObjectNode) jsonNode)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

}
