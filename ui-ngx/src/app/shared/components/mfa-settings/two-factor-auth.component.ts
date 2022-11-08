///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, forwardRef, Input } from '@angular/core';
import { FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntil, tap } from 'rxjs/operators';
import {
  AccountTwoFaSettingProviders,
  AccountTwoFaSettings,
  BackupCodeTwoFactorAuthAccountConfig,
  EmailTwoFactorAuthAccountConfig,
  SmsTwoFactorAuthAccountConfig,
  twoFactorAuthProvidersData,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { Observable, of, Subject } from 'rxjs';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { authenticationDialogMap } from '@home/pages/security/authentication-dialog/authentication-dialog.map';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { User } from '@shared/models/user.model';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-two-factor-auth-component',
  templateUrl: './two-factor-auth.component.html',
  styleUrls: ['../../../modules/home/pages/security/security.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TwoFactorAuthComponent),
    multi: true
  }]
})
export class TwoFactorAuthComponent {

  twoFactorFormGroup: FormGroup;

  private readonly destroy$ = new Subject<void>();
  private accountConfig: AccountTwoFaSettingProviders;

  @Input()
  set setForceTwoFa(value: boolean) {
    if (isDefinedAndNotNull(value) && value) {
      this.isForceTwoFa = value;
    }
  }
  isForceTwoFa = false;

  user: User;

  allowTwoFactorProviders: TwoFactorAuthProviderType[] = [];
  providersData = twoFactorAuthProvidersData;
  twoFactorAuthProviderType = TwoFactorAuthProviderType;
  useByDefault: TwoFactorAuthProviderType = null;
  activeSingleProvider = true;

  constructor(private store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              private translate: TranslateService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              private authService: AuthService,
              private fb: FormBuilder) {
    this.buildTwoFactorForm();
    this.user = this.route.snapshot.data.user;
    this.twoFactorLoad(this.route.snapshot.data.providers);
  }

  private buildTwoFactorForm() {
    this.twoFactorFormGroup = this.fb.group({
      TOTP: [false],
      SMS: [false],
      EMAIL: [false],
      BACKUP_CODE: [{value: false, disabled: true}]
    });
    this.twoFactorFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: {TwoFactorAuthProviderType: boolean}) => {
      const formActiveValue = Object.keys(value).filter(item => value[item] && item !== TwoFactorAuthProviderType.BACKUP_CODE);
      this.activeSingleProvider = formActiveValue.length < 2;
      if (formActiveValue.length) {
        this.twoFactorFormGroup.get('BACKUP_CODE').enable({emitEvent: false});
      } else {
        this.twoFactorFormGroup.get('BACKUP_CODE').disable({emitEvent: false});
      }
    });
  }

  private twoFactorLoad(providers: TwoFactorAuthProviderType[]) {
    if (providers.length) {
      this.twoFaService.getAccountTwoFaSettings().subscribe(data =>
        this.processTwoFactorAuthConfig(data)
      );
      Object.values(TwoFactorAuthProviderType).forEach(type => {
        if (providers.includes(type)) {
          this.allowTwoFactorProviders.push(type);
        }
      });
    }
  }

  private processTwoFactorAuthConfig(setting: AccountTwoFaSettings) {
    this.accountConfig = setting?.configs || {};
    Object.values(TwoFactorAuthProviderType).forEach(provider => {
      if (this.accountConfig[provider]) {
        this.twoFactorFormGroup.get(provider).setValue(true);
        if (this.accountConfig[provider].useByDefault) {
          this.useByDefault = provider;
        }
      } else {
        this.twoFactorFormGroup.get(provider).setValue(false);
      }
    });
  }

  confirm2FAChange(event: MouseEvent, provider: TwoFactorAuthProviderType) {
    event.stopPropagation();
    event.preventDefault();
    if (this.twoFactorFormGroup.get(provider).disabled) {
      return;
    }
    if (this.twoFactorFormGroup.get(provider).value) {
      const providerName = this.translate.instant(`security.2fa.provider.${provider.toLowerCase()}`);
      this.dialogService.confirm(
        this.translate.instant('security.2fa.disable-2fa-provider-title', {name: providerName}),
        this.translate.instant('security.2fa.disable-2fa-provider-text', {name: providerName}),
      ).subscribe(res => {
        if (res) {
          this.twoFactorFormGroup.disable({emitEvent: false});
          this.twoFaService.deleteTwoFaAccountConfig(provider)
            .pipe(tap(() => this.twoFactorFormGroup.enable({emitEvent: false})))
            .subscribe(data => this.processTwoFactorAuthConfig(data));
        }
      });
    } else {
      this.createdNewAuthConfig(provider);
    }
  }

  private createdNewAuthConfig(provider: TwoFactorAuthProviderType) {
    const dialogData = provider === TwoFactorAuthProviderType.EMAIL ? {email: this.user.email} : {};
    this.dialog.open(authenticationDialogMap.get(provider), {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: dialogData
    }).afterClosed().subscribe(res => {
      if (isDefinedAndNotNull(res)) {
        this.processTwoFactorAuthConfig(res);
      }
    });
  }

  changeDefaultProvider(event: MouseEvent, provider: TwoFactorAuthProviderType) {
    event.stopPropagation();
    event.preventDefault();
    if (this.useByDefault !== provider) {
      this.twoFactorFormGroup.disable({emitEvent: false});
      this.twoFaService.updateTwoFaAccountConfig(provider, true)
        .pipe(tap(() => this.twoFactorFormGroup.enable({emitEvent: false})))
        .subscribe(data => this.processTwoFactorAuthConfig(data));
    }
  }

  generateNewBackupCode() {
    const codeLeft = (this.accountConfig[TwoFactorAuthProviderType.BACKUP_CODE] as BackupCodeTwoFactorAuthAccountConfig).codesLeft;
    let subscription: Observable<boolean>;
    if (codeLeft) {
      subscription = this.dialogService.confirm(
        'Get new set of backup codes?',
        `If you get new backup codes, ${codeLeft} remaining codes you have left will be unusable.`,
        '',
        'Get new codes'
      );
    } else {
      subscription = of(true);
    }
    subscription.subscribe(res => {
      if (res) {
        this.twoFactorFormGroup.disable({emitEvent: false});
        this.twoFaService.deleteTwoFaAccountConfig(TwoFactorAuthProviderType.BACKUP_CODE)
          .pipe(tap(() => this.twoFactorFormGroup.enable({emitEvent: false})))
          .subscribe(() => this.createdNewAuthConfig(TwoFactorAuthProviderType.BACKUP_CODE));
      }
    });
  }

  providerDataInfo(provider: TwoFactorAuthProviderType) {
    const info = {info: null};
    const providerConfig = this.accountConfig[provider];
    if (isDefinedAndNotNull(providerConfig)) {
      switch (provider) {
        case TwoFactorAuthProviderType.EMAIL:
          info.info = (providerConfig as EmailTwoFactorAuthAccountConfig).email;
          break;
        case TwoFactorAuthProviderType.SMS:
          info.info = (providerConfig as SmsTwoFactorAuthAccountConfig).phoneNumber;
          break;
        case TwoFactorAuthProviderType.BACKUP_CODE:
          info.info = (providerConfig as BackupCodeTwoFactorAuthAccountConfig).codesLeft;
          break;
      }
    }
    return info;
  }

  save() {
    this.twoFaService.getTwoFaLogin().subscribe(
      value => this.authService.setUserFromJwtToken(value.token, value.refreshToken, true),
      error => this.store.dispatch(
        new ActionNotificationShow({
          message: error.error.message,
          type: 'error',
          duration: 1500,
          verticalPosition: 'top',
          horizontalPosition: 'right'
        }))
    );
  }

  trackByProvider(i: number, provider: TwoFactorAuthProviderType) {
    return provider;
  }
}
