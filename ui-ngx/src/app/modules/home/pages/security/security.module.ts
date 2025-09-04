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
