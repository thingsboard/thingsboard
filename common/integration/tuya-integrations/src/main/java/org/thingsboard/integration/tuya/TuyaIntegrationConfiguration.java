// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.integration.tuya.mq.MqEnv;
import org.thingsboard.integration.tuya.mq.TuyaRegion;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TuyaIntegrationConfiguration {

    private TuyaRegion region;

    @TemplateField(key = "tuyaAccessId", label = "Access ID",
                   secret = true, group = "Tuya Connection", required = true)
    private String accessId;

    @TemplateField(key = "tuyaAccessKey", label = "Access key",
                   type = FormFieldType.PASSWORD, secret = true, group = "Tuya Connection", required = true)
    private String accessKey;

    private MqEnv env;

}
