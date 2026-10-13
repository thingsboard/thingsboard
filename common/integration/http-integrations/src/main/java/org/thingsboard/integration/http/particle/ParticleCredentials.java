// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.particle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticleCredentials {

    @TemplateField(key = "particleAccessToken", label = "Particle access token",
                   type = FormFieldType.PASSWORD, secret = true,
                   group = "Particle Connection", required = true)
    private String token;

    public void setInterceptor(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            wrapper.getHeaders().setBearerAuth(token);
            return execution.execute(wrapper, body);
        });
    }
}
