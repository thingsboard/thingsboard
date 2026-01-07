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

import { OAuth2ClientId } from '@shared/models/id/oauth2-client-id';
import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { HasTenantId } from './entity.models';
import { DomainId } from './id/domain-id';
import { HasUUID } from '@shared/models/id/has-uuid';

export enum DomainSchema {
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

export enum PlatformType {
  WEB = 'WEB',
  ANDROID = 'ANDROID',
  IOS = 'IOS'
}

export interface OAuth2ClientRegistrationTemplate extends OAuth2RegistrationInfo {
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
  mapperConfig: OAuth2MapperConfig;
  additionalInfo: string;
}

export enum ClientAuthenticationMethod {
  NONE = 'NONE',
  BASIC = 'BASIC',
  POST = 'POST'
}

export interface Domain extends BaseData<DomainId>, HasTenantId {
  tenantId?: TenantId;
  name: string;
  oauth2Enabled: boolean;
  propagateToEdge: boolean;
}

export interface DomainInfo extends Domain {
  oauth2ClientInfos?: Array<OAuth2ClientInfo> | Array<string>;
}

export interface OAuth2Client extends BaseData<OAuth2ClientId>, HasTenantId {
  tenantId?: TenantId;
  title: string;
  mapperConfig: OAuth2MapperConfig;
  clientId: string;
  clientSecret: string;
  authorizationUri: string;
  accessTokenUri: string;
  scope: Array<string>;
  userInfoUri?: string;
  userNameAttributeName: string;
  jwkSetUri?: string;
  clientAuthenticationMethod: ClientAuthenticationMethod;
  loginButtonLabel: string;
  loginButtonIcon?: string;
  platforms?: Array<PlatformType>;
  additionalInfo: any;
}

export interface OAuth2MapperConfig {
  allowUserCreation: boolean;
  activateUser: boolean;
  type: MapperType;
  basic?: OAuth2BasicMapperConfig;
  custom?: OAuth2CustomMapperConfig
}

export enum MapperType {
  BASIC = 'BASIC',
  CUSTOM = 'CUSTOM',
  GITHUB = 'GITHUB',
  APPLE = 'APPLE'
}

export interface OAuth2BasicMapperConfig {
  emailAttributeKey?: string;
  firstNameAttributeKey?: string;
  lastNameAttributeKey?: string;
  tenantNameStrategy?: TenantNameStrategyType;
  tenantNamePattern?: string;
  customerNamePattern?: string;
  defaultDashboardName?: string;
  alwaysFullScreen?: boolean;
}

export enum TenantNameStrategyType {
  DOMAIN = 'DOMAIN',
  EMAIL = 'EMAIL',
  CUSTOM = 'CUSTOM'
}

export interface OAuth2CustomMapperConfig {
  url?: string;
  username?: string;
  password?: string;
  sendToken: boolean;
}

export const platformTypeTranslations = new Map<PlatformType, string>(
  [
    [PlatformType.WEB, 'admin.oauth2.platform-web'],
    [PlatformType.ANDROID, 'admin.oauth2.platform-android'],
    [PlatformType.IOS, 'admin.oauth2.platform-ios']
  ]
);

export interface OAuth2ClientInfo extends BaseData<OAuth2ClientId> {
  title: string;
  providerName: string;
  platforms?: Array<PlatformType>;
}

export interface OAuth2ClientLoginInfo {
  name: string;
  icon: string;
  url: string;
}

export function getProviderHelpLink(provider: Provider): string {
  if (providerHelpLinkMap.has(provider)) {
    return providerHelpLinkMap.get(provider);
  }
  return 'oauth2Settings';
}

export enum Provider {
  CUSTOM = 'Custom',
  FACEBOOK = 'Facebook',
  GOOGLE = 'Google',
  GITHUB = 'Github',
  APPLE = 'Apple'
}

const providerHelpLinkMap = new Map<Provider, string>(
  [
    [Provider.CUSTOM, 'oauth2Settings'],
    [Provider.APPLE, 'oauth2Apple'],
    [Provider.FACEBOOK, 'oauth2Facebook'],
    [Provider.GITHUB, 'oauth2Github'],
    [Provider.GOOGLE, 'oauth2Google'],
  ]
)
