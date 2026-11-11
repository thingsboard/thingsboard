// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.thingpark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

/**
 * Synthetic descriptor for the ThingPark Enterprise (TPE) integration's persisted
 * configuration. Walked by the export pipeline (PojoFieldWalker +
 * IntegrationConfigPojoRegistry) to discover {@link TemplateField} annotations. Covers
 * the new-security mode fields only — legacy mode is out of scope for export v1. Runtime
 * persistence still uses {@code ThingParkIntegrationEnterprise.init()}'s raw-JSON read
 * path — this class is never instantiated.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThingParkEnterpriseConfiguration {

    @TemplateField(key = "tpeAsId", label = "Application Server ID",
                   group = "TPE Connection", required = true)
    private String asIdNew;

    @TemplateField(key = "tpeAsKey", label = "Application Server Key",
                   type = FormFieldType.PASSWORD, secret = true,
                   group = "TPE Connection", required = true)
    private String asKey;

    @TemplateField(key = "tpeClientId", label = "OAuth client ID",
                   group = "TPE Connection")
    private String clientIdNew;

    @TemplateField(key = "tpeClientSecret", label = "OAuth client secret",
                   type = FormFieldType.PASSWORD, secret = true,
                   group = "TPE Connection")
    private String clientSecret;

    @TemplateField(key = "tpeDownlinkUrl", label = "Downlink URL",
                   group = "TPE Connection")
    private String downlinkUrl;

}
