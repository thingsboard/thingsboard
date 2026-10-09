// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.coap.adaptors;

import org.eclipse.californium.core.coap.Request;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoapAdaptorUtils {

    public static TransportProtos.GetAttributeRequestMsg toGetAttributeRequestMsg(Request inbound) throws AdaptorException {
        List<String> queryElements = inbound.getOptions().getUriQuery();
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        if (queryElements != null && queryElements.size() > 0) {
            boolean allClient = "true".equalsIgnoreCase(getQueryValue(queryElements, "allClientKeys"));
            boolean allShared = "true".equalsIgnoreCase(getQueryValue(queryElements, "allSharedKeys"));
            Set<String> clientKeys = allClient ? null : toKeys(queryElements, "clientKeys");
            Set<String> sharedKeys = allShared ? null : toKeys(queryElements, "sharedKeys");
            JsonConverter.applyClientScope(result, allClient, clientKeys);
            JsonConverter.applySharedScope(result, allShared, sharedKeys);
        }
        result.setOnlyShared(false);
        return result.build();
    }

    private static String getQueryValue(List<String> queryElements, String name) {
        // Keep the last matching occurrence to preserve the original toKeys() behavior.
        String value = null;
        for (String queryElement : queryElements) {
            String[] queryItem = queryElement.split("=");
            if (queryItem.length == 2 && queryItem[0].equals(name)) {
                value = queryItem[1];
            }
        }
        return value;
    }

    private static Set<String> toKeys(List<String> queryElements, String attributeName) {
        String keys = getQueryValue(queryElements, attributeName);
        if (!StringUtils.isEmpty(keys)) {
            return new HashSet<>(Arrays.asList(keys.split(",")));
        } else {
            return null;
        }
    }
}
