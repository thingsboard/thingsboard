// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.permission.GroupPermission;

import java.io.Serializable;
import java.util.List;

@JsonPropertyOrder({"type", "enabled", "title", "captcha", "permissions", "notificationRecipient", "signUpFields",
        "customerTitlePrefix", "showPrivacyPolicy", "showTermsOfUse", "defaultDashboard", "homeDashboard",
        "customerGroupId", "customMenuId"})
@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "WEB", schema = WebSelfRegistrationParams.class),
                @DiscriminatorMapping(value = "MOBILE", schema = MobileSelfRegistrationParams.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WebSelfRegistrationParams.class, name = "WEB"),
        @JsonSubTypes.Type(value = MobileSelfRegistrationParams.class, name = "MOBILE"),
})
public interface SelfRegistrationParams extends Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    SelfRegistrationType getType();

    Boolean getEnabled();

    String getTitle();

    CaptchaParams getCaptcha();

    List<GroupPermission> getPermissions();

    NotificationTargetId getNotificationRecipient();

    List<SignUpField> getSignUpFields();

    String getCustomerTitlePrefix();

    Boolean getShowPrivacyPolicy();

    Boolean getShowTermsOfUse();

    DefaultDashboardParams getDefaultDashboard();

    HomeDashboardParams getHomeDashboard();

    EntityGroupId getCustomerGroupId();

    CustomMenuId getCustomMenuId();

    SignUpSelfRegistrationParams toSignUpSelfRegistrationParams(PlatformType platformType);

}
