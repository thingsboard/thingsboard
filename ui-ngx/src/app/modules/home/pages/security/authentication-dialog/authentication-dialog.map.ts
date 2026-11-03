// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
