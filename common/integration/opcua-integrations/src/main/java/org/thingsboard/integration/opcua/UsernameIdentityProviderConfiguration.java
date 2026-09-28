// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import lombok.Data;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

/**
 * Created by ashvayka on 16.01.17.
 */
@Data
public class UsernameIdentityProviderConfiguration implements IdentityProviderConfiguration {

    @TemplateField(key = "opcuaUsername", label = "Username", secret = true, group = "OPC UA Auth", required = true)
    private final String username;
    @TemplateField(key = "opcuaPassword", label = "Password", type = FormFieldType.PASSWORD, secret = true, group = "OPC UA Auth", required = true)
    private final String password;

    @Override
    public IdentityProvider toProvider() {
        return new UsernameProvider(username, password);
    }
}
