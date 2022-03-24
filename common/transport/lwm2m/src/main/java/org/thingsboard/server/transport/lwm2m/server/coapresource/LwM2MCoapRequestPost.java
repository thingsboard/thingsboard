/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.coapresource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.node.LwM2mPath;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Data
public class LwM2MCoapRequestPost {

    CoapExchange exchange;
    List<String> uriPath;
    String endpoint;
    LwM2mPath lwM2mPath;
    String payLoadResponse;

    public boolean validateParamsRequest() {
        try {
            this.endpoint = uriPath.get(1);
            List<String> lwM2mPathList =
                    IntStream.range(2, uriPath.size())
                            .mapToObj(uriPath::get)
                            .collect(Collectors.toList());
            String lwM2mPathStr = StringUtils.join(lwM2mPathList, "/");
            this.lwM2mPath = new LwM2mPath(lwM2mPathStr);
            if (exchange.getRequestPayload() != null && StringUtils.isNotEmpty(endpoint)) {
                if (this.lwM2mPath.isResourceInstance() || this.lwM2mPath.isResource()) {
                    return true;
                } else {
                    this.payLoadResponse = String.format("Invalid LwM2mPath resource, must be LwM2mResourceInstance or  LwM2mSingleResource. Lwm2m coap Post request: %s", uriPath);
                    log.error(payLoadResponse);
                }
            } else {
                this.payLoadResponse = String.format("Invalid Endpoint or PayLoadRequest. Lwm2m coap Post request: %s", uriPath);
                log.error(payLoadResponse);
            }
        } catch (Exception e) {
            this.payLoadResponse = String.format("Invalid Lwm2m coap Post request: [%s]", e.getMessage());
            log.error(payLoadResponse);
        }
        return false;
    }
}
