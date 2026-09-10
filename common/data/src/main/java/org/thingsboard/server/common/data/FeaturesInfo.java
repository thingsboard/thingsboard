// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@JsonPropertyOrder({
        "emailEnabled",
        "smsEnabled",
        "notificationEnabled",
        "oauthEnabled",
        "twoFaEnabled"
})
@Schema
@Data
public class FeaturesInfo {
    @JsonProperty("emailEnabled")
    boolean isEmailEnabled;
    @JsonProperty("smsEnabled")
    boolean isSmsEnabled;
    @JsonProperty("notificationEnabled")
    boolean isNotificationEnabled;
    @JsonProperty("oauthEnabled")
    boolean isOauthEnabled;
    @JsonProperty("twoFaEnabled")
    boolean isTwoFaEnabled;
}
