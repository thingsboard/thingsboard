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

import { Component, ElementRef, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettings,
  BackupCodeTwoFactorAuthAccountConfig,
  TotpTwoFactorAuthAccountConfig,
  TwoFactorAuthAccountConfig,
  twoFactorAuthProvidersEnterCodeCardTranslate,
  twoFactorAuthProvidersLoginData,
  twoFactorAuthProvidersSuccessCardTranslate,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { phoneNumberPattern } from '@shared/models/settings.models';
import { deepClone, isDefinedAndNotNull, unwrapModule } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import printTemplate from '@home/pages/security/authentication-dialog/backup-code-print-template.raw';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { mergeMap, tap } from 'rxjs/operators';
import { ActionNotificationShow } from "@core/notification/notification.actions";

enum ForceTwoFAState {
  SETUP = 'setup',
  AUTHENTICATOR_APP = 'authenticatorApp',
  SMS = 'sms',
  EMAIL = 'email',
  BACKUP_CODE = 'backupCode',
}

enum ProvidersState {
  INPUT = 'INPUT',
  ENTER_CODE = 'ENTER_CODE',
  SUCCESS = 'SUCCESS',
}

enum BackupCodeState {
  CODE = 'CODE',
  SUCCESS = 'SUCCESS',
}

@Component({
  selector: 'tb-force-two-factor-auth-login',
  templateUrl: './force-two-factor-auth-login.component.html',
  styleUrls: ['./force-two-factor-auth-login.component.scss']
})
export class ForceTwoFactorAuthLoginComponent extends PageComponent implements OnInit, OnDestroy {

  TwoFactorAuthProviderType = TwoFactorAuthProviderType;
  providersData = twoFactorAuthProvidersLoginData;
  allowProviders: TwoFactorAuthProviderType[] = [];
  config: AccountTwoFaSettings;

  twoFactorAuthProvidersEnterCodeCardTranslate = twoFactorAuthProvidersEnterCodeCardTranslate;
  twoFactorAuthProvidersSuccessCardTranslate = twoFactorAuthProvidersSuccessCardTranslate;

  ForceTwoFAState = ForceTwoFAState;
  ProvidersState = ProvidersState;
  BackupCodeState = BackupCodeState

  state = signal<ForceTwoFAState>(ForceTwoFAState.SETUP);
  appState = signal<ProvidersState>(ProvidersState.INPUT);
  smsState = signal<ProvidersState>(ProvidersState.INPUT);
  emailState = signal<ProvidersState>(ProvidersState.INPUT);
  backupCodeState = signal<BackupCodeState>(BackupCodeState.CODE);

  totpAuthURL: string;
  totpAuthURLSecret: string;
  backupCode: BackupCodeTwoFactorAuthAccountConfig;

  configForm: UntypedFormGroup;
  smsConfigForm: UntypedFormGroup;
  emailConfigForm: UntypedFormGroup;

  private providersInfo: TwoFactorAuthProviderType[];
  private authAccountConfig: TwoFactorAuthAccountConfig;
  private useByDefault: boolean = true;

  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  constructor(protected store: Store<AppState>,
              private authService: AuthService,
              private twoFaService: TwoFactorAuthenticationService,
              private importExportService: ImportExportService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.providersInfo = this.authService.forceTwoFactorAuthProviders;
    this.allowedProviders();
    this.configForm = this.fb.group({
      verificationCode: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(6),
        Validators.pattern(/^\d*$/)
      ]]
    });

    this.smsConfigForm = this.fb.group({
      phone: ['', [Validators.required, Validators.pattern(phoneNumberPattern)]]
    });

    this.emailConfigForm = this.fb.group({
      email: [getCurrentAuthUser(this.store).sub, [Validators.required, Validators.email]]
    });

    this.twoFaService.getAccountTwoFaSettings().subscribe(accountConfig => {
      if (accountConfig) {
        this.config = accountConfig;
        this.useByDefault = false;
      }
    });
  }

  goBackByType(type: TwoFactorAuthProviderType) {
    switch (type) {
      case TwoFactorAuthProviderType.TOTP:
        this.appState.set(ProvidersState.INPUT);
        this.updateQRCode();
        break;
      case TwoFactorAuthProviderType.SMS:
        this.smsState.set(ProvidersState.INPUT);
        break;
      case TwoFactorAuthProviderType.EMAIL:
        this.emailState.set(ProvidersState.INPUT);
        break;
    }
  }

  get isAnyProviderAvailable() {
    return this.config?.configs ? Object.keys(this.config?.configs)?.length < this.allowProviders?.length : true;
  }

  private allowedProviders() {
    if (isDefinedAndNotNull(this.config)) {
      this.allowProviders = this.providersInfo;
    } else {
      this.allowProviders = this.providersInfo.filter(provider => provider !== TwoFactorAuthProviderType.BACKUP_CODE);
    }
  }

  updateState(type: TwoFactorAuthProviderType) {
    switch (type) {
      case TwoFactorAuthProviderType.TOTP:
        this.state.set(ForceTwoFAState.AUTHENTICATOR_APP);
        this.twoFaService.generateTwoFaAccountConfig(TwoFactorAuthProviderType.TOTP).subscribe(accountConfig => {
          this.authAccountConfig = accountConfig as TotpTwoFactorAuthAccountConfig;
          this.totpAuthURL = this.authAccountConfig.authUrl;
          this.totpAuthURLSecret = new URL(this.totpAuthURL).searchParams.get('secret');
          this.authAccountConfig.useByDefault = this.useByDefault;
          this.useByDefault = false;
          this.updateQRCode();
        });
        break;
      case TwoFactorAuthProviderType.SMS:
        this.state.set(ForceTwoFAState.SMS);
        break;
      case TwoFactorAuthProviderType.EMAIL:
        this.state.set(ForceTwoFAState.EMAIL);
        break;
      case TwoFactorAuthProviderType.BACKUP_CODE:
        this.state.set(ForceTwoFAState.BACKUP_CODE);
        this.twoFaService.generateTwoFaAccountConfig(TwoFactorAuthProviderType.BACKUP_CODE).pipe(
          tap((data: BackupCodeTwoFactorAuthAccountConfig) => this.backupCode = data),
          mergeMap(data => this.twoFaService.verifyAndSaveTwoFaAccountConfig(data, null, {ignoreLoading: true}))
        ).subscribe((config) => {
          this.config = config;
        });
        break;
    }
  }

  sendSmsCode() {
    if (this.smsConfigForm.valid) {
      this.authAccountConfig = {
        providerType: TwoFactorAuthProviderType.SMS,
        useByDefault: this.useByDefault,
        phoneNumber: this.smsConfigForm.get('phone').value as string
      };
      this.useByDefault = false;
      this.twoFaService.submitTwoFaAccountConfig(this.authAccountConfig).subscribe(() => this.smsState.set(ProvidersState.ENTER_CODE));
    }
  }

  sendEmailCode() {
    if (this.emailConfigForm.valid) {
      this.authAccountConfig = {
        providerType: TwoFactorAuthProviderType.EMAIL,
        useByDefault: this.useByDefault,
        email: this.emailConfigForm.get('email').value as string
      };
      this.useByDefault = false;
      this.twoFaService.submitTwoFaAccountConfig(this.authAccountConfig).subscribe(() => this.emailState.set(ProvidersState.ENTER_CODE));
    }
  }

  tryAnotherWay(type: TwoFactorAuthProviderType) {
    this.state.set(ForceTwoFAState.SETUP);
    this.configForm.reset();
    switch (type) {
      case TwoFactorAuthProviderType.TOTP:
        this.appState.set(ProvidersState.INPUT);
        break;
      case TwoFactorAuthProviderType.SMS:
        this.smsState.set(ProvidersState.INPUT);
        this.smsConfigForm.reset();
        break;
      case TwoFactorAuthProviderType.EMAIL:
        this.emailState.set(ProvidersState.INPUT)
        this.emailConfigForm.get('email').reset(getCurrentAuthUser(this.store).sub);
        break;
    }
  }

  saveConfig(type: TwoFactorAuthProviderType) {
    if (this.configForm.valid) {
      this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
        this.configForm.get('verificationCode').value).subscribe({
        next: (config) => {
          switch (type) {
            case TwoFactorAuthProviderType.TOTP:
              this.appState.set(ProvidersState.SUCCESS);
              break;
            case TwoFactorAuthProviderType.SMS:
              this.smsState.set(ProvidersState.SUCCESS);
              break;
            case TwoFactorAuthProviderType.EMAIL:
              this.emailState.set(ProvidersState.SUCCESS);
              break;
          }
          this.config = config;
          this.authAccountConfig = null;
          this.allowedProviders();
        },
        error: error => {
          if (error.status === 400) {
            this.configForm.get('verificationCode').setErrors({incorrectCode: true});
          } else if (error.status === 429) {
            this.configForm.get('verificationCode').setErrors({tooManyRequest: true});
          } else {
            this.store.dispatch(new ActionNotificationShow({
              message: error.error.message,
              type: 'error',
              verticalPosition: 'top',
              horizontalPosition: 'left'
            }));
          }
        }
      })
    }
  }

  private updateQRCode() {
    import('qrcode').then((QRCode) => {
      unwrapModule(QRCode).toCanvas(this.canvasRef.nativeElement, this.totpAuthURL);
      this.canvasRef.nativeElement.style.width = 'auto';
      this.canvasRef.nativeElement.style.height = 'auto';
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  cancelLogin() {
    this.authService.logout();
  }

  downloadFile() {
    this.importExportService.exportText(this.backupCode.codes, 'backup-codes');
  }

  printCode() {
    const codeTemplate = deepClone(this.backupCode.codes)
      .map(code => `<div class="code-row"><input type="checkbox"><span class="code">${code}</span></div>`).join('');
    const printPage = printTemplate.replace('${codesBlock}', codeTemplate);
    const newWindow = window.open('', 'Print backup code');

    newWindow.document.open();
    newWindow.document.write(printPage);

    setTimeout(() => {
      newWindow.print();

      newWindow.document.close();

      setTimeout(() => {
        newWindow.close();
      }, 10);
    }, 0);
  }
}
