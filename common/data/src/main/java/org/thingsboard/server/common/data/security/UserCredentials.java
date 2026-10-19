// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
