// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.wl;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.DomainId;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginWhiteLabelingParams extends WhiteLabelingParams {

    @Schema(description = "Login page background color", example = "#d90f0f")
    private String pageBackgroundColor;
    @Schema(description = "Enable/Disable dark foreground")
    private boolean darkForeground;
    @Schema(description = "Domain id")
    private DomainId domainId;
    @Schema(description = "Base URL for the activation link, etc", example = "https://iot.mycompany.com")
    private String baseUrl;
    @Schema(description = "Prohibit use of other URLs. It is recommended to enable this setting", example = "true")
    private boolean prohibitDifferentUrl;
    @Schema(description = "Id of the settings object that store this parameters")
    private String adminSettingsId;
    @Schema(description = "Show platform name and version on login page")
    private Boolean showNameBottom;

    public LoginWhiteLabelingParams merge(LoginWhiteLabelingParams otherWlParams) {
        Integer prevLogoImageHeight = this.logoImageHeight;
        super.merge(otherWlParams);
        if (prevLogoImageHeight == null) {
            this.logoImageHeight = null;
        }
        if (this.showNameBottom == null) {
            this.showNameBottom = otherWlParams.showNameBottom;
        }
        if (this.pageBackgroundColor == null) {
            this.pageBackgroundColor = otherWlParams.pageBackgroundColor;
        }
        return this;
    }
}
