// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

/**
 * Synthetic descriptor for the ChirpStack integration's persisted clientConfiguration.
 * The export pipeline (see PojoFieldWalker + IntegrationConfigPojoRegistry) walks this
 * class by reflection to discover {@link TemplateField} annotations. Runtime persistence
 * still uses {@code ChirpStackIntegration.init()}'s raw-JSON read path — this class is
 * never instantiated.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChirpStackConfiguration {

    @TemplateField(key = "chirpstackApplicationServerUrl", label = "Application server URL",
                   group = "ChirpStack Connection", required = true)
    private String applicationServerUrl;

    @TemplateField(key = "chirpstackApplicationServerApiToken", label = "Application server API token",
                   type = FormFieldType.PASSWORD, secret = true,
                   group = "ChirpStack Connection", required = true)
    private String applicationServerAPIToken;

    @TemplateField(key = "chirpstackUseApi4Plus", label = "Use ChirpStack v4+ API",
                   type = FormFieldType.BOOLEAN,
                   group = "ChirpStack Connection")
    private boolean useAPI4Plus;

}
