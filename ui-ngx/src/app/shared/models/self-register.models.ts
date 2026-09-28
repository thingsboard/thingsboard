// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { GroupPermission } from '@shared/models/group-permission.models';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { CustomMenuId } from '@shared/models/id/custom-menu-id';
import { NotificationTargetId } from '@shared/models/id/notification-target-id';
import { DomainId } from '@shared/models/id/domain-id';

export type CaptchaVersion = 'v2' | 'v3' | 'enterprise';

export enum SelfRegistrationType {
  WEB = 'WEB',
  MOBILE = 'MOBILE'
}

export enum SignUpFieldId {
  EMAIL = 'EMAIL',
  PASSWORD = 'PASSWORD',
  REPEAT_PASSWORD = 'REPEAT_PASSWORD',
  FIRST_NAME = 'FIRST_NAME',
  LAST_NAME = 'LAST_NAME',
  PHONE = 'PHONE',
  COUNTRY = 'COUNTRY',
  CITY = 'CITY',
  STATE = 'STATE',
  ZIP = 'ZIP',
  ADDRESS = 'ADDRESS',
  ADDRESS2 = 'ADDRESS2'
}

export interface SignUpField {
  id: SignUpFieldId,
  label: string;
  required: boolean;
}

export const SignUpFieldMap = new Map<SignUpFieldId, SignUpField>([
  [
    SignUpFieldId.EMAIL,
    {
      id: SignUpFieldId.EMAIL,
      label: 'user.email',
      required: true
    }
  ],
  [
    SignUpFieldId.PASSWORD,
    {
      id: SignUpFieldId.PASSWORD,
      label: 'common.password',
      required: true
    }
  ],
  [
    SignUpFieldId.REPEAT_PASSWORD,
    {
      id: SignUpFieldId.REPEAT_PASSWORD,
      label: 'common.repeat-password',
      required: true
    }
  ],
  [
    SignUpFieldId.FIRST_NAME,
    {
      id: SignUpFieldId.FIRST_NAME,
      label: 'user.first-name',
      required: false
    }
  ],
  [
    SignUpFieldId.LAST_NAME,
    {
      id: SignUpFieldId.LAST_NAME,
      label: 'user.last-name',
      required: false
    }
  ],
  [
    SignUpFieldId.PHONE,
    {
      id: SignUpFieldId.PHONE,
      label: 'contact.phone',
      required: false
    }
  ],
  [
    SignUpFieldId.PHONE,
    {
      id: SignUpFieldId.PHONE,
      label: 'contact.phone',
      required: false
    }
  ],
  [
    SignUpFieldId.COUNTRY,
    {
      id: SignUpFieldId.COUNTRY,
      label: 'contact.country',
      required: false
    }
  ],
  [
    SignUpFieldId.CITY,
    {
      id: SignUpFieldId.CITY,
      label: 'contact.city',
      required: false
    }
  ],
  [
    SignUpFieldId.STATE,
    {
      id: SignUpFieldId.STATE,
      label: 'contact.state',
      required: false
    }
  ],
  [
    SignUpFieldId.ZIP,
    {
      id: SignUpFieldId.ZIP,
      label: 'contact.postal-code',
      required: false
    }
  ],
  [
    SignUpFieldId.ADDRESS,
    {
      id: SignUpFieldId.ADDRESS,
      label: 'contact.address',
      required: false
    }
  ],
  [
    SignUpFieldId.ADDRESS2,
    {
      id: SignUpFieldId.ADDRESS2,
      label: 'contact.address2',
      required: false
    }
  ]
])

export const defaultSignUpFields: SignUpFieldId[] =
  [SignUpFieldId.FIRST_NAME, SignUpFieldId.LAST_NAME, SignUpFieldId.EMAIL, SignUpFieldId.PASSWORD, SignUpFieldId.REPEAT_PASSWORD];

export const alwaysRequiredSignUpFields: SignUpFieldId[] =
  [SignUpFieldId.EMAIL, SignUpFieldId.PASSWORD, SignUpFieldId.REPEAT_PASSWORD];

export interface SignUpSelfRegistrationParams {
  title?: string;
  captcha?: CaptchaParams;
  signUpFields: Array<SignUpField>;
  activate?: boolean;
  showPrivacyPolicy?: boolean;
  showTermsOfUse?: boolean;
}

export interface WebSelfRegistrationParams extends AbstractSelfRegistrationParams {
  domainId?: DomainId;
}

export interface AbstractSelfRegistrationParams {
  enabled: boolean;
  title: string;
  captcha?: CaptchaParams;
  signUpFields: Array<SignUpField>;
  showPrivacyPolicy: boolean;
  showTermsOfUse: boolean;
  privacyPolicy?: string;
  termsOfUse?: string;
  notificationRecipient: NotificationTargetId;
  customerTitlePrefix: string;
  customerGroupId: EntityGroupId;
  permissions: GroupPermission[];
  defaultDashboard: DefaultDashboardParams;
  homeDashboard: HomeDashboardParams;
  customMenuId?: CustomMenuId;
  type: SelfRegistrationType;
}

export interface SignUpField {
  id: SignUpFieldId;
  label: string;
  required: boolean;
}

interface HomeDashboardParams {
  id: string;
  hideToolbar: boolean;
}

export interface CaptchaParams {
  siteKey?: string;
  version: CaptchaVersion;
  logActionName?: string;
  secretKey?: string;
  projectId?: string;
  androidKey?: string;
  iosKey?: string;
  serviceAccountCredentials?: string;
  serviceAccountCredentialsFileName?: string;
}

interface DefaultDashboardParams {
  id: string;
  fullscreen: boolean;
}

export interface MobileSelfRegistrationParams extends AbstractSelfRegistrationParams {
  redirect: MobileRedirectParams;
}

interface MobileRedirectParams {
  scheme: string;
  host: string;
}
