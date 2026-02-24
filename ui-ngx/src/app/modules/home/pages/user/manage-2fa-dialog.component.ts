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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, FormGroupDirective, NgForm } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DialogService, TwoFactorAuthenticationService } from '@core/public-api';
import { AccountTwoFaSettings, twoFactorAuthManagementData, TwoFactorAuthProviderType } from '@shared/models/two-factor-auth.models';
import { TranslateService } from '@ngx-translate/core';

export interface User2FADialogData {
  userId?: string;
}

@Component({
  selector: 'manage-2fa-dialog',
  templateUrl: './manage-2fa-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: Manage2FADialogComponent}],
  styleUrls: ['./manage-2fa-dialog.component.scss'],
  standalone: false
})
export class Manage2FADialogComponent extends 
  DialogComponent<Manage2FADialogComponent, User2FADialogData> implements OnInit, ErrorStateMatcher {

  loading = true;
  private userSettings?: AccountTwoFaSettings;
  providersData = twoFactorAuthManagementData;

  twoFaProviders: TwoFactorAuthProviderType[] = [];

  private submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: User2FADialogData,
              private dialogService: DialogService,
              private translate: TranslateService,
              private twoFaService: TwoFactorAuthenticationService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<Manage2FADialogComponent, User2FADialogData>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    forkJoin({
      securitySettings: this.twoFaService.getTwoFaSettings(),
      userSettings: this.twoFaService.getUsersTwoFaSettings(this.data.userId)
    }).subscribe({
      next: ({securitySettings, userSettings}) => {
        this.userSettings = userSettings;
        console.log(securitySettings);
        if (securitySettings == null || securitySettings.providers.length === 0) {
          this.dialogService.confirm(
            this.translate.instant('security.2fa.2fa'),
            this.translate.instant('admin.2fa.no-2fa')
          ).subscribe(() => {
            this.cancel();
          });
        }
        securitySettings.providers.forEach((provider) => {
          this.twoFaProviders.push(provider.providerType); 
        });
      },
      error: () => {
        this.cancel();
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  resetTwoFa(provider: TwoFactorAuthProviderType) {
    const providerName = this.translate.instant(`security.2fa.provider.${provider.toLowerCase()}`);
    this.dialogService.confirm(
      this.translate.instant('security.2fa.disable-2fa-provider-title', {name: providerName}),
      this.translate.instant('security.2fa.disable-user-2fa-provider-text', {name: providerName}),
    ).subscribe(res => {
      if (res) {
        this.loading = true;
        this.twoFaService.deleteUserTwoFaAccountConfig(provider, this.data.userId).subscribe({
          next: (settings) => {
            this.userSettings = settings;
          },
          complete: () => {
            this.loading = false;
          }
        })
      }
    });
  }

  isEnabled(provider: TwoFactorAuthProviderType) {
    return this.userSettings?.configs[provider] !== undefined;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}