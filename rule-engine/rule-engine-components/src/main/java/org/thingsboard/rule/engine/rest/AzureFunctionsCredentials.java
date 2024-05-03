package org.thingsboard.rule.engine.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureFunctionsCredentials implements ClientCredentials {
    private String accessKey;

    @Override
    public CredentialsType getType() {
        return CredentialsType.ACCESS_KEY;
    }
}
