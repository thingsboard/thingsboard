/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.adaptors;

import org.eclipse.californium.core.coap.Request;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
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
            Set<String> clientKeys = toKeys(queryElements, "clientKeys");
            Set<String> sharedKeys = toKeys(queryElements, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
        }
        return result.build();
    }

    private static Set<String> toKeys(List<String> queryElements, String attributeName) throws AdaptorException {
        String keys = null;
        for (String queryElement : queryElements) {
            String[] queryItem = queryElement.split("=");
            if (queryItem.length == 2 && queryItem[0].equals(attributeName)) {
                keys = queryItem[1];
            }
        }
        if (keys != null && !StringUtils.isEmpty(keys)) {
            return new HashSet<>(Arrays.asList(keys.split(",")));
        } else {
            return null;
        }
    }
}
