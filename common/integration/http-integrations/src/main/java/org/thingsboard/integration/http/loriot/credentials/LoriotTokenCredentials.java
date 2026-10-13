// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.loriot.credentials;

import lombok.Data;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;

@Data
public class LoriotTokenCredentials implements LoriotCredentials {

    private String token;

    @Override
    public void setInterceptor(RestTemplate restTemplate, String baseUrl) {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            wrapper.getHeaders().setBearerAuth(token);
            return execution.execute(wrapper, body);
        });
    }
}
