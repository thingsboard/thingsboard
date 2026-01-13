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

import { Type } from '@angular/core';
import { TwoFactorAuthProviderType } from '@shared/models/two-factor-auth.models';
import { TotpAuthDialogComponent } from './totp-auth-dialog.component';
import { SMSAuthDialogComponent } from './sms-auth-dialog.component';
import { EmailAuthDialogComponent } from './email-auth-dialog.component';
import {
  BackupCodeAuthDialogComponent
} from '@home/pages/security/authentication-dialog/backup-code-auth-dialog.component';

export const authenticationDialogMap = new Map<TwoFactorAuthProviderType, Type<any>>(
  [
    [TwoFactorAuthProviderType.TOTP, TotpAuthDialogComponent],
    [TwoFactorAuthProviderType.SMS, SMSAuthDialogComponent],
    [TwoFactorAuthProviderType.EMAIL, EmailAuthDialogComponent],
    [TwoFactorAuthProviderType.BACKUP_CODE, BackupCodeAuthDialogComponent]
  ]
);
