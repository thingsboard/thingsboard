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
