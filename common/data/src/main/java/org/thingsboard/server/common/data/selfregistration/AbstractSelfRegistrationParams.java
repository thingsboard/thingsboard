// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.permission.GroupPermission;

import java.util.List;

@Data
@EqualsAndHashCode
public abstract class AbstractSelfRegistrationParams implements SelfRegistrationParams {

    @Schema(description = "Indicates if self-registration is allowed via sign-up form")
    protected Boolean enabled;
    @Schema(description = "The text message to appear on login form")
    @NotNull
    protected String title;
    @Schema(description = "Captcha site key for 'I'm not a robot' validation")
    @NotNull
    protected CaptchaParams captcha;
    @Schema(description = "List of sign-up form fields")
    @NotNull
    protected List<SignUpField> signUpFields;
    @Schema(description = "Show or hide 'Privacy Policy'")
    protected Boolean showPrivacyPolicy;
    @Schema(description = "Show or hide 'Terms of Use'")
    protected Boolean showTermsOfUse;
    @Schema(description = "Recipient to use for notifications when new user self-registered.")
    protected NotificationTargetId notificationRecipient;
    @Schema(description = "Prefix to add to created customer")
    protected String customerTitlePrefix;
    @Schema(description = "Id of the customer group customer wil be added to.")
    protected EntityGroupId customerGroupId;
    @Schema(description = "Group Permissions to assign for the new customer user.")
    @NotNull
    protected List<GroupPermission> permissions;
    @Schema(description = "Default dashboard params")
    protected DefaultDashboardParams defaultDashboard;
    @Schema(description = "Home dashboard params")
    protected HomeDashboardParams homeDashboard;
    @Schema(description = "Custom menu id")
    protected CustomMenuId customMenuId;
    @Schema(description = "Privacy policy text. Supports HTML.")
    protected String privacyPolicy;
    @Schema(description = "Terms of User text. Supports HTML.")
    protected String termsOfUse;

    @Override
    public SignUpSelfRegistrationParams toSignUpSelfRegistrationParams(PlatformType platformType) {
        return (enabled) ? new SignUpSelfRegistrationParams(
                title,
                captcha.toInfo(platformType),
                signUpFields,
                showPrivacyPolicy,
                showTermsOfUse) : null;
    }
}
