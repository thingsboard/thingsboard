// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, DestroyRef } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminSettings, SmsProviderConfiguration } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MatDialog } from '@angular/material/dialog';
import { SendTestSmsDialogComponent, SendTestSmsDialogData } from '@home/pages/admin/send-test-sms-dialog.component';
import { NotificationDeliveryMethod, NotificationSettings } from '@shared/models/notification.models';
import { deepTrim, isDefined, isNotEmptyStr } from '@core/utils';
import { NotificationService } from '@core/http/notification.service';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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

  private readonly authUser = getCurrentAuthUser(this.store);
  readonly = this.isTenantAdmin() && !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private notificationService: NotificationService,
              private dialog: MatDialog,
              private userPermissionsService: UserPermissionsService,
              public fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
    this.buildSmsProviderForm();
    this.buildGeneralServerSettingsForm();
    this.notificationService.getNotificationSettings().subscribe(
      (settings) => {
        this.notificationSettings = settings;
        this.setNotificationSettings(this.notificationSettings);
      }
    );
    this.adminService.getAdminSettings<SmsProviderConfiguration>('sms', false, {ignoreErrors: true}).subscribe({
      next: adminSettings => {
        this.adminSettings = adminSettings;
        this.setSmsProviderSettings(this.adminSettings.jsonValue);
      },
      error: () => {
        this.adminSettings = {
          key: 'sms',
          jsonValue: null
        };
        this.setSmsProviderSettings(this.adminSettings.jsonValue);
      }
    });
  }

  private setSmsProviderSettings(smsProviderConfiguration: SmsProviderConfiguration) {
    const useSystemSmsSettings = this.isTenantAdmin()
      ? (smsProviderConfiguration && isDefined(smsProviderConfiguration.useSystemSmsSettings) ?
         smsProviderConfiguration.useSystemSmsSettings : true)
      : false;
    if (smsProviderConfiguration) {
      delete smsProviderConfiguration.useSystemSmsSettings;
    }
    this.smsProvider.reset({configuration: useSystemSmsSettings ? null : smsProviderConfiguration});
    if (this.isTenantAdmin()) {
      this.smsProvider.get('useSystemSmsSettings').setValue(
        useSystemSmsSettings, {emitEvent: false}
      );
    }
    this.updateValidators();
  }

  private setNotificationSettings(notificationSettings: NotificationSettings) {
    const useSystemSettings = this.isTenantAdmin()
      ? (notificationSettings.deliveryMethodsConfigs?.MOBILE_APP && isDefined(notificationSettings.deliveryMethodsConfigs.MOBILE_APP.useSystemSettings) ?
        notificationSettings.deliveryMethodsConfigs.MOBILE_APP.useSystemSettings : true)
      : false;
    if (notificationSettings.deliveryMethodsConfigs?.MOBILE_APP) {
      delete notificationSettings.deliveryMethodsConfigs.MOBILE_APP.useSystemSettings;
    }
    this.notificationSettingsForm.reset(this.notificationSettings, {emitEvent: false});
    if (this.isTenantAdmin()) {
      this.notificationSettingsForm.get('deliveryMethodsConfigs.MOBILE_APP.useSystemSettings').setValue(
        useSystemSettings, {emitEvent: false}
      );
    }
    this.updatedNotificationSettings(useSystemSettings);
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  private buildSmsProviderForm() {
    this.smsProvider = this.fb.group({
      useSystemSmsSettings: [false],
      configuration: [null, [Validators.required]]
    });
    if (this.readonly) {
      this.smsProvider.get('configuration').disable({emitEvent: false});
      if (this.isTenantAdmin()) {
        this.smsProvider.get('useSystemSmsSettings').disable({emitEvent: false});
      }
    } else {
      this.registerDisableOnLoadFormControl(this.smsProvider.get('configuration'));
      if (this.isTenantAdmin()) {
        this.registerDisableOnLoadFormControl(this.smsProvider.get('useSystemSmsSettings'));
      }
    }
    if (this.isTenantAdmin()) {
      this.smsProvider.get('useSystemSmsSettings').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(
        () => {
          this.updateValidators();
        }
      );
    }
  }

  private updateValidators() {
    const useSystemSmsSettings: boolean = this.smsProvider.get('useSystemSmsSettings').value;
    if (this.isTenantAdmin() && useSystemSmsSettings) {
      this.smsProvider.get('configuration').setValidators([]);
    } else {
      this.smsProvider.get('configuration').setValidators([Validators.required]);
    }
    this.smsProvider.get('configuration').updateValueAndValidity({emitEvent: false});
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
    if (this.isTenantAdmin()) {
      if (this.smsProvider.value.useSystemSmsSettings) {
        this.adminSettings.jsonValue = {
          useSystemSmsSettings: true
        } as SmsProviderConfiguration;
      } else {
        this.adminSettings.jsonValue = {...this.smsProvider.value.configuration, useSystemSmsSettings: false};
      }
    } else {
      this.adminSettings.jsonValue = this.smsProvider.value.configuration;
    }
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.setSmsProviderSettings(this.adminSettings.jsonValue);
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
          useSystemSettings: [false],
          firebaseServiceAccountCredentialsFileName: [''],
          firebaseServiceAccountCredentials: ['', this.isTenantAdmin() ? [Validators.required] : []]
        })
      })
    });
    if(this.readonly) {
      this.notificationSettingsForm.disable({emitEvent: false});
    } else {
      this.isLoading$.subscribe((isLoading) => {
        if (isLoading) {
          this.notificationSettingsForm.disable({emitEvent: false});
        } else {
          this.notificationSettingsForm.enable({emitEvent: false});
          this.updatedNotificationSettings(this.notificationSettingsForm.value.deliveryMethodsConfigs.MOBILE_APP.useSystemSettings)
        }
      })
    }
    if (this.isTenantAdmin()) {
      this.notificationSettingsForm.get('deliveryMethodsConfigs.MOBILE_APP.useSystemSettings').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(
        (value: boolean) => {
          this.updatedNotificationSettings(value);
        }
      );
    }
  }

  private updatedNotificationSettings(useMobileSystemSettings: boolean) {
    if (this.isTenantAdmin() && useMobileSystemSettings) {
      this.notificationSettingsForm.get('deliveryMethodsConfigs.MOBILE_APP.firebaseServiceAccountCredentials').disable({emitEvent: false});
      this.notificationSettingsForm.get('deliveryMethodsConfigs.MOBILE_APP.firebaseServiceAccountCredentialsFileName').disable({emitEvent: false});
    } else {
      this.notificationSettingsForm.get('deliveryMethodsConfigs.MOBILE_APP.firebaseServiceAccountCredentials').enable({emitEvent: false});
      this.notificationSettingsForm.get('deliveryMethodsConfigs.MOBILE_APP.firebaseServiceAccountCredentialsFileName').enable({emitEvent: false});
    }
  }

  saveNotification(): void {
    this.notificationSettings = deepTrim({
      ...this.notificationSettings,
      ...this.notificationSettingsForm.value
    });
    if (this.isTenantAdmin() && this.notificationSettings.deliveryMethodsConfigs.MOBILE_APP.useSystemSettings) {
      this.notificationSettings.deliveryMethodsConfigs.MOBILE_APP = {
        useSystemSettings: true
      } as any;
    } else {
      this.notificationSettings.deliveryMethodsConfigs.MOBILE_APP.useSystemSettings = false;
    }
    // eslint-disable-next-line guard-for-in
    for (const method in this.notificationSettings.deliveryMethodsConfigs) {
      if (this.isTenantAdmin() && method === NotificationDeliveryMethod.MOBILE_APP) {
        this.notificationSettings.deliveryMethodsConfigs[method].method = method;
        continue;
      }
      const keys = Object.keys(this.notificationSettings.deliveryMethodsConfigs[method]);
      if (keys.every(item => !isNotEmptyStr(this.notificationSettings.deliveryMethodsConfigs[method][item]))) {
        delete this.notificationSettings.deliveryMethodsConfigs[method];
      } else {
        this.notificationSettings.deliveryMethodsConfigs[method].method = method;
      }
    }
    this.notificationService.saveNotificationSettings(this.notificationSettings).subscribe(setting => {
      this.notificationSettings = setting;
      this.setNotificationSettings(this.notificationSettings);
    });
  }
}
