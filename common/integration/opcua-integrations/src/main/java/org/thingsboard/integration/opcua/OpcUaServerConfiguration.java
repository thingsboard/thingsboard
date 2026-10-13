// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpcUaServerConfiguration {

    @TemplateField(key = "opcuaApplicationName", label = "Application name", group = "OPC UA Connection")
    private String applicationName;
    @TemplateField(key = "opcuaApplicationUri", label = "Application URI", group = "OPC UA Connection")
    private String applicationUri;
    @TemplateField(key = "opcuaHost", label = "Server host", group = "OPC UA Connection", required = true)
    private String host;
    @TemplateField(key = "opcuaPort", label = "Server port", type = FormFieldType.INTEGER, group = "OPC UA Connection", required = true)
    private int port;
    @TemplateField(key = "opcuaEndpoint", label = "Endpoint", group = "OPC UA Connection")
    private String endpoint;
    @TemplateField(key = "opcuaScanPeriodInSeconds", label = "Scan period (s)", type = FormFieldType.INTEGER, group = "OPC UA Connection")
    private int scanPeriodInSeconds;
    @TemplateField(key = "opcuaTimeoutInMillis", label = "Timeout (ms)", type = FormFieldType.INTEGER, group = "OPC UA Connection")
    private int timeoutInMillis;
    @TemplateField(key = "opcuaSecurity", label = "Security policy", type = FormFieldType.STRING_AUTOCOMPLETE, options = {"None", "Basic128Rsa15", "Basic256", "Basic256Sha256", "Aes128_Sha256_RsaOaep", "Aes256_Sha256_RsaPss"}, group = "OPC UA Connection")
    private String security;
    private IdentityProviderConfiguration identity;
    private KeystoreConfiguration keystore;
    private List<DeviceMapping> mapping;

}
