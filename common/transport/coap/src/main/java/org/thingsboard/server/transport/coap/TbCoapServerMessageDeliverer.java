/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.ServerMessageDeliverer;
import org.eclipse.californium.core.server.resources.Resource;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
public class TbCoapServerMessageDeliverer extends ServerMessageDeliverer {

    public TbCoapServerMessageDeliverer(Resource root) {
        super(root);
    }

    @Override
    public void deliverRequest(Exchange exchange) {
        if (exchange == null) {
            throw new NullPointerException("exchange must not be null");
        }
        boolean processed = preDeliverRequest(exchange);
        if (!processed) {
            OptionSet options = exchange.getRequest().getOptions();
            List<String> uriPath = options.getUriPath();
            String path = validateUriPath(uriPath);
            if (path != null) {
                options.setUriPath(path);
                exchange.getRequest().setOptions(options);
            }
            final Resource resource = findResource(exchange);
            if (resource != null) {
                checkForObserveOption(exchange, resource);

                // Get the executor and let it process the request
                Executor executor = resource.getExecutor();
                if (executor != null) {
                    executor.execute(() -> resource.handleRequest(exchange));
                } else {
                    resource.handleRequest(exchange);
                }
            } else {
                if (log.isInfoEnabled()) {
                    Request request = exchange.getRequest();
                    log.info("did not find resource /{} requested by {}", request.getOptions().getUriPathString(),
                            request.getSourceContext().getPeerAddress());
                }
                exchange.sendResponse(new Response(CoAP.ResponseCode.NOT_FOUND));
            }
        }
    }

    private String validateUriPath(List<String> list) {
        if (!CollectionUtils.isEmpty(list) && list.size() == 1) {
            final String slash = "/";
            String path = list.get(0);
            if (path.startsWith(slash)) {
                path = path.substring(slash.length());
            }
            return path;
        }
        return null;
    }

}