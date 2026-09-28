// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public class EventUtil {

    public static JsonNode toBodyJson(String serviceId, ComponentLifecycleEvent event, Optional<Exception> e) {
        ObjectNode node = JacksonUtil.newObjectNode().put("server", serviceId).put("event", event.name());
        if (e.isPresent()) {
            node = node.put("success", false);
            node = node.put("error", toString(e.get()));
        } else {
            node = node.put("success", true);
        }
        return node;
    }

    public static JsonNode toBodyJson(String serviceId, long messagesProcessed, long errorsOccurred) {
        return JacksonUtil.newObjectNode().put("server", serviceId).put("messagesProcessed", messagesProcessed).put("errorsOccurred", errorsOccurred);
    }

    public static String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
