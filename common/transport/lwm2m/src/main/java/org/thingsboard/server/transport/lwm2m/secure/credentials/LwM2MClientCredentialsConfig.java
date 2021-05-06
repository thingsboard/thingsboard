package org.thingsboard.server.transport.lwm2m.secure.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.leshan.core.SecurityMode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "securityConfigClientMode")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoSecClientCredentialsConfig.class, name = "NO_SEC"),
        @JsonSubTypes.Type(value = PSKClientCredentialsConfig.class, name = "PSK"),
        @JsonSubTypes.Type(value = RPKClientCredentialsConfig.class, name = "RPK"),
        @JsonSubTypes.Type(value = X509ClientCredentialsConfig.class, name = "X509")})
public interface LwM2MClientCredentialsConfig {

    @JsonIgnore
    SecurityMode getSecurityConfigClientMode();
}
