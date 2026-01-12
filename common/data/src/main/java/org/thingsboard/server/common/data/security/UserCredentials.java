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
package org.thingsboard.server.common.data.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserCredentials extends BaseDataWithAdditionalInfo<UserCredentialsId> {

    @Serial
    private static final long serialVersionUID = -2108436378880529163L;

    private UserId userId;
    private boolean enabled;
    private String password;
    private String activateToken;
    private Long activateTokenExpTime;
    private String resetToken;
    private Long resetTokenExpTime;
    private Long lastLoginTs;
    private Integer failedLoginAttempts;

    public UserCredentials() {
        super();
    }

    public UserCredentials(UserCredentialsId id) {
        super(id);
    }


    @JsonIgnore
    public boolean isActivationTokenExpired() {
        return getActivationTokenTtl() == 0;
    }

    @JsonIgnore
    public long getActivationTokenTtl() {
        return activateTokenExpTime != null ? Math.max(activateTokenExpTime - System.currentTimeMillis(), 0) : 0;
    }

    @JsonIgnore
    public boolean isResetTokenExpired() {
        return getResetTokenTtl() == 0;
    }

    @JsonIgnore
    public long getResetTokenTtl() {
        return resetTokenExpTime != null ? Math.max(resetTokenExpTime - System.currentTimeMillis(), 0) : 0;
    }

}
