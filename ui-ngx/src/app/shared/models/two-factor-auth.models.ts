///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { UsersFilter } from '@shared/models/notification.models';

export interface TwoFactorAuthSettings {
  enforceTwoFa: boolean;
  enforcedUsersFilter: UsersFilter;
  maxVerificationFailuresBeforeUserLockout: number;
  providers: Array<TwoFactorAuthProviderConfig>;
  totalAllowedTimeForVerification: number;
  useSystemTwoFactorAuthSettings: boolean;
  verificationCodeCheckRateLimit: string;
  minVerificationCodeSendPeriod: number;
}

export interface TwoFactorAuthSettingsForm extends TwoFactorAuthSettings{
  enforceTwoFa: boolean;
  enforcedUsersFilter: UsersFilterWithFilterByTenant;
  providers: Array<TwoFactorAuthProviderConfigForm>;
  verificationCodeCheckRateLimitEnable: boolean;
  verificationCodeCheckRateLimitNumber: number;
  verificationCodeCheckRateLimitTime: number;
}

export interface UsersFilterWithFilterByTenant extends UsersFilter{
  filterByTenants?: boolean;
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
  EMAIL = 'EMAIL',
  BACKUP_CODE = 'BACKUP_CODE'
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

export interface BackupCodeTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  codesLeft: number;
  codes?: Array<string>;
}

export type TwoFactorAuthAccountConfig = TotpTwoFactorAuthAccountConfig | SmsTwoFactorAuthAccountConfig |
  EmailTwoFactorAuthAccountConfig | BackupCodeTwoFactorAuthAccountConfig;

export interface AccountTwoFaSettings {
  configs: AccountTwoFaSettingProviders;
}

export type AccountTwoFaSettingProviders = {
  [key in TwoFactorAuthProviderType]?: TwoFactorAuthAccountConfig;
};

export interface TwoFaProviderInfo {
  type: TwoFactorAuthProviderType;
  default: boolean;
  contact?: string;
  minVerificationCodeSendPeriod?: number;
}

export interface TwoFactorAuthProviderData {
  name: string;
  description: string;
  activatedHint: string;
}

export interface TwoFactorAuthProviderLoginData extends Omit<TwoFactorAuthProviderData, 'activatedHint'> {
  icon: string;
  placeholder: string;
}

export const twoFactorAuthProvidersData = new Map<TwoFactorAuthProviderType, TwoFactorAuthProviderData>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
        name: 'security.2fa.provider.totp',
        description: 'security.2fa.provider.totp-description',
        activatedHint: 'security.2fa.provider.totp-hint'
      }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
        name: 'security.2fa.provider.sms',
        description: 'security.2fa.provider.sms-description',
        activatedHint: 'security.2fa.provider.sms-hint'
      }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
        name: 'security.2fa.provider.email',
        description: 'security.2fa.provider.email-description',
        activatedHint: 'security.2fa.provider.email-hint'
      }
    ],
    [
      TwoFactorAuthProviderType.BACKUP_CODE, {
        name: 'security.2fa.provider.backup_code',
        description: 'security.2fa.provider.backup-code-description',
        activatedHint: 'security.2fa.provider.backup-code-hint'
      }
    ]
  ]
);

export const twoFactorAuthProvidersLoginData = new Map<TwoFactorAuthProviderType, TwoFactorAuthProviderLoginData>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
        name: 'security.2fa.provider.totp',
        description: 'login.totp-auth-description',
        placeholder: 'login.totp-auth-placeholder',
        icon: 'mdi:cellphone-key'
      }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
        name: 'security.2fa.provider.sms',
        description: 'login.sms-auth-description',
        placeholder: 'login.sms-auth-placeholder',
        icon: 'mdi:message-reply-text-outline'
      }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
        name: 'security.2fa.provider.email',
        description: 'login.email-auth-description',
        placeholder: 'login.email-auth-placeholder',
        icon: 'mdi:email-outline'
      }
    ],
    [
      TwoFactorAuthProviderType.BACKUP_CODE, {
        name: 'security.2fa.provider.backup_code',
        description: 'login.backup-code-auth-description',
        placeholder: 'login.backup-code-auth-placeholder',
        icon: 'mdi:lock-outline'
      }
    ]
  ]
);

export const twoFactorAuthProvidersEnterCodeCardTranslate = new Map<TwoFactorAuthProviderType, Omit<TwoFactorAuthProviderData, 'activatedHint'>>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
      name: 'login.enable-authenticator-app',
      description: 'login.enable-authenticator-app-description'
    }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
      name: 'login.enable-authenticator-sms',
      description: 'login.enable-authenticator-sms-description'
    }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
      name: 'login.enable-authenticator-email',
      description: 'login.enable-authenticator-email-description'
    }
    ],
    [
      TwoFactorAuthProviderType.BACKUP_CODE, {
      name: 'security.2fa.provider.backup_code',
      description: 'login.backup-code-auth-description'
    }
    ]
  ]
);

export const twoFactorAuthProvidersSuccessCardTranslate = new Map<TwoFactorAuthProviderType, Omit<TwoFactorAuthProviderData, 'activatedHint'>>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
      name: 'login.authenticator-app-success',
      description: 'login.authenticator-app-success-description'
    }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
      name: 'login.authenticator-sms-success',
      description: 'login.authenticator-sms-success-description'
    }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
      name: 'login.authenticator-email-success',
      description: 'login.authenticator-email-success-description'
    }
    ],
    [
      TwoFactorAuthProviderType.BACKUP_CODE, {
      name: 'login.authenticator-backup-code-success',
      description: 'login.authenticator-backup-code-success-description'
    }
    ]
  ]
);
