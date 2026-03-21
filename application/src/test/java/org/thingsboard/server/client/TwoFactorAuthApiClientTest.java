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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.AccountTwoFaSettings;
import org.thingsboard.client.model.PlatformTwoFaSettings;
import org.thingsboard.client.model.TotpTwoFaAccountConfig;
import org.thingsboard.client.model.TotpTwoFaProviderConfig;
import org.thingsboard.client.model.TwoFaAccountConfig;
import org.thingsboard.client.model.TwoFaProviderType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class TwoFactorAuthApiClientTest extends AbstractApiClientTest {

    @Test
    public void testTwoFactorAuthLifecycle() throws Exception {
        // save original platform 2FA settings as sysadmin
        client.login("sysadmin@thingsboard.org", "sysadmin");

        // configure platform 2FA settings with TOTP provider
        TotpTwoFaProviderConfig totpProviderConfig = new TotpTwoFaProviderConfig();
        totpProviderConfig.setIssuerName("TestThingsBoard");

        PlatformTwoFaSettings newSettings = new PlatformTwoFaSettings();
        newSettings.setProviders(List.of(totpProviderConfig));
        newSettings.setMinVerificationCodeSendPeriod(30);
        newSettings.setTotalAllowedTimeForVerification(300);
        newSettings.setMaxVerificationFailuresBeforeUserLockout(5);

        PlatformTwoFaSettings savedSettings = client.savePlatformTwoFaSettings(newSettings);
        assertNotNull(savedSettings);
        assertNotNull(savedSettings.getProviders());
        assertFalse(savedSettings.getProviders().isEmpty());
        assertEquals(30, savedSettings.getMinVerificationCodeSendPeriod().intValue());
        assertEquals(300, savedSettings.getTotalAllowedTimeForVerification().intValue());

        // get available 2FA providers (should include TOTP)
        List<TwoFaProviderType> providerTypes = client.getAvailableTwoFaProviderTypes();
        assertNotNull(providerTypes);
        assertTrue(providerTypes.contains(TwoFaProviderType.TOTP));

        // get account 2FA settings (should be empty initially)
        AccountTwoFaSettings accountSettings = client.getAccountTwoFaSettings();
        assertNull(accountSettings);

        // generate TOTP account config
        TwoFaAccountConfig generatedConfig = client.generateTwoFaAccountConfig(TwoFaProviderType.TOTP.getValue());
        assertNotNull(generatedConfig);
        TotpTwoFaAccountConfig totpConfig = (TotpTwoFaAccountConfig) generatedConfig;
        assertNotNull(totpConfig);
        assertNotNull(totpConfig.getAuthUrl());
        assertTrue(totpConfig.getAuthUrl().startsWith("otpauth://totp/"));
    }

}
