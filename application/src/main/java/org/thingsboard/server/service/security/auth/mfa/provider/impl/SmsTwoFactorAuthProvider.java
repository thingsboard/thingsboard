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
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.config.account.SmsTwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.provider.SmsTwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProvider;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@TbCoreComponent
public class SmsTwoFactorAuthProvider implements TwoFactorAuthProvider<SmsTwoFactorAuthProviderConfig, SmsTwoFactorAuthAccountConfig> {

    private final SmsService smsService;
    private final TimeseriesService timeseriesService;

    @Override
    public SmsTwoFactorAuthAccountConfig generateNewAccountConfig(User user, SmsTwoFactorAuthProviderConfig providerConfig) {
        return new SmsTwoFactorAuthAccountConfig();
    }

    @Override
    @SneakyThrows // fixme
    public void prepareVerificationCode(SecurityUser user, SmsTwoFactorAuthProviderConfig providerConfig, SmsTwoFactorAuthAccountConfig accountConfig) {
        ConstraintValidator.validateFields(accountConfig);

        String verificationCode = RandomStringUtils.randomNumeric(6);
        saveVerificationCode(user, verificationCode);

        String phoneNumber = accountConfig.getPhoneNumber();

        Map<String, String> data = Map.of(
                "verificationCode", verificationCode,
                "userEmail", user.getEmail()
        );
        String message = TbNodeUtils.processTemplate(providerConfig.getSmsVerificationMessageTemplate(), data);

        smsService.sendSms(user.getTenantId(), user.getCustomerId(), new String[]{phoneNumber}, message);
    }

    @Override
    public boolean checkVerificationCode(SecurityUser user, String verificationCode, SmsTwoFactorAuthAccountConfig accountConfig) {
        if (verificationCode.equals(getVerificationCode(user))) {
            removeVerificationCode(user);
            return true;
        } else {
            return false;
        }
    }


    @SneakyThrows
    private void saveVerificationCode(SecurityUser user, String verificationCode) {
        timeseriesService.save(user.getTenantId(), user.getId(),
                new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("twoFaVerificationCode:" + user.getSessionId(), verificationCode))
        ).get();
    }

    @SneakyThrows
    private String getVerificationCode(SecurityUser user) {
        return timeseriesService.findLatest(user.getTenantId(), user.getId(),
                Collections.singletonList("twoFaVerificationCode:" + user.getSessionId())).get().stream().findFirst()
                .map(codeTs -> codeTs.getStrValue().get())
                .orElse(null);
    }

    private void removeVerificationCode(SecurityUser user) {
        timeseriesService.remove(user.getTenantId(), user.getId(), Collections.singletonList(
                new BaseDeleteTsKvQuery("twoFaVerificationCode:" + user.getSessionId(), 0, System.currentTimeMillis())
        ));
    }


    @Override
    public TwoFactorAuthProviderType getType() {
        return TwoFactorAuthProviderType.SMS;
    }

}
