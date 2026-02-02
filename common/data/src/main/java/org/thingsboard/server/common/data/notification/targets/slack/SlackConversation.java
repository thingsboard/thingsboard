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
package org.thingsboard.server.common.data.notification.targets.slack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackConversation implements NotificationRecipient {

    @NotNull
    private SlackConversationType type;
    @NotEmpty
    private String id;
    @NotEmpty
    private String name;

    private String wholeName;
    private String email;

    @Override
    public String getTitle() {
        if (type == SlackConversationType.DIRECT) {
            return StringUtils.defaultIfEmpty(wholeName, name);
        } else {
            return name;
        }
    }

    @JsonIgnore
    @Override
    public String getFirstName() {
        String firstName = StringUtils.contains(wholeName, " ") ? wholeName.split(" ")[0] : wholeName;
        if (isEmpty(firstName)) {
            firstName = name;
        }
        return firstName;
    }

    @JsonIgnore
    @Override
    public String getLastName() {
        return StringUtils.contains(wholeName, " ") ? wholeName.split(" ")[1] : null;
    }

    @JsonIgnore
    public String getPointer() {
        return type == SlackConversationType.DIRECT ? "@" : "#";
    }

}
