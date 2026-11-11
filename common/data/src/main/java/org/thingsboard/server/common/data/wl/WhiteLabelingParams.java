// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Schema
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhiteLabelingParams {

    @Schema(description = "Logo image URL", example = "https://company.com/images/logo.png")
    protected String logoImageUrl;
    @Schema(description = "The height of a logo container. Logo image will be automatically scaled.")
    protected Integer logoImageHeight;
    @Schema(description = "Logo shown when side menu is collapsed", example = "https://company.com/images/collapsed-logo.png")
    protected String collapsedLogoImageUrl;
    @Schema(description = "White-labeled name of the platform", example = "My Company IoT Platform")
    protected String appTitle;
    @Schema(description = "JSON object that contains website icon url and type")
    protected Favicon favicon;
    @Schema(description = "Complex JSON that describes structure of the Angular Material Palette. See [theming](https://material.angular.io/guide/theming) for more details")
    protected PaletteSettings paletteSettings;
    @Schema(description = "Whether primary-color panels (top toolbar, side navigation, dialog and entity-details headers) use the primary palette color as background. When false, they render on a white background")
    protected Boolean primaryColorPanels;
    @Schema(description = "Base URL for help link")
    protected String helpLinkBaseUrl;
    @Schema(description = "Base URL for the repository with the UI help components (markdown)")
    protected String uiHelpBaseUrl;
    @Schema(description = "Enable or Disable help links")
    protected Boolean enableHelpLinks;
    @Schema(description = "Enable white-labeling", accessMode = Schema.AccessMode.READ_ONLY)
    protected boolean whiteLabelingEnabled = true;
    @Schema(description = "Show platform name and version on UI and login screen")
    protected Boolean showNameVersion;
    @Schema(description = "White-labeled platform name")
    protected String platformName;
    @Schema(description = "White-labeled platform version")
    protected String platformVersion;
    @Schema(description = "Custom CSS content")
    protected String customCss;
    @Schema(description = "Hide device connectivity dialog")
    protected Boolean hideConnectivityDialog;
    @Schema(description = "Override Trendz Add-on name")
    protected Boolean overrideTrendzName;

    public WhiteLabelingParams merge(WhiteLabelingParams otherWlParams) {
        if (StringUtils.isEmpty(this.logoImageUrl)) {
            this.logoImageUrl = otherWlParams.logoImageUrl;
        }
        if (this.logoImageHeight == null) {
            this.logoImageHeight = otherWlParams.logoImageHeight;
        }
        if (StringUtils.isEmpty(this.collapsedLogoImageUrl)) {
            this.collapsedLogoImageUrl = otherWlParams.collapsedLogoImageUrl;
        }
        if (StringUtils.isEmpty(appTitle)) {
            this.appTitle = otherWlParams.appTitle;
        }
        if (favicon == null || StringUtils.isEmpty(favicon.getUrl())) {
            this.favicon = otherWlParams.favicon;
        }
        if (this.paletteSettings == null) {
            this.paletteSettings = otherWlParams.paletteSettings;
        } else if (otherWlParams.paletteSettings != null) {
            this.paletteSettings.merge(otherWlParams.paletteSettings);
        }
        if (otherWlParams.helpLinkBaseUrl != null) {
            this.helpLinkBaseUrl = otherWlParams.helpLinkBaseUrl;
        }
        if (otherWlParams.uiHelpBaseUrl != null) {
            this.uiHelpBaseUrl = otherWlParams.uiHelpBaseUrl;
        }
        if (otherWlParams.enableHelpLinks != null) {
            this.enableHelpLinks = otherWlParams.enableHelpLinks;
        }
        if (this.showNameVersion == null) {
            this.showNameVersion = otherWlParams.showNameVersion;
            this.platformName = otherWlParams.platformName;
            this.platformVersion = otherWlParams.platformVersion;
        }
        if (!StringUtils.isEmpty(otherWlParams.customCss)) {
            if (StringUtils.isEmpty(this.customCss)) {
                this.customCss = otherWlParams.customCss;
            } else {
                this.customCss = otherWlParams.customCss + "\n" + this.customCss;
            }
        }
        if (this.hideConnectivityDialog == null) {
            this.hideConnectivityDialog = false;
        }
        if (this.overrideTrendzName == null) {
            this.overrideTrendzName = otherWlParams.overrideTrendzName;
        }
        if (this.primaryColorPanels == null) {
            this.primaryColorPanels = otherWlParams.primaryColorPanels;
        }
        return this;
    }
}
