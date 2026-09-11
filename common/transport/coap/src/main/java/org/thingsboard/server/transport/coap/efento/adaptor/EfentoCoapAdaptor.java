// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.efento.adaptor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class EfentoCoapAdaptor {

    private static final Gson gson = new Gson();

    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, List<CoapEfentoTransportResource.EfentoTelemetry> telemetryList) throws AdaptorException {
        try {
            return JsonConverter.convertToTelemetryProto(gson.toJsonTree(telemetryList));
        } catch (Exception ex) {
            log.warn("[{}] Failed to convert EfentoMeasurements to PostTelemetry request!", sessionId);
            throw new AdaptorException(ex);
        }
    }

    public TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, JsonElement deviceInfo) throws AdaptorException {
        try {
            return JsonConverter.convertToAttributesProto(deviceInfo);
        } catch (Exception ex) {
            log.warn("[{}] Failed to convert JsonObject to PostTelemetry request!", sessionId);
            throw new AdaptorException(ex);
        }
    }


}
