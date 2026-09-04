// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.UserId;

import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Builder
@AllArgsConstructor
@Data
public class AlarmAssignee implements Serializable {

    private static final long serialVersionUID = 6628286223963972860L;

    private final UserId id;
    private final String firstName;
    private final String lastName;
    private final String email;

    @JsonIgnore
    public String getTitle() {
        String title = "";
        if (isNotEmpty(firstName)) {
            title += firstName;
        }
        if (isNotEmpty(lastName)) {
            if (!title.isEmpty()) {
                title += " ";
            }
            title += lastName;
        }
        if (title.isEmpty()) {
            title = email;
        }
        return title;
    }

}
