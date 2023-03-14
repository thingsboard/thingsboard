///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { NotificationService } from '@core/http/notification.service';
import { NotificationSettings } from '@shared/models/notification.models';
import { deepTrim, isEmptyStr } from '@core/utils';

@Component({
  selector: 'tb-notification-settings',
  templateUrl: './notification-settings.component.html',
  styleUrls: ['./settings-card.scss']
})
export class NotificationSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  notificationSettingsForm: FormGroup;
  private notificationSettings: NotificationSettings;

  constructor(protected store: Store<AppState>,
              private notificationService: NotificationService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildGeneralServerSettingsForm();
    this.notificationService.getNotificationSettings().subscribe(
      (settings) => {
        this.notificationSettings = settings;
        this.notificationSettingsForm.reset(this.notificationSettings);
      }
    );
  }

  buildGeneralServerSettingsForm() {
    this.notificationSettingsForm = this.fb.group({
      deliveryMethodsConfigs: this.fb.group({
        SLACK: this.fb.group({
          botToken: ['']
        })
      })
    });
  }

  save(): void {
    this.notificationSettings = deepTrim({
      ...this.notificationSettings,
      ...this.notificationSettingsForm.value
    });
    for (const method in this.notificationSettings.deliveryMethodsConfigs) {
      const keys = Object.keys(this.notificationSettings.deliveryMethodsConfigs[method]);
      if (keys.some(item => isEmptyStr(this.notificationSettings.deliveryMethodsConfigs[method][item]))) {
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

  confirmForm(): FormGroup {
    return this.notificationSettingsForm;
  }

}
