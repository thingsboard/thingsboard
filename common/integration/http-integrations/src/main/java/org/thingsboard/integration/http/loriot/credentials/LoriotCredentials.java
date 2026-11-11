// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.loriot.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.web.client.RestTemplate;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LoriotBasicCredentials.class, name = "basic"),
        @JsonSubTypes.Type(value = LoriotTokenCredentials.class, name = "token")})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface LoriotCredentials {
    String AUTH_HEADER_PARAM = "Authorization";

    void setInterceptor(RestTemplate restTemplate, String baseUrl);
}
