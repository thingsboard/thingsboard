// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export enum Authority {
  SYS_ADMIN = 'SYS_ADMIN',
  TENANT_ADMIN = 'TENANT_ADMIN',
  CUSTOMER_USER = 'CUSTOMER_USER',
  REFRESH_TOKEN = 'REFRESH_TOKEN',
  ANONYMOUS = 'ANONYMOUS',
  PRE_VERIFICATION_TOKEN = 'PRE_VERIFICATION_TOKEN'
}
