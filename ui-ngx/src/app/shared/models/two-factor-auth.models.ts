///
/// Copyright © 2016-2022 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

export interface TwoFactorAuthSettings {
  maxVerificationFailuresBeforeUserLockout: number;
  providers: Array<TwoFactorAuthProviderConfig>;
  totalAllowedTimeForVerification: number;
  useSystemTwoFactorAuthSettings: boolean;
  verificationCodeCheckRateLimit: string;
  minVerificationCodeSendPeriod: number;
}

export interface TwoFactorAuthSettingsForm extends TwoFactorAuthSettings{
  providers: Array<TwoFactorAuthProviderConfigForm>;
  verificationCodeCheckRateLimitEnable: boolean;
  verificationCodeCheckRateLimitNumber: number;
  verificationCodeCheckRateLimitTime: number;
}

export type TwoFactorAuthProviderConfig = Partial<TotpTwoFactorAuthProviderConfig | SmsTwoFactorAuthProviderConfig |
                                                    EmailTwoFactorAuthProviderConfig>;

export type TwoFactorAuthProviderConfigForm = Partial<TotpTwoFactorAuthProviderConfig | SmsTwoFactorAuthProviderConfig |
  EmailTwoFactorAuthProviderConfig> & TwoFactorAuthProviderFormConfig;

export interface TotpTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  issuerName: string;
}

export interface SmsTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  smsVerificationMessageTemplate: string;
  verificationCodeLifetime: number;
}

export interface EmailTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  verificationCodeLifetime: number;
}

export interface TwoFactorAuthProviderFormConfig {
  enable: boolean;
}

export enum TwoFactorAuthProviderType{
  TOTP = 'TOTP',
  SMS = 'SMS',
  EMAIL = 'EMAIL'
}

interface GeneralTwoFactorAuthAccountConfig {
  providerType: TwoFactorAuthProviderType;
  useByDefault: boolean;
}

export interface TotpTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  authUrl: string;
}

export interface SmsTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  phoneNumber: string;
}

export interface EmailTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  email: string;
}

export type TwoFactorAuthAccountConfig = TotpTwoFactorAuthAccountConfig | SmsTwoFactorAuthAccountConfig | EmailTwoFactorAuthAccountConfig;


export interface AccountTwoFaSettings {
  configs: {TwoFactorAuthProviderType: TwoFactorAuthAccountConfig};
}

export interface TwoFaProviderInfo {
  type: TwoFactorAuthProviderType;
  default: boolean;
  contact?: string;
  minVerificationCodeSendPeriod?: number;
}

export interface TwoFactorAuthProviderData {
  name: string;
  description: string;
}

export interface TwoFactorAuthProviderLoginData extends TwoFactorAuthProviderData {
  icon: string;
}

export const twoFactorAuthProvidersData = new Map<TwoFactorAuthProviderType, TwoFactorAuthProviderData>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
        name: 'security.2fa.provider.totp',
        description: 'security.2fa.provider.totp-description'
      }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
        name: 'security.2fa.provider.sms',
        description: 'security.2fa.provider.sms-description'
      }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
        name: 'security.2fa.provider.email',
        description: 'security.2fa.provider.email-description'
      }
    ],
  ]
);

export const twoFactorAuthProvidersLoginData = new Map<TwoFactorAuthProviderType, TwoFactorAuthProviderLoginData>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
      name: 'security.2fa.provider.totp',
      description: 'login.totp-auth-description',
      icon: 'mdi:cellphone-key'
    }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
      name: 'security.2fa.provider.sms',
      description: 'login.sms-auth-description',
      icon: 'mdi:message-reply-text-outline'
    }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
      name: 'security.2fa.provider.email',
      description: 'login.email-auth-description',
      icon: 'mdi:email-outline'
    }
    ],
  ]
);
