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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { User } from '@shared/models/user.model';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AbstractControl,
  UntypedFormBuilder,
  UntypedFormGroup, FormGroupDirective,
  NgForm,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { ActivatedRoute } from '@angular/router';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DatePipe } from '@angular/common';
import { ClipboardService } from 'ngx-clipboard';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettingProviders,
  AccountTwoFaSettings,
  BackupCodeTwoFactorAuthAccountConfig,
  EmailTwoFactorAuthAccountConfig,
  SmsTwoFactorAuthAccountConfig,
  twoFactorAuthProvidersData,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { authenticationDialogMap } from '@home/pages/security/authentication-dialog/authentication-dialog.map';
import { takeUntil, tap } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { AuthService } from '@core/auth/auth.service';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { MatCheckboxChange } from '@angular/material/checkbox';
import {
  ApiKeysTableDialogComponent, ApiKeysTableDialogData
} from '@home/components/api-key/components/dialog/api-keys-table-dialog.component';

@Component({
  selector: 'tb-security',
  templateUrl: './security.component.html',
  styleUrls: ['./security.component.scss']
})
export class SecurityComponent extends PageComponent implements OnInit, OnDestroy {

  private readonly destroy$ = new Subject<void>();
  private accountConfig: AccountTwoFaSettingProviders;

  twoFactorAuth: UntypedFormGroup;
  changePassword: UntypedFormGroup;

  user: User;
  passwordPolicy: UserPasswordPolicy;

  allowTwoFactorProviders: TwoFactorAuthProviderType[] = [];
  providersData = twoFactorAuthProvidersData;
  twoFactorAuthProviderType = TwoFactorAuthProviderType;
  useByDefault: TwoFactorAuthProviderType = null;
  activeSingleProvider = true;

  get jwtToken(): string {
    return `Bearer ${localStorage.getItem('jwt_token')}`;
  }

  get jwtTokenExpiration(): string {
    return localStorage.getItem('jwt_token_expiration');
  }

  get expirationJwtData(): string {
    const expirationData = this.datePipe.transform(this.jwtTokenExpiration, 'yyyy-MM-dd HH:mm:ss');
    return this.translate.instant('profile.valid-till', { expirationData });
  }

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private translate: TranslateService,
              private twoFaService: TwoFactorAuthenticationService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              public fb: UntypedFormBuilder,
              private datePipe: DatePipe,
              private authService: AuthService,
              private clipboardService: ClipboardService) {
    super(store);
  }

  ngOnInit() {
    this.buildTwoFactorForm();
    this.user = this.route.snapshot.data.user;
    this.twoFactorLoad(this.route.snapshot.data.providers);
    this.buildChangePasswordForm();
    this.loadPasswordPolicy();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildTwoFactorForm() {
    this.twoFactorAuth = this.fb.group({
      TOTP: [false],
      SMS: [false],
      EMAIL: [false],
      BACKUP_CODE: [{value: false, disabled: true}]
    });
    this.twoFactorAuth.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: {TwoFactorAuthProviderType: boolean}) => {
      const formActiveValue = Object.keys(value).filter(item => value[item] && item !== TwoFactorAuthProviderType.BACKUP_CODE);
      this.activeSingleProvider = formActiveValue.length < 2;
      if (formActiveValue.length) {
        this.twoFactorAuth.get('BACKUP_CODE').enable({emitEvent: false});
      } else {
        this.twoFactorAuth.get('BACKUP_CODE').disable({emitEvent: false});
      }
    });
  }

  private twoFactorLoad(providers: TwoFactorAuthProviderType[]) {
    if (providers.length) {
      this.twoFaService.getAccountTwoFaSettings().subscribe(data => this.processTwoFactorAuthConfig(data));
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
        this.twoFactorAuth.get(provider).setValue(true);
        if (this.accountConfig[provider].useByDefault) {
          this.useByDefault = provider;
        }
      } else {
        this.twoFactorAuth.get(provider).setValue(false);
      }
    });
  }

  private buildChangePasswordForm() {
    this.changePassword = this.fb.group({
      currentPassword: [''],
      newPassword: ['', Validators.required],
      newPassword2: ['', this.samePasswordValidation(false, 'newPassword')]
    });
  }

  private loadPasswordPolicy() {
    this.authService.getUserPasswordPolicy().subscribe(policy => {
      this.passwordPolicy = policy;
      this.changePassword.get('newPassword').setValidators([
        this.passwordStrengthValidator(),
        this.samePasswordValidation(true, 'currentPassword'),
        Validators.required
      ]);
      this.changePassword.get('newPassword').updateValueAndValidity({emitEvent: false});
    });
  }

  private passwordStrengthValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value: string = control.value;
      const errors: any = {};

      if (this.passwordPolicy.minimumUppercaseLetters > 0 &&
        !new RegExp(`(?:.*?[A-Z]){${this.passwordPolicy.minimumUppercaseLetters}}`).test(value)) {
        errors.notUpperCase = true;
      }

      if (this.passwordPolicy.minimumLowercaseLetters > 0 &&
        !new RegExp(`(?:.*?[a-z]){${this.passwordPolicy.minimumLowercaseLetters}}`).test(value)) {
        errors.notLowerCase = true;
      }

      if (this.passwordPolicy.minimumDigits > 0
        && !new RegExp(`(?:.*?\\d){${this.passwordPolicy.minimumDigits}}`).test(value)) {
        errors.notNumeric = true;
      }

      if (this.passwordPolicy.minimumSpecialCharacters > 0 &&
        !new RegExp(`(?:.*?[\\W_]){${this.passwordPolicy.minimumSpecialCharacters}}`).test(value)) {
        errors.notSpecial = true;
      }

      if (!this.passwordPolicy.allowWhitespaces && /\s/.test(value)) {
        errors.hasWhitespaces = true;
      }

      if (this.passwordPolicy.minimumLength > 0 && value.length < this.passwordPolicy.minimumLength) {
        errors.minLength = true;
      }

      if (!value.length || this.passwordPolicy.maximumLength > 0 && value.length > this.passwordPolicy.maximumLength) {
        errors.maxLength = true;
      }

      return isEqual(errors, {}) ? null : errors;
    };
  }

  private samePasswordValidation(isSame: boolean, key: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value: string = control.value;
      const keyValue = control.parent?.value[key];

      if (isSame) {
        return value === keyValue ? {samePassword: true} : null;
      }
      return value !== keyValue ? {differencePassword: true} : null;
    };
  }

  trackByProvider(i: number, provider: TwoFactorAuthProviderType) {
    return provider;
  }

  copyToken() {
    if (+this.jwtTokenExpiration < Date.now()) {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('profile.tokenCopiedWarnMessage'),
        type: 'warn',
        duration: 1500,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
    } else {
      this.clipboardService.copyFromContent(this.jwtToken);
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('profile.tokenCopiedSuccessMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
    }
  }

  confirm2FAChange(event: MouseEvent, provider: TwoFactorAuthProviderType) {
    event.stopPropagation();
    event.preventDefault();
    if (this.twoFactorAuth.get(provider).disabled) {
      return;
    }
    if (this.twoFactorAuth.get(provider).value) {
      const providerName = this.translate.instant(`security.2fa.provider.${provider.toLowerCase()}`);
      this.dialogService.confirm(
        this.translate.instant('security.2fa.disable-2fa-provider-title', {name: providerName}),
        this.translate.instant('security.2fa.disable-2fa-provider-text', {name: providerName}),
      ).subscribe(res => {
        if (res) {
          this.twoFactorAuth.disable({emitEvent: false});
          this.twoFaService.deleteTwoFaAccountConfig(provider)
            .pipe(tap(() => this.twoFactorAuth.enable({emitEvent: false})))
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

  changeDefaultProvider(event: MatCheckboxChange, provider: TwoFactorAuthProviderType) {
    if (this.useByDefault !== provider) {
      this.twoFactorAuth.disable({emitEvent: false});
      this.twoFaService.updateTwoFaAccountConfig(provider, true)
        .pipe(tap(() => this.twoFactorAuth.enable({emitEvent: false})))
        .subscribe(data => this.processTwoFactorAuthConfig(data));
    } else {
      event.source.checked = true;
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
        this.twoFactorAuth.disable({emitEvent: false});
        this.twoFaService.deleteTwoFaAccountConfig(TwoFactorAuthProviderType.BACKUP_CODE)
          .pipe(tap(() => this.twoFactorAuth.enable({emitEvent: false})))
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

  onChangePassword(form: FormGroupDirective): void {
    if (this.changePassword.valid) {
      this.authService.changePassword(this.changePassword.get('currentPassword').value,
        this.changePassword.get('newPassword').value, {ignoreErrors: true}).subscribe(() => {
          this.discardChanges(form);
        },
        (error) => {
          if (error.status === 400 && error.error.message === 'Current password doesn\'t match!') {
            this.changePassword.get('currentPassword').setErrors({differencePassword: true});
          } else if (error.status === 400 && error.error.message.startsWith('Password must')) {
            this.loadPasswordPolicy();
          } else if (error.status === 400 && error.error.message.startsWith('Password was already used')) {
            this.changePassword.get('newPassword').setErrors({alreadyUsed: error.error.message});
          } else {
            this.store.dispatch(new ActionNotificationShow({
              message: error.error.message,
              type: 'error',
              target: 'changePassword'
            }));
          }
        });
    } else {
      this.changePassword.markAllAsTouched();
    }
  }

  discardChanges(form: FormGroupDirective, event?: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    form.resetForm({
      currentPassword: '',
      newPassword: '',
      newPassword2: ''
    });
  }

  openApiKeysTable() {
    this.dialog.open<ApiKeysTableDialogComponent, ApiKeysTableDialogData>(
      ApiKeysTableDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          userId: this.user.id,
        }
      }).afterClosed().subscribe();
  }
}
