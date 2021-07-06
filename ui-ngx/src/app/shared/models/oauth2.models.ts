///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { HasUUID } from '@shared/models/id/has-uuid';

export interface OAuth2Info {
  enabled: boolean;
  oauth2ParamsInfos: OAuth2ParamsInfo[];
}

export interface OAuth2ParamsInfo {
  clientRegistrations: OAuth2RegistrationInfo[];
  domainInfos: OAuth2DomainInfo[];
  mobileInfos: OAuth2MobileInfo[];
}

export interface OAuth2DomainInfo {
  name: string;
  scheme: DomainSchema;
}

export interface OAuth2MobileInfo {
  pkgName: string;
  appSecret: string;
}

export enum DomainSchema{
  HTTP = 'HTTP',
  HTTPS = 'HTTPS',
  MIXED = 'MIXED'
}

export const domainSchemaTranslations = new Map<DomainSchema, string>(
  [
    [DomainSchema.HTTP, 'admin.oauth2.domain-schema-http'],
    [DomainSchema.HTTPS, 'admin.oauth2.domain-schema-https'],
    [DomainSchema.MIXED, 'admin.oauth2.domain-schema-mixed']
  ]
);

export enum MapperConfigType{
  BASIC = 'BASIC',
  CUSTOM = 'CUSTOM',
  GITHUB = 'GITHUB',
  APPLE = 'APPLE'
}

export enum TenantNameStrategy{
  DOMAIN = 'DOMAIN',
  EMAIL = 'EMAIL',
  CUSTOM = 'CUSTOM'
}

export enum PlatformType {
  WEB = 'WEB',
  ANDROID = 'ANDROID',
  IOS = 'IOS'
}

export const platformTypeTranslations = new Map<PlatformType, string>(
  [
    [PlatformType.WEB, 'admin.oauth2.platform-web'],
    [PlatformType.ANDROID, 'admin.oauth2.platform-android'],
    [PlatformType.IOS, 'admin.oauth2.platform-ios']
  ]
);

export interface OAuth2ClientRegistrationTemplate extends OAuth2RegistrationInfo{
  comment: string;
  createdTime: number;
  helpLink: string;
  name: string;
  providerId: string;
  id: HasUUID;
}

export interface OAuth2RegistrationInfo {
  loginButtonLabel: string;
  loginButtonIcon: string;
  clientId: string;
  clientSecret: string;
  accessTokenUri: string;
  authorizationUri: string;
  scope: string[];
  platforms: PlatformType[];
  jwkSetUri?: string;
  userInfoUri: string;
  clientAuthenticationMethod: ClientAuthenticationMethod;
  userNameAttributeName: string;
  mapperConfig: MapperConfig;
  additionalInfo: string;
}

export enum ClientAuthenticationMethod {
  BASIC = 'BASIC',
  POST = 'POST'
}

export interface MapperConfig {
  allowUserCreation: boolean;
  activateUser: boolean;
  type: MapperConfigType;
  basic?: MapperConfigBasic;
  custom?: MapperConfigCustom;
}

export interface MapperConfigBasic {
  emailAttributeKey: string;
  firstNameAttributeKey?: string;
  lastNameAttributeKey?: string;
  tenantNameStrategy: TenantNameStrategy;
  tenantNamePattern?: string;
  customerNamePattern?: string;
  defaultDashboardName?: string;
  alwaysFullScreen?: boolean;
}

export interface MapperConfigCustom {
  url: string;
  username?: string;
  password?: string;
}

export interface OAuth2ClientInfo {
  name: string;
  icon?: string;
  url: string;
}
