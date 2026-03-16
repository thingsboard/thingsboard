/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
