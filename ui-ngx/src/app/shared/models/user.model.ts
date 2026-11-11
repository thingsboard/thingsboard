// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { BaseData, GroupEntityInfo } from './base-data';
import { UserId } from './id/user-id';
import { CustomerId } from './id/customer-id';
import { Authority } from './authority.enum';
import { TenantId } from './id/tenant-id';
import { CustomMenuId } from '@shared/models/id/custom-menu-id';
import { UnitSystem } from '@shared/models/unit.models';

export interface User extends BaseData<UserId> {
  tenantId: TenantId;
  customerId: CustomerId;
  email: string;
  phone: string;
  authority: Authority;
  firstName: string;
  lastName: string;
  customMenuId?: CustomMenuId;
  additionalInfo: Partial<UserAdditionalInfo>;
}

export interface UserAdditionalInfo {
  userCredentialsEnabled: boolean;
  userActivated: boolean;
  description: string;
  defaultDashboardId: string;
  defaultDashboardFullscreen: boolean;
  homeDashboardId: string;
  homeDashboardHideToolbar: boolean;
  unitSystem: UnitSystem;
  lang: string;
  [key: string]: any;
}

export type UserInfo = User & GroupEntityInfo<UserId>;

export enum ActivationMethod {
  DISPLAY_ACTIVATION_LINK = 'DISPLAY_ACTIVATION_LINK',
  SEND_ACTIVATION_MAIL = 'SEND_ACTIVATION_MAIL'
}

export const activationMethodTranslations = new Map<ActivationMethod, string>(
  [
    [ActivationMethod.DISPLAY_ACTIVATION_LINK, 'user.display-activation-link'],
    [ActivationMethod.SEND_ACTIVATION_MAIL, 'user.send-activation-mail']
  ]
);

export interface ActivationLinkInfo {
  value: string;
  ttlMs: number;
}

export interface AuthUser {
  sub: string;
  scopes: string[];
  userId: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  tenantId: string;
  customerId: string;
  isPublic: boolean;
  authority: Authority;
}

export interface UserEmailInfo {
  id: UserId;
  email: string;
  firstName: string;
  lastName: string;
}
