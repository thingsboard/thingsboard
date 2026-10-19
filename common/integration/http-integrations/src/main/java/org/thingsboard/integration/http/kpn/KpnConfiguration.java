// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.kpn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpnConfiguration {

    @TemplateField(key = "kpnDestinationSharedSecret", label = "Destination shared secret",
                   type = FormFieldType.PASSWORD, secret = true, group = "KPN Connection")
    private String destinationSharedSecret;

    private Boolean enableSecurity;

    @TemplateField(key = "kpnApiId", label = "API ID",
                   secret = true, group = "KPN Connection", required = true)
    private String apiId;

    @TemplateField(key = "kpnApiKey", label = "API key",
                   type = FormFieldType.PASSWORD, secret = true, group = "KPN Connection", required = true)
    private String apiKey;
}
