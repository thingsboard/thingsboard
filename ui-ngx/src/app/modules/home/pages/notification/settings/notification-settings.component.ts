// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormArray, FormControl, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import {
  isNotificationTypeReceivable,
  NotificationDeliveryMethod,
  NotificationDeliveryMethodInfoMap,
  NotificationType,
  NotificationUserSetting,
  NotificationUserSettings
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { DialogService } from '@core/services/dialog.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

type NotificationSettingRow = NotificationUserSetting & { name: string };

@Component({
    selector: 'tb-notification-settings',
    templateUrl: './notification-settings.component.html',
    styleUrls: ['./../../admin/settings-card.scss', './notification-settings.component.scss'],
    standalone: false
})
export class NotificationSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  notificationSettings: UntypedFormGroup;

  notificationDeliveryMethods: NotificationDeliveryMethod[];
  notificationDeliveryMethodInfoMap = NotificationDeliveryMethodInfoMap;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.PROFILE, Operation.WRITE);

  private deliveryMethods = new Set([
    NotificationDeliveryMethod.SLACK,
    NotificationDeliveryMethod.MICROSOFT_TEAMS
  ]);

  private userPrefs: {[key: string]: NotificationUserSetting} = {};

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private translate: TranslateService,
              private dialogService: DialogService,
              private notificationService: NotificationService,
              private fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService,
              ) {
    super(store);
    this.notificationService.getAvailableDeliveryMethods({ignoreLoading: true}).subscribe(
      allowMethods => {
        this.notificationDeliveryMethods = allowMethods.filter(value => !this.deliveryMethods.has(value));
        this.patchNotificationSettings(this.route.snapshot.data.userSettings);
      });
  }

  ngOnInit() {
    this.buildNotificationSettingsForm();
  }

  private buildNotificationSettingsForm() {
    this.notificationSettings = this.fb.group({
      prefs: this.fb.array([])
    });
    if (this.readonly) {
      this.notificationSettings.disable();
    }
  }

  private patchNotificationSettings(settings: NotificationUserSettings) {
    this.userPrefs = settings.prefs ?? {};
    this.setPrefsValue(this.prepareNotificationSettings(this.userPrefs));
  }

  private setPrefsValue(prefs: NotificationSettingRow[]) {
    const prefsArray = this.notificationSettingsFormArray;
    const sameRows = prefsArray.length === prefs.length &&
      prefs.every((pref, index) => prefsArray.at(index).value?.name === pref.name);
    if (sameRows) {
      prefsArray.patchValue(prefs, {emitEvent: false});
    } else {
      this.notificationSettings.setControl('prefs', this.fb.array(
        prefs.map(pref => this.fb.control({value: pref, disabled: this.readonly}, [Validators.required]))
      ), {emitEvent: false});
    }
  }

  private prepareNotificationSettings(prefs: {[key: string]: NotificationUserSetting}): NotificationSettingRow[] {
    const authState = getCurrentAuthState(this.store);
    return Object.entries(deepClone(prefs))
      .filter(([type]) =>
        isNotificationTypeReceivable(type as NotificationType, authState.authUser.authority, authState.edgesSupportEnabled))
      .map((value: any) => {
        value[1].name = value[0];
        if (!value[1].enabled && Object.values(value[1].enabledDeliveryMethods).some(deliveryMethod => deliveryMethod === true)) {
          const enabledDeliveryMethod = deepClone(value[1].enabledDeliveryMethods);
          Object.keys(enabledDeliveryMethod).forEach(key => {
            enabledDeliveryMethod[key] = false;
          });
          value[1].enabledDeliveryMethods = enabledDeliveryMethod;
        }
        value[1].enabledDeliveryMethods = Object.assign(
          this.notificationDeliveryMethods.reduce((a, v) => ({ ...a, [v]: true}), {}),
          value[1].enabledDeliveryMethods
        );
        return value[1];
      });
  }

  resetSettings() {
    this.dialogService.confirm(
      this.translate.instant('notification.settings.reset-all-title'),
      this.translate.instant('notification.settings.reset-all-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(
      result => {
        if (result) {
          const allMethodsEnabled = this.notificationDeliveryMethods.reduce((a, v) => ({ ...a, [v]: true}), {});
          this.setPrefsValue(this.prepareNotificationSettings(this.userPrefs).map(setting =>
            ({...setting, enabled: true, enabledDeliveryMethods: allMethodsEnabled})
          ));
          this.save({});
        }
      }
    );
  }

  getChecked = (method: NotificationDeliveryMethod = null): boolean => {
    const type = this.notificationSettings.get('prefs').value;
    if (isDefinedAndNotNull(method)) {
      return isDefinedAndNotNull(type) && type.every(resource => resource.enabledDeliveryMethods[method]);
    }
    return isDefinedAndNotNull(type) && type.every(resource => resource.enabled);
  };

  getSomeChecked = () => {
    const type = this.notificationSettings.get('prefs').value;
    return isDefinedAndNotNull(type) && type.some(resource => resource.enabled);
  };

  getIndeterminate = (deliveryMethod: NotificationDeliveryMethod = null): boolean => {
    const type = this.notificationSettings.get('prefs').value;
    if (isDefinedAndNotNull(type)) {
      const checkedResource = isDefinedAndNotNull(deliveryMethod) ?
        type.filter(resource => resource.enabledDeliveryMethods[deliveryMethod]) :
        type.filter(resource => resource.enabled);
      return checkedResource.length !== 0 && checkedResource.length !== type.length;
    }
    return false;
  };

  changeInstanceTypeCheckBox = (value: boolean, deliveryMethod: NotificationDeliveryMethod = null): void => {
    const type = deepClone(this.notificationSettings.get('prefs').value);
    if (isDefinedAndNotNull(deliveryMethod)) {
      type.forEach(notificationType => notificationType.enabledDeliveryMethods[deliveryMethod] = value);
    } else {
      type.forEach(notificationType => {
        notificationType.enabled = value;
        notificationType.enabledDeliveryMethods =
          Object.keys(notificationType.enabledDeliveryMethods).reduce((a, v) => ({ ...a, [v]: value}), {});
      });
    }
    this.notificationSettings.get('prefs').patchValue(type, {emitEvent: false});
    this.notificationSettings.markAsDirty();
  };

  get notificationSettingsFormArray(): FormArray<FormControl> {
    return this.notificationSettings.get('prefs') as FormArray<FormControl>;
  }

  save(basePrefs = this.userPrefs): void {
    const settings = {prefs: deepClone(basePrefs)};
    (this.notificationSettings.getRawValue().prefs as NotificationSettingRow[]).forEach(({name, ...pref}) => {
      settings.prefs[name] = pref;
    });
    this.notificationService.saveNotificationUserSettings(settings).subscribe(
      (userSettings) => {
        this.patchNotificationSettings(userSettings);
        this.notificationSettings.markAsPristine();
      }
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.notificationSettings;
  }
}
