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

import { Component, OnInit, ViewChild } from '@angular/core';
import { UserService } from '@core/http/user.service';
import { AuthUser, User } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { ActionAuthUpdateUserDetails } from '@core/auth/auth.actions';
import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import { ActionSettingsChangeLanguage } from '@core/settings/settings.actions';
import { ChangePasswordDialogComponent } from '@modules/home/pages/profile/change-password-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { AuthService } from '@core/auth/auth.service';
import { ActivatedRoute } from '@angular/router';
import { isDefinedAndNotNull } from '@core/utils';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DatePipe } from '@angular/common';
import { ClipboardService } from 'ngx-clipboard';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { AccountTwoFaSettings, TwoFactorAuthProviderType } from '@shared/models/two-factor-auth.models';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { TotpAuthDialogComponent } from '@home/pages/profile/authentication-dialog/totp-auth-dialog.component';
import { SMSAuthDialogComponent } from '@home/pages/profile/authentication-dialog/sms-auth-dialog.component';
import { EmailAuthDialogComponent, } from '@home/pages/profile/authentication-dialog/email-auth-dialog.component';

@Component({
  selector: 'tb-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent extends PageComponent implements OnInit, HasConfirmForm {

  authorities = Authority;
  profile: FormGroup;
  twoFactorAuth: FormGroup;
  user: User;
  languageList = env.supportedLangs;
  allowTwoFactorAuth = false;
  allowSMS2faProvider = false;
  allowTOTP2faProvider = false;
  allowEmail2faProvider = false;
  twoFactorAuthProviderType = TwoFactorAuthProviderType;

  @ViewChild('totp') totp: MatSlideToggle;

  private authDialogMap = new Map<TwoFactorAuthProviderType, any>(
[
          [TwoFactorAuthProviderType.TOTP, TotpAuthDialogComponent],
          [TwoFactorAuthProviderType.SMS, SMSAuthDialogComponent],
          [TwoFactorAuthProviderType.EMAIL, EmailAuthDialogComponent]
      ]
  );

  private readonly authUser: AuthUser;

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
              private userService: UserService,
              private authService: AuthService,
              private translate: TranslateService,
              private twoFaService: TwoFactorAuthenticationService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              public fb: FormBuilder,
              private datePipe: DatePipe,
              private clipboardService: ClipboardService) {
    super(store);
    this.authUser = getCurrentAuthUser(this.store);
  }

  ngOnInit() {
    this.buildProfileForm();
    this.buildTwoFactorForm();
    this.userLoaded(this.route.snapshot.data.user);
    this.twoFactorLoad(this.route.snapshot.data.providers);
  }

  private buildProfileForm() {
    this.profile = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: [''],
      lastName: [''],
      language: [''],
      homeDashboardId: [null],
      homeDashboardHideToolbar: [true]
    });
  }

  private buildTwoFactorForm() {
    this.twoFactorAuth = this.fb.group({
      TOTP: [false],
      SMS: [false],
      EMAIL: [false],
      useByDefault: [null]
    });
    this.twoFactorAuth.get('useByDefault').valueChanges.subscribe(value => {
      this.twoFaService.updateTwoFaAccountConfig(value, true, {ignoreLoading: true})
        .subscribe(data => this.processTwoFactorAuthConfig(data));
    });
  }

  save(): void {
    this.user = {...this.user, ...this.profile.value};
    if (!this.user.additionalInfo) {
      this.user.additionalInfo = {};
    }
    this.user.additionalInfo.lang = this.profile.get('language').value;
    this.user.additionalInfo.homeDashboardId = this.profile.get('homeDashboardId').value;
    this.user.additionalInfo.homeDashboardHideToolbar = this.profile.get('homeDashboardHideToolbar').value;
    this.userService.saveUser(this.user).subscribe(
      (user) => {
        this.userLoaded(user);
        this.store.dispatch(new ActionAuthUpdateUserDetails({ userDetails: {
            additionalInfo: {...user.additionalInfo},
            authority: user.authority,
            createdTime: user.createdTime,
            tenantId: user.tenantId,
            customerId: user.customerId,
            email: user.email,
            firstName: user.firstName,
            id: user.id,
            lastName: user.lastName,
          } }));
        this.store.dispatch(new ActionSettingsChangeLanguage({ userLang: user.additionalInfo.lang }));
      }
    );
  }

  changePassword(): void {
    this.dialog.open(ChangePasswordDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    });
  }

  private userLoaded(user: User) {
    this.user = user;
    this.profile.reset(user);
    let lang;
    let homeDashboardId;
    let homeDashboardHideToolbar = true;
    if (user.additionalInfo) {
      if (user.additionalInfo.lang) {
        lang = user.additionalInfo.lang;
      }
      homeDashboardId = user.additionalInfo.homeDashboardId;
      if (isDefinedAndNotNull(user.additionalInfo.homeDashboardHideToolbar)) {
        homeDashboardHideToolbar = user.additionalInfo.homeDashboardHideToolbar;
      }
    }
    if (!lang) {
      lang = this.translate.currentLang;
    }
    this.profile.get('language').setValue(lang);
    this.profile.get('homeDashboardId').setValue(homeDashboardId);
    this.profile.get('homeDashboardHideToolbar').setValue(homeDashboardHideToolbar);
  }

  private twoFactorLoad(providers: TwoFactorAuthProviderType[]) {
    if (providers.length) {
      this.allowTwoFactorAuth = true;
      this.twoFaService.getAccountTwoFaSettings().subscribe(data => this.processTwoFactorAuthConfig(data));
      providers.forEach(provider => {
        switch (provider) {
          case TwoFactorAuthProviderType.SMS:
            this.allowSMS2faProvider = true;
            break;
          case TwoFactorAuthProviderType.TOTP:
            this.allowTOTP2faProvider = true;
            break;
          case TwoFactorAuthProviderType.EMAIL:
            this.allowEmail2faProvider = true;
            break;
        }
      });
    }
  }

  private processTwoFactorAuthConfig(setting?: AccountTwoFaSettings) {
    if (setting) {
      Object.values(setting.configs).forEach(config => {
        this.twoFactorAuth.get(config.providerType).setValue(true);
        if (config.useByDefault) {
          this.twoFactorAuth.get('useByDefault').setValue(config.providerType, {emitEvent: false});
        }
      });
    }
  }

  confirmForm(): FormGroup {
    return this.profile;
  }

  isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
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
    const providerName = provider === TwoFactorAuthProviderType.TOTP ? 'authenticator app' : `${provider.toLowerCase()} authentication`;
    if (this.twoFactorAuth.get(provider).value) {
      this.dialogService.confirm(`Are you sure you want to disable ${providerName}?`,
        `Disabling ${providerName} will make your account less secure`).subscribe(res => {
        if (res) {
          this.twoFaService.deleteTwoFaAccountConfig(provider).subscribe(data => this.processTwoFactorAuthConfig(data));
        }
      });
    } else {
      this.dialog.open(this.authDialogMap.get(provider), {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          email: this.user.email
        }
      }).afterClosed().subscribe(res => {
        if (res) {
          this.twoFactorAuth.get(provider).setValue(res);
          this.twoFactorAuth.get('useByDefault').setValue(provider, {emitEvent: false});
        }
      });
    }
  }
}
