// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
