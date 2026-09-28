// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.loriot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.integration.http.loriot.credentials.LoriotCredentials;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoriotConfiguration {

    private String httpEndpoint;

    @TemplateField(key = "loriotServer", label = "LORIOT server",
                   type = FormFieldType.STRING_AUTOCOMPLETE,
                   options = {"eu1", "us1", "as1", "eu2", "ap1"},
                   group = "LORIOT Connection", required = true)
    private String server;

    @TemplateField(key = "loriotDomain", label = "LORIOT domain",
                   group = "LORIOT Connection")
    private String domain;

    @TemplateField(key = "loriotAppId", label = "Application ID",
                   type = FormFieldType.STRING,
                   secret = true, group = "LORIOT Connection", required = true)
    private String appId;

    private LoriotCredentials credentials;

    private boolean createLoriotOutput;

    private boolean enableSecurity;

    private boolean sendDownlink;

    @TemplateField(key = "loriotDownlinkUrl", label = "LORIOT downlink URL",
            type = FormFieldType.STRING,
            group = "LORIOT Connection",
            required = true)
    private String loriotDownlinkUrl;

    private Map<String, String> headersFilter;

    @TemplateField(key = "loriotToken", label = "Application Access Token",
                   type = FormFieldType.PASSWORD, secret = true,
                   group = "LORIOT Connection", required = true)
    private String token;
}
