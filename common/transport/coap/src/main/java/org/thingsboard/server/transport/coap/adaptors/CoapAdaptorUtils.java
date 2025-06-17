/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CoapAdaptorUtils {

    private static final String ATTRIBUTES = "attributes";

    public static TransportProtos.GetAttributeRequestMsg toGetAttributeRequestMsg(Request inbound) throws AdaptorException {
        List<String> queryParameters = inbound.getOptions().getUriQuery();
        List<String> uriPaths = inbound.getOptions().getUriPath();

        String scope = extractScope(uriPaths);

        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        result.setAddClient(false);
        result.setAddShared(false);
        if (scope == null) {
            processNotScopedRequest(queryParameters, result);
        } else {
            processScopedRequest(scope, queryParameters, result);
        }

        result.setOnlyShared(false);
        return result.build();
    }

    private static String extractScope(List<String> uriPaths) {
        String lastPath = uriPaths.get(uriPaths.size() - 1);
        return ATTRIBUTES.equals(lastPath) ? null : lastPath;
    }

    private static void processNotScopedRequest(List<String> queryParameters, TransportProtos.GetAttributeRequestMsg.Builder result) {
        if (queryParameters != null && !queryParameters.isEmpty()) {
            result.setAddClient(false);
            result.setAddShared(false);
            Set<String> clientKeys = extractKeys(queryParameters, "clientKeys");
            Set<String> sharedKeys = extractKeys(queryParameters, "sharedKeys");

            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
                result.setAddClient(true);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
                result.setAddShared(true);
            }
        } else {
            result.setAddClient(true);
            result.setAddShared(true);
        }
    }

    private static void processScopedRequest(String scope, List<String> queryParameters, TransportProtos.GetAttributeRequestMsg.Builder result) throws AdaptorException {
        switch (scope) {
            case "client":
                result.setAddClient(true);
                addKeysFromQuery(queryParameters, result::addAllClientAttributeNames);
                break;
            case "shared":
                result.setAddShared(true);
                addKeysFromQuery(queryParameters, result::addAllSharedAttributeNames);
                break;
            default:
                throw new AdaptorException("Unsupported scope: " + scope);
        }
    }

    private static void addKeysFromQuery(List<String> queryParameters, Consumer<Collection<String>> keyConsumer) {
        if (queryParameters != null && !queryParameters.isEmpty()) {
            Set<String> keys = extractKeys(queryParameters, "keys");
            if (keys != null) {
                keyConsumer.accept(keys);
            }
        }
    }

    private static Set<String> extractKeys(List<String> queryParameters, String attributeName) {
        return queryParameters.stream()
                .filter(query -> isValidQueryItem(query, attributeName))
                .map(query -> query.split("=")[1])
                .filter(keys -> !StringUtils.isEmpty(keys))
                .findFirst()
                .map(keys -> new HashSet<>(Arrays.asList(keys.split(","))))
                .orElse(null);
    }

    private static boolean isValidQueryItem(String query, String attributeName) {
        String[] queryParts = query.split("=");
        return queryParts.length == 2 && queryParts[0].equals(attributeName);
    }
}
