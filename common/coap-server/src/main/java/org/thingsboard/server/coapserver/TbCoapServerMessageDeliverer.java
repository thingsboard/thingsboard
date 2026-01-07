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
package org.thingsboard.server.coapserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.DelivererException;
import org.eclipse.californium.core.server.ServerMessageDeliverer;
import org.eclipse.californium.core.server.resources.Resource;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
public class TbCoapServerMessageDeliverer extends ServerMessageDeliverer {

    public TbCoapServerMessageDeliverer(Resource root) {
        super(root);
    }

    @Override
    protected Resource findResource(Exchange exchange) throws DelivererException {
        validateUriPath(exchange);
        return findResource(exchange.getRequest().getOptions().getUriPath());
    }

    private void validateUriPath(Exchange exchange) {
        OptionSet options = exchange.getRequest().getOptions();
        List<String> uriPathList = options.getUriPath();
        String path = toPath(uriPathList);
        if (path != null) {
            options.setUriPath(path);
            exchange.getRequest().setOptions(options);
        }
    }

    private String toPath(List<String> list) {
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