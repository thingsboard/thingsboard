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
package org.thingsboard.server.transport.coap.efento.adaptor;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class EfentoCoapAdaptor {

    private static final Gson gson = new Gson();

    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, List<CoapEfentoTransportResource.EfentoMeasurements> measurements) throws AdaptorException {
        try {
            return JsonConverter.convertToTelemetryProto(gson.toJsonTree(measurements));
        } catch (Exception ex) {
            log.warn("[{}] Failed to convert EfentoMeasurements to PostTelemetry request!", sessionId);
            throw new AdaptorException(ex);
        }
    }
}
