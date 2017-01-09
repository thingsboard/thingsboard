/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;

public class UserCredentials extends BaseData<UserCredentialsId> {

    private static final long serialVersionUID = -2108436378880529163L;

    private UserId userId;
    private boolean enabled;
    private String password;
    private String activateToken;
    private String resetToken;
    
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((activateToken == null) ? 0 : activateToken.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((resetToken == null) ? 0 : resetToken.hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserCredentials other = (UserCredentials) obj;
        if (activateToken == null) {
            if (other.activateToken != null)
                return false;
        } else if (!activateToken.equals(other.activateToken))
            return false;
        if (enabled != other.enabled)
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (resetToken == null) {
            if (other.resetToken != null)
                return false;
        } else if (!resetToken.equals(other.resetToken))
            return false;
        if (userId == null) {
            if (other.userId != null)
                return false;
        } else if (!userId.equals(other.userId))
            return false;
        return true;
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
