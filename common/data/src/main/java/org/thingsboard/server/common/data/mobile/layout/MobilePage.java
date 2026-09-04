// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.mobile.layout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import org.thingsboard.server.common.data.Views;

import java.io.Serializable;

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

    @JsonView(Views.Private.class)
    MobilePageType getType();

    @JsonView(Views.Private.class)
    boolean isVisible();

}
