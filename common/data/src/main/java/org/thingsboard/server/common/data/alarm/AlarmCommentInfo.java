/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AlarmCommentInfo extends AlarmComment {
    private static final long serialVersionUID = 2807343093519543377L;

    @ApiModelProperty(position = 19, value = "User name", example = "John")
    private String firstName;

    @ApiModelProperty(position = 19, value = "User name", example = "Brown")
    private String lastName;

    public AlarmCommentInfo() {
        super();
    }

    public AlarmCommentInfo(AlarmComment alarmComment) {
        super(alarmComment);
    }

    public AlarmCommentInfo(AlarmComment alarmComment, String firstName, String lastName) {
        super(alarmComment);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AlarmCommentInfo alarmCommentInfo = (AlarmCommentInfo) o;

        if (firstName == null) {
            if (alarmCommentInfo.firstName != null)
                return false;
        } else if (!firstName.equals(alarmCommentInfo.firstName))
            return false;

        if (lastName == null) {
            if (alarmCommentInfo.lastName != null)
                return false;
        } else if (!lastName.equals(alarmCommentInfo.lastName))
            return false;

        return true;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        return result;
    }
}
