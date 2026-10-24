// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

/**
 * Created by ashvayka on 16.01.17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoreConfiguration {

    @TemplateField(key = "opcuaKeystoreType", label = "Keystore type", type = FormFieldType.STRING_AUTOCOMPLETE, options = {"JKS", "PKCS12"}, group = "OPC UA Keystore")
    private String type;
    private String location;
    @TemplateField(key = "opcuaKeystoreFileContent", label = "Keystore file", type = FormFieldType.PASSWORD, secret = true, secretType = SecretType.TEXT_FILE, group = "OPC UA Keystore")
    private String fileContent;
    @TemplateField(key = "opcuaKeystorePassword", label = "Keystore password", type = FormFieldType.PASSWORD, secret = true, group = "OPC UA Keystore")
    private String password;
    @TemplateField(key = "opcuaKeystoreAlias", label = "Certificate alias", group = "OPC UA Keystore")
    private String alias;
    @TemplateField(key = "opcuaKeystoreKeyPassword", label = "Key password", type = FormFieldType.PASSWORD, secret = true, group = "OPC UA Keystore")
    private String keyPassword;

}
