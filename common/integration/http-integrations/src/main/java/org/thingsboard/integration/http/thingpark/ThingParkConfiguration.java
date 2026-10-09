// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.thingpark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

/**
 * Synthetic descriptor for the ThingPark integration's persisted configuration.
 * Walked by the export pipeline (PojoFieldWalker + IntegrationConfigPojoRegistry) to
 * discover {@link TemplateField} annotations. Runtime persistence still uses
 * {@code ThingParkIntegration.init()}'s raw-JSON read path — this class is never
 * instantiated.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThingParkConfiguration {

    @TemplateField(key = "thingparkAsId", label = "Application Server ID",
                   group = "ThingPark Connection", required = true)
    private String asId;

    @TemplateField(key = "thingparkAsKey", label = "Application Server Key",
                   type = FormFieldType.PASSWORD, secret = true,
                   group = "ThingPark Connection", required = true)
    private String asKey;

    @TemplateField(key = "thingparkMaxTimeDiffInSeconds", label = "Max time difference (s)",
                   type = FormFieldType.INTEGER,
                   group = "ThingPark Connection")
    private long maxTimeDiffInSeconds;

    @TemplateField(key = "thingparkDownlinkUrl", label = "Downlink URL",
                   group = "ThingPark Connection")
    private String downlinkUrl;

}
