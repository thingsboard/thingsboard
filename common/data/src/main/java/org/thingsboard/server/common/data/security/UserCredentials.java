/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Arrays;
import java.util.Objects;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.setJson;

public class UserCredentials extends BaseData<UserCredentialsId> {

    private static final long serialVersionUID = -2108436378880529163L;

    private UserId userId;
    private boolean enabled;
    private String password;
    private String activateToken;
    private String resetToken;

    @NoXss
    private transient JsonNode additionalInfo;

    @JsonIgnore
    private byte[] additionalInfoBytes;

    public JsonNode getAdditionalInfo() {
        return getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    public void setAdditionalInfo(JsonNode settings) {
        setJson(settings, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }
    
    public UserCredentials() {
        super();
    }

    public UserCredentials(UserCredentialsId id) {
        super(id);
    }

    public UserCredentials(UserCredentials userCredentials) {
        super(userCredentials);
        this.userId = userCredentials.getUserId();
        this.password = userCredentials.getPassword();
        this.enabled = userCredentials.isEnabled();
        this.activateToken = userCredentials.getActivateToken();
        this.resetToken = userCredentials.getResetToken();
        setAdditionalInfo(userCredentials.getAdditionalInfo());
    }

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getActivateToken() {
        return activateToken;
    }

    public void setActivateToken(String activateToken) {
        this.activateToken = activateToken;
    }
    
    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserCredentials that = (UserCredentials) o;
        return enabled == that.enabled && userId.equals(that.userId) && Objects.equals(password, that.password)
                && Objects.equals(activateToken, that.activateToken) && Objects.equals(resetToken, that.resetToken)
                && Arrays.equals(additionalInfoBytes, that.additionalInfoBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), userId, enabled, password, activateToken, resetToken);
        result = 31 * result + Arrays.hashCode(additionalInfoBytes);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserCredentials [userId=");
        builder.append(userId);
        builder.append(", enabled=");
        builder.append(enabled);
        builder.append(", password=");
        builder.append(password);
        builder.append(", activateToken=");
        builder.append(activateToken);
        builder.append(", resetToken=");
        builder.append(resetToken);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
