// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.oauth2.PlatformType;

import java.io.Serializable;

@Schema(
        discriminatorProperty = "version",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "enterprise", schema = EnterpriseCaptchaParams.class),
                @DiscriminatorMapping(value = "v2", schema = V2CaptchaParams.class),
                @DiscriminatorMapping(value = "v3", schema = V3CaptchaParams.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "version")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EnterpriseCaptchaParams.class, name = "enterprise"),
        @JsonSubTypes.Type(value = V2CaptchaParams.class, name = "v2"),
        @JsonSubTypes.Type(value = V3CaptchaParams.class, name = "v3")
})
public interface CaptchaParams extends Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String getVersion();

    CaptchaParams toInfo(PlatformType platformType);
}
