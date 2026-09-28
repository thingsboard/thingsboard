// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.particle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticleConfiguration {
    private String httpEndpoint;
    private ParticleCredentials credentials;

    @TemplateField(key = "particleAllowDownlink", label = "Allow downlink commands",
                   type = FormFieldType.BOOLEAN, group = "Particle Connection")
    private boolean allowDownlink;
    private boolean enableSecurity;
    private Map<String, String> headersFilter;
}
