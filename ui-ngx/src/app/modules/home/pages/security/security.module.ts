// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SecurityComponent } from './security.component';
import { SharedModule } from '@shared/shared.module';
import { SecurityRoutingModule } from './security-routing.module';
import { TotpAuthDialogComponent } from './authentication-dialog/totp-auth-dialog.component';
import { SMSAuthDialogComponent } from '@home/pages/security/authentication-dialog/sms-auth-dialog.component';
import { EmailAuthDialogComponent } from '@home/pages/security/authentication-dialog/email-auth-dialog.component';
import {
  BackupCodeAuthDialogComponent
} from '@home/pages/security/authentication-dialog/backup-code-auth-dialog.component';

@NgModule({
  declarations: [
    SecurityComponent,
    TotpAuthDialogComponent,
    SMSAuthDialogComponent,
    EmailAuthDialogComponent,
    BackupCodeAuthDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SecurityRoutingModule
  ]
})
export class SecurityModule { }
