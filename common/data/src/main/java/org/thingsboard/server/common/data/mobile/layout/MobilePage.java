// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile.layout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.Views;

import java.io.Serializable;

@Schema(
        description = "Configuration for a mobile page",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DEFAULT", schema = DefaultMobilePage.class),
                @DiscriminatorMapping(value = "DASHBOARD", schema = DashboardPage.class),
                @DiscriminatorMapping(value = "WEB_VIEW", schema = WebViewPage.class),
                @DiscriminatorMapping(value = "CUSTOM", schema = CustomMobilePage.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultMobilePage.class, name = "DEFAULT"),
        @JsonSubTypes.Type(value = DashboardPage.class, name = "DASHBOARD"),
        @JsonSubTypes.Type(value = WebViewPage.class, name = "WEB_VIEW"),
        @JsonSubTypes.Type(value = CustomMobilePage.class, name = "CUSTOM")
})
public interface MobilePage extends Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonView(Views.Private.class)
    MobilePageType getType();

    @JsonView(Views.Private.class)
    boolean isVisible();

}
