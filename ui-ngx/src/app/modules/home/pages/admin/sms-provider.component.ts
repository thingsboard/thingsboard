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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminSettings, SmsProviderConfiguration } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MatDialog } from '@angular/material/dialog';
import { SendTestSmsDialogComponent, SendTestSmsDialogData } from '@home/pages/admin/send-test-sms-dialog.component';
import { NotificationSettings } from '@shared/models/notification.models';
import { deepTrim, isNotEmptyStr } from '@core/utils';
import { NotificationService } from '@core/http/notification.service';
import { Authority } from '@shared/models/authority.enum';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';

@Component({
    selector: 'tb-sms-provider',
    templateUrl: './sms-provider.component.html',
    styleUrls: ['./sms-provider.component.scss', './settings-card.scss'],
    standalone: false
})
export class SmsProviderComponent extends PageComponent implements HasConfirmForm {

  smsProvider: FormGroup;
  private adminSettings: AdminSettings<SmsProviderConfiguration>;

  notificationSettingsForm: FormGroup;
  private notificationSettings: NotificationSettings;

  private readonly authUser: AuthUser;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              private notificationService: NotificationService,
              private dialog: MatDialog,
              public fb: FormBuilder) {
    super(store);
    this.authUser = getCurrentAuthUser(this.store);
    this.buildSmsProviderForm();
    this.buildGeneralServerSettingsForm();
    this.notificationService.getNotificationSettings().subscribe(
      (settings) => {
        this.notificationSettings = settings;
        this.notificationSettingsForm.reset(this.notificationSettings);
      }
    );
    if (this.isSysAdmin()) {
      this.adminService.getAdminSettings<SmsProviderConfiguration>('sms', {ignoreErrors: true}).subscribe({
        next: adminSettings => {
          this.adminSettings = adminSettings;
          this.smsProvider.reset({configuration: this.adminSettings.jsonValue});
        },
        error: () => {
          this.adminSettings = {
            key: 'sms',
            jsonValue: null
          };
          this.smsProvider.reset({configuration: this.adminSettings.jsonValue});
        }
      });
    }
  }

  private buildSmsProviderForm() {
    this.smsProvider = this.fb.group({
      configuration: [null, [Validators.required]]
    });
    this.registerDisableOnLoadFormControl(this.smsProvider.get('configuration'));
  }

  sendTestSms(): void {
    this.dialog.open<SendTestSmsDialogComponent, SendTestSmsDialogData>(SendTestSmsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        smsProviderConfiguration: this.smsProvider.value.configuration
      }
    });
  }

  save(): void {
    this.adminSettings.jsonValue = this.smsProvider.value.configuration;
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.smsProvider.reset({configuration: this.adminSettings.jsonValue});
      }
    );
  }

  confirmForm(): FormGroup {
    return this.smsProvider.dirty ? this.smsProvider : this.notificationSettingsForm;
  }

  private buildGeneralServerSettingsForm() {
    this.notificationSettingsForm = this.fb.group({
      deliveryMethodsConfigs: this.fb.group({
        SLACK: this.fb.group({
          botToken: ['']
        }),
        MOBILE_APP: this.fb.group({
          firebaseServiceAccountCredentialsFileName: [''],
          firebaseServiceAccountCredentials: ['']
        })
      })
    });
    this.registerDisableOnLoadFormControl(this.notificationSettingsForm.get('deliveryMethodsConfigs'));
  }

  saveNotification(): void {
    this.notificationSettings = deepTrim({
      ...this.notificationSettings,
      ...this.notificationSettingsForm.value
    });
    // eslint-disable-next-line guard-for-in
    for (const method in this.notificationSettings.deliveryMethodsConfigs) {
      const keys = Object.keys(this.notificationSettings.deliveryMethodsConfigs[method]);
      if (keys.some(item => !isNotEmptyStr(this.notificationSettings.deliveryMethodsConfigs[method][item]))) {
        delete this.notificationSettings.deliveryMethodsConfigs[method];
      } else {
        this.notificationSettings.deliveryMethodsConfigs[method].method = method;
      }
    }
    this.notificationService.saveNotificationSettings(this.notificationSettings).subscribe(setting => {
      this.notificationSettings = setting;
      this.notificationSettingsForm.reset(this.notificationSettings);
    });
  }

  isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

}
