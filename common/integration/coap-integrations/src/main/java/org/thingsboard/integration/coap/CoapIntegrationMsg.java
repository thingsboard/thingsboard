// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.coap;

import lombok.Data;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.integration.api.data.ContentType;

@Data
public class CoapIntegrationMsg {

    private final CoapExchange exchange;

    public ContentType getContentType() {
        int code = exchange.getRequestOptions().getContentFormat();
        switch (code) {
            case MediaTypeRegistry.APPLICATION_JSON:
            case MediaTypeRegistry.APPLICATION_SENML_JSON:
            case MediaTypeRegistry.APPLICATION_VND_OMA_LWM2M_JSON:
                return ContentType.JSON;
            case MediaTypeRegistry.APPLICATION_VND_OMA_LWM2M_TLV:
            case MediaTypeRegistry.APPLICATION_XML:
            case MediaTypeRegistry.APPLICATION_XMPP_XML:
            case MediaTypeRegistry.TEXT_PLAIN:
                return ContentType.TEXT;
            default:
                return ContentType.BINARY;
        }
    }

    public byte[] getPayloadBytes() {
        return exchange.advanced().getRequest().getPayload();
    }

}
