package org.thingsboard.rule.engine.rest;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;

import java.util.Collections;
import java.util.Map;

@Data
public class TbAzureFunctionsNodeConfiguration extends TbRestApiCallNodeConfiguration {

    private Map<String, String> queryParams;
    private Map<String, String> inputKeys;

    @Override
    public TbAzureFunctionsNodeConfiguration defaultConfiguration() {
        TbAzureFunctionsNodeConfiguration configuration = new TbAzureFunctionsNodeConfiguration();
        configuration.setRestEndpointUrlPattern("http://localhost:<port>/api/<function-name>");
        configuration.setRequestMethod("POST");
        configuration.setHeaders(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        configuration.setQueryParams(Collections.emptyMap());
        configuration.setInputKeys(Collections.emptyMap());
        configuration.setCredentials(new AnonymousCredentials());
        return configuration;
    }
}
