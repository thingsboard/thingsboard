///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { ValidatorFn } from '@angular/forms';
import { isNotEmptyStr, isNumber } from '@core/utils';
import { VersionCreateConfig } from '@shared/models/vc.models';
import { HasUUID } from '@shared/models/id/has-uuid';

export const smtpPortPattern: RegExp = /^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$/;

export interface AdminSettings<T> {
  key: string;
  jsonValue: T;
}

export enum SmtpProtocol {
  SMTP = 'smtp',
  SMTPS = 'smtps'
}

export interface MailServerSettings {
  showChangePassword?: boolean;
  mailFrom: string;
  smtpProtocol: SmtpProtocol;
  smtpHost: string;
  smtpPort: number;
  timeout: number;
  enableTls: boolean;
  tlsVersion: string;
  username: string;
  changePassword?: boolean;
  password?: string;
  enableProxy: boolean;
  proxyHost: string;
  proxyPort: number;
  proxyUser: string;
  proxyPassword: string;
  enableOauth2: boolean;
  providerId?: string;
  clientId?: string;
  clientSecret?: string;
  providerTenantId?: string;
  authUri?: string;
  tokenUri?: string;
  scope?: Array<string>;
  redirectUri?: string;
  tokenGenerated?: boolean;
}

export enum MailServerOauth2Provider {
  OFFICE_365 = 'OFFICE_365',
  CUSTOM = 'CUSTOM'
}

export interface MailConfigTemplate {
  id: HasUUID;
  createdTime: number;
  name: string;
  providerId: string;
  helpLink: string;
  scope: Array<string>;
  accessTokenUri: string;
  authorizationUri: string;
  enableTls: boolean;
  tlsVersion: string;
  smtpProtocol: SmtpProtocol;
  smtpHost: string;
  smtpPort: number;
  timeout: number;
  additionalInfo: any;
}

export interface GeneralSettings {
  baseUrl: string;
}

export type DeviceConnectivityProtocol = 'http' | 'https' | 'mqtt' | 'mqtts' | 'coap' | 'coaps';

export interface DeviceConnectivityInfo {
  enabled: boolean;
  host: string;
  port: number;
}

export type DeviceConnectivitySettings = Record<DeviceConnectivityProtocol, DeviceConnectivityInfo>;

export interface UserPasswordPolicy {
  minimumLength: number;
  maximumLength: number;
  minimumUppercaseLetters: number;
  minimumLowercaseLetters: number;
  minimumDigits: number;
  minimumSpecialCharacters: number;
  passwordExpirationPeriodDays: number;
  allowWhitespaces: boolean;
  forceUserToResetPasswordIfNotValid: boolean;
}

export interface SecuritySettings {
  passwordPolicy: UserPasswordPolicy;
  maxFailedLoginAttempts: number;
  userLockoutNotificationEmail: string;
  mobileSecretKeyLength: number;
  userActivationTokenTtl: number;
  passwordResetTokenTtl: number;
}

export interface JwtSettings {
  tokenIssuer: string;
  tokenSigningKey: string;
  tokenExpirationTime: number;
  refreshTokenExpTime: number;
}

export interface UpdateMessage {
  updateAvailable: boolean;
  currentVersion: string;
  latestVersion: string;
  upgradeInstructionsUrl: string;
  currentVersionReleaseNotesUrl: string;
  latestVersionReleaseNotesUrl: string;
}

export const phoneNumberPattern = /^\+[1-9]\d{1,14}$/;
export const phoneNumberPatternTwilio = /^\+[1-9]\d{1,14}$|^(MG|PN).*$/;

export enum SmsProviderType {
  AWS_SNS = 'AWS_SNS',
  TWILIO = 'TWILIO',
  SMPP = 'SMPP'
}

export const smsProviderTypeTranslationMap = new Map<SmsProviderType, string>(
  [
    [SmsProviderType.AWS_SNS, 'admin.sms-provider-type-aws-sns'],
    [SmsProviderType.TWILIO, 'admin.sms-provider-type-twilio'],
    [SmsProviderType.SMPP, 'admin.sms-provider-type-smpp']
  ]
);

export interface AwsSnsSmsProviderConfiguration {
  accessKeyId?: string;
  secretAccessKey?: string;
  region?: string;
}

export interface TwilioSmsProviderConfiguration {
  accountSid?: string;
  accountToken?: string;
  numberFrom?: string;
}

export interface SmppSmsProviderConfiguration {
  protocolVersion: number;
  host: string;
  port: number;
  systemId: string;
  password: string;
  systemType?: string;
  bindType?: string;
  serviceType?: string;
  sourceAddress?: string;
  sourceTon?: number;
  sourceNpi?: number;
  destinationTon?: number;
  destinationNpi?: number;
  addressRange?: string;
  codingScheme?: number;
}

export const smppVersions = [
  {value: 3.3},
  {value: 3.4}
];

export enum BindTypes {
  TX = 'TX',
  RX = 'RX',
  TRX = 'TRX'
}

export const bindTypesTranslationMap = new Map<BindTypes, string>([
  [BindTypes.TX, 'admin.smpp-provider.bind-type-tx'],
  [BindTypes.RX, 'admin.smpp-provider.bind-type-rx'],
  [BindTypes.TRX, 'admin.smpp-provider.bind-type-trx']
]);

export enum TypeOfNumber {
  Unknown = 'Unknown',
  International = 'International',
  National = 'National',
  NetworkSpecific = 'NetworkSpecific',
  SubscriberNumber = 'SubscriberNumber',
  Alphanumeric = 'Alphanumeric',
  Abbreviated = 'Abbreviated'
}

export interface TypeDescriptor {
  name: string;
  value: number;
}

export const typeOfNumberMap = new Map<TypeOfNumber, TypeDescriptor>([
  [TypeOfNumber.Unknown, {
    name: 'admin.smpp-provider.ton-unknown',
    value: 0
  }],
  [TypeOfNumber.International, {
    name: 'admin.smpp-provider.ton-international',
    value: 1
  }],
  [TypeOfNumber.National, {
    name: 'admin.smpp-provider.ton-national',
    value: 2
  }],
  [TypeOfNumber.NetworkSpecific, {
    name: 'admin.smpp-provider.ton-network-specific',
    value: 3
  }],
  [TypeOfNumber.SubscriberNumber, {
    name: 'admin.smpp-provider.ton-subscriber-number',
    value: 4
  }],
  [TypeOfNumber.Alphanumeric, {
    name: 'admin.smpp-provider.ton-alphanumeric',
    value: 5
  }],
  [TypeOfNumber.Abbreviated, {
    name: 'admin.smpp-provider.ton-abbreviated',
    value: 6
  }],
]);

export enum NumberingPlanIdentification {
  Unknown = 'Unknown',
  ISDN = 'ISDN',
  DataNumberingPlan = 'DataNumberingPlan',
  TelexNumberingPlan = 'TelexNumberingPlan',
  LandMobile = 'LandMobile',
  NationalNumberingPlan = 'NationalNumberingPlan',
  PrivateNumberingPlan = 'PrivateNumberingPlan',
  ERMESNumberingPlan = 'ERMESNumberingPlan',
  Internet = 'Internet',
  WAPClientId = 'WAPClientId',
}

export const numberingPlanIdentificationMap = new Map<NumberingPlanIdentification, TypeDescriptor>([
  [NumberingPlanIdentification.Unknown, {
    name: 'admin.smpp-provider.npi-unknown',
    value: 0
  }],
  [NumberingPlanIdentification.ISDN, {
    name: 'admin.smpp-provider.npi-isdn',
    value: 1
  }],
  [NumberingPlanIdentification.DataNumberingPlan, {
    name: 'admin.smpp-provider.npi-data-numbering-plan',
    value: 3
  }],
  [NumberingPlanIdentification.TelexNumberingPlan, {
    name: 'admin.smpp-provider.npi-telex-numbering-plan',
    value: 4
  }],
  [NumberingPlanIdentification.LandMobile, {
    name: 'admin.smpp-provider.npi-land-mobile',
    value: 5
  }],
  [NumberingPlanIdentification.NationalNumberingPlan, {
    name: 'admin.smpp-provider.npi-national-numbering-plan',
    value: 8
  }],
  [NumberingPlanIdentification.PrivateNumberingPlan, {
    name: 'admin.smpp-provider.npi-private-numbering-plan',
    value: 9
  }],
  [NumberingPlanIdentification.ERMESNumberingPlan, {
    name: 'admin.smpp-provider.npi-ermes-numbering-plan',
    value: 10
  }],
  [NumberingPlanIdentification.Internet, {
    name: 'admin.smpp-provider.npi-internet',
    value: 13
  }],
  [NumberingPlanIdentification.WAPClientId, {
    name: 'admin.smpp-provider.npi-wap-client-id',
    value: 18
  }],
]);

export enum CodingSchemes {
  SMSC = 'SMSC',
  IA5 = 'IA5',
  OctetUnspecified2 = 'OctetUnspecified2',
  Latin1 = 'Latin1',
  OctetUnspecified4 = 'OctetUnspecified4',
  JIS = 'JIS',
  Cyrillic = 'Cyrillic',
  LatinHebrew = 'LatinHebrew',
  UCS2UTF16 = 'UCS2UTF16',
  PictogramEncoding = 'PictogramEncoding',
  MusicCodes = 'MusicCodes',
  ExtendedKanjiJIS = 'ExtendedKanjiJIS',
  KoreanGraphicCharacterSet = 'KoreanGraphicCharacterSet',
}

export const codingSchemesMap = new Map<CodingSchemes, TypeDescriptor>([
  [CodingSchemes.SMSC, {
    name: 'admin.smpp-provider.scheme-smsc',
    value: 0
  }],
  [CodingSchemes.IA5, {
    name: 'admin.smpp-provider.scheme-ia5',
    value: 1
  }],
  [CodingSchemes.OctetUnspecified2, {
    name: 'admin.smpp-provider.scheme-octet-unspecified-2',
    value: 2
  }],
  [CodingSchemes.Latin1, {
    name: 'admin.smpp-provider.scheme-latin-1',
    value: 3
  }],
  [CodingSchemes.OctetUnspecified4, {
    name: 'admin.smpp-provider.scheme-octet-unspecified-4',
    value: 4
  }],
  [CodingSchemes.JIS, {
    name: 'admin.smpp-provider.scheme-jis',
    value: 5
  }],
  [CodingSchemes.Cyrillic, {
    name: 'admin.smpp-provider.scheme-cyrillic',
    value: 6
  }],
  [CodingSchemes.LatinHebrew, {
    name: 'admin.smpp-provider.scheme-latin-hebrew',
    value: 7
  }],
  [CodingSchemes.UCS2UTF16, {
    name: 'admin.smpp-provider.scheme-ucs-utf',
    value: 8
  }],
  [CodingSchemes.PictogramEncoding, {
    name: 'admin.smpp-provider.scheme-pictogram-encoding',
    value: 9
  }],
  [CodingSchemes.MusicCodes, {
    name: 'admin.smpp-provider.scheme-music-codes',
    value: 10
  }],
  [CodingSchemes.ExtendedKanjiJIS, {
    name: 'admin.smpp-provider.scheme-extended-kanji-jis',
    value: 13
  }],
  [CodingSchemes.KoreanGraphicCharacterSet, {
    name: 'admin.smpp-provider.scheme-korean-graphic-character-set',
    value: 14
  }],
]);

export type SmsProviderConfigurations =
  Partial<SmppSmsProviderConfiguration> & AwsSnsSmsProviderConfiguration & TwilioSmsProviderConfiguration;

export interface SmsProviderConfiguration extends SmsProviderConfigurations {
  type: SmsProviderType;
}

export function smsProviderConfigurationValidator(required: boolean): ValidatorFn {
  return control => {
    const configuration: SmsProviderConfiguration = control.value;
    let errors = null;
    if (required) {
      let valid = false;
      if (configuration && configuration.type) {
        switch (configuration.type) {
          case SmsProviderType.AWS_SNS:
            const awsSnsConfiguration: AwsSnsSmsProviderConfiguration = configuration;
            valid = isNotEmptyStr(awsSnsConfiguration.accessKeyId) && isNotEmptyStr(awsSnsConfiguration.secretAccessKey)
              && isNotEmptyStr(awsSnsConfiguration.region);
            break;
          case SmsProviderType.TWILIO:
            const twilioConfiguration: TwilioSmsProviderConfiguration = configuration;
            valid = isNotEmptyStr(twilioConfiguration.numberFrom) && isNotEmptyStr(twilioConfiguration.accountSid)
              && isNotEmptyStr(twilioConfiguration.accountToken);
            break;
          case SmsProviderType.SMPP:
            const smppConfiguration = configuration as SmppSmsProviderConfiguration;
            valid = isNotEmptyStr(smppConfiguration.host) && isNumber(smppConfiguration.port)
              && isNotEmptyStr(smppConfiguration.systemId) && isNotEmptyStr(smppConfiguration.password);
            break;
        }
      }
      if (!valid) {
        errors = {
          invalid: true
        };
      }
    }
    return errors;
  };
}

export interface TestSmsRequest {
  providerConfiguration: SmsProviderConfiguration;
  numberTo: string;
  message: string;
}

export function createSmsProviderConfiguration(type: SmsProviderType): SmsProviderConfiguration {
  let smsProviderConfiguration: SmsProviderConfiguration;
  if (type) {
    switch (type) {
      case SmsProviderType.AWS_SNS:
        const awsSnsSmsProviderConfiguration: AwsSnsSmsProviderConfiguration = {
          accessKeyId: '',
          secretAccessKey: '',
          region: 'us-east-1'
        };
        smsProviderConfiguration = {...awsSnsSmsProviderConfiguration, type: SmsProviderType.AWS_SNS};
        break;
      case SmsProviderType.TWILIO:
        const twilioSmsProviderConfiguration: TwilioSmsProviderConfiguration = {
          numberFrom: '',
          accountSid: '',
          accountToken: ''
        };
        smsProviderConfiguration = {...twilioSmsProviderConfiguration, type: SmsProviderType.TWILIO};
        break;
      case SmsProviderType.SMPP:
        const smppSmsProviderConfiguration: SmppSmsProviderConfiguration = {
          protocolVersion: 3.3,
          host: '',
          port: null,
          systemId: '',
          password: '',
          systemType: '',
          bindType: 'TX',
          serviceType: '',
          sourceAddress: '',
          sourceTon: 5,
          sourceNpi: 0,
          destinationTon: 5,
          destinationNpi: 0,
          addressRange: '',
          codingScheme: 0
        };
        smsProviderConfiguration = {...smppSmsProviderConfiguration, type: SmsProviderType.SMPP};
        break;
    }
  }
  return smsProviderConfiguration;
}

export enum RepositoryAuthMethod {
  USERNAME_PASSWORD = 'USERNAME_PASSWORD',
  PRIVATE_KEY = 'PRIVATE_KEY'
}

export const repositoryAuthMethodTranslationMap = new Map<RepositoryAuthMethod, string>([
  [RepositoryAuthMethod.USERNAME_PASSWORD, 'admin.auth-method-username-password'],
  [RepositoryAuthMethod.PRIVATE_KEY, 'admin.auth-method-private-key']
]);

export interface RepositorySettings {
  repositoryUri: string;
  defaultBranch: string;
  showMergeCommits: boolean;
  authMethod: RepositoryAuthMethod;
  username: string;
  password: string;
  privateKeyFileName: string;
  privateKey: string;
  privateKeyPassword: string;
}

export interface RepositorySettingsInfo {
  configured: boolean;
  readOnly: boolean;
}

export interface AutoVersionCreateConfig extends VersionCreateConfig {
  branch: string;
}

export type AutoCommitSettings = {[entityType: string]: AutoVersionCreateConfig};

export interface FeaturesInfo {
  emailEnabled: boolean;
  smsEnabled: boolean;
  notificationEnabled: boolean;
  oauthEnabled: boolean;
  twoFaEnabled: boolean;
}
