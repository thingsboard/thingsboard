// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.coap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoapClientConfiguration {

    private CoapSecurityMode securityMode;

    @TemplateField(key = "coapEndpoint", label = "CoAP endpoint",
                   group = "CoAP Connection", required = true)
    private String coapEndpoint;

    private String dtlsCoapEndpoint;

}
