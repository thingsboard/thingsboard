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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import {
  NotificationDeliveryMethod,
  NotificationDeliveryMethodInfoMap,
  NotificationUserSettings
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-notification-settings',
  templateUrl: './notification-settings.component.html',
  styleUrls: ['./notification-settings.component.scss']
})
export class NotificationSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  notificationSettings: UntypedFormGroup;

  notificationDeliveryMethods: NotificationDeliveryMethod[];
  notificationDeliveryMethodInfoMap = NotificationDeliveryMethodInfoMap;

  private deliveryMethods = new Set([
    NotificationDeliveryMethod.SLACK,
    NotificationDeliveryMethod.MICROSOFT_TEAMS
  ]);

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private translate: TranslateService,
              private dialogService: DialogService,
              private notificationService: NotificationService,
              private fb: UntypedFormBuilder,) {
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
  }

  private patchNotificationSettings(settings: NotificationUserSettings) {
    const notificationSettingsControls: Array<AbstractControl> = [];
    if (settings.prefs) {
      this.prepareNotificationSettings(settings.prefs).forEach(setting =>
        notificationSettingsControls.push(this.fb.control(setting, [Validators.required]))
      );
    }
    this.notificationSettings.setControl('prefs', this.fb.array(notificationSettingsControls), {emitEvent: false});
  }

  private prepareNotificationSettings(prefs: any) {
    return Object.entries(prefs).map((value: any) => {
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
          const settings = this.prepareNotificationSettings(this.route.snapshot.data.userSettings.prefs);
          const notificationSettingsControls: Array<AbstractControl> = [];
          this.notificationSettings.reset({});
          if (settings) {
            settings.forEach((setting) => {
              setting.enabled = true;
              setting.enabledDeliveryMethods = this.notificationDeliveryMethods.reduce((a, v) => ({ ...a, [v]: true}), {});
              notificationSettingsControls.push(this.fb.control(setting, [Validators.required]));
            });
          }
          this.notificationSettings.setControl('prefs', this.fb.array(notificationSettingsControls), {emitEvent: false});
          this.save();
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

  get notificationSettingsFormArray(): UntypedFormArray {
    return this.notificationSettings.get('prefs') as UntypedFormArray;
  }

  save(): void {
    const settings = {prefs: {}};
    this.notificationSettings.getRawValue().prefs.forEach(value => {
      const key = value.name;
      delete value.name;
      settings.prefs[key] = value;
    });
    this.notificationService.saveNotificationUserSettings(settings).subscribe(
      (userSettings) => {
        this.notificationSettings.get('prefs').reset({});
        this.patchNotificationSettings(userSettings);
      }
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.notificationSettings;
  }
}
