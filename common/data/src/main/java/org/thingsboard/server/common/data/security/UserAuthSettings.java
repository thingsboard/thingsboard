// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.UserAuthSettingsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAuthSettings extends BaseData<UserAuthSettingsId> {

    private static final long serialVersionUID = 2628320657987010348L;

    private UserId userId;
    private AccountTwoFaSettings twoFaSettings;

}
