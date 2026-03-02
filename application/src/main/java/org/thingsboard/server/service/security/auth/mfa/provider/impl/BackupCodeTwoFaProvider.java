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
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.model.mfa.account.BackupCodeTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.BackupCodeTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@TbCoreComponent
public class BackupCodeTwoFaProvider implements TwoFaProvider<BackupCodeTwoFaProviderConfig, BackupCodeTwoFaAccountConfig> {

    @Autowired @Lazy
    private TwoFaConfigManager twoFaConfigManager;

    @Override
    public BackupCodeTwoFaAccountConfig generateNewAccountConfig(User user, BackupCodeTwoFaProviderConfig providerConfig) {
        BackupCodeTwoFaAccountConfig config = new BackupCodeTwoFaAccountConfig();
        config.setCodes(generateCodes(providerConfig.getCodesQuantity(), 8));
        config.setSerializeHiddenFields(true);
        return config;
    }

    private static Set<String> generateCodes(int count, int length) {
        return Stream.generate(() -> StringUtils.random(length, "0123456789abcdef"))
                .distinct().limit(count)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean checkVerificationCode(SecurityUser user, String code, BackupCodeTwoFaProviderConfig providerConfig, BackupCodeTwoFaAccountConfig accountConfig) {
        if (CollectionsUtil.contains(accountConfig.getCodes(), code)) {
            accountConfig.getCodes().remove(code);
            twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user, accountConfig);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.BACKUP_CODE;
    }

}
