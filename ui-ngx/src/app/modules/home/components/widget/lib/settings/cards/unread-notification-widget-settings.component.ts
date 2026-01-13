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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { unreadNotificationDefaultSettings } from '@home/components/widget/lib/cards/unread-notification-widget.models';

@Component({
  selector: 'tb-unread-notification-widget-settings',
  templateUrl: './unread-notification-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UnreadNotificationWidgetSettingsComponent extends WidgetSettingsComponent {

  unreadNotificationWidgetSettingsForm: UntypedFormGroup;

  countPreviewFn = this._countPreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.unreadNotificationWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return unreadNotificationDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.unreadNotificationWidgetSettingsForm = this.fb.group({
      maxNotificationDisplay: [settings?.maxNotificationDisplay, [Validators.required, Validators.min(1)]],
      showCounter: [settings?.showCounter, []],
      counterValueFont: [settings?.counterValueFont, []],
      counterValueColor: [settings?.counterValueColor, []],
      counterColor: [settings?.counterColor, []],

      enableViewAll: [settings?.enableViewAll, []],
      enableFilter: [settings?.enableFilter, []],
      enableMarkAsRead: [settings?.enableMarkAsRead, []],

      background: [settings?.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showCounter'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showCounter: boolean = this.unreadNotificationWidgetSettingsForm.get('showCounter').value;

    if (showCounter) {
      this.unreadNotificationWidgetSettingsForm.get('counterValueFont').enable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterValueColor').enable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterColor').enable({emitEvent});
    } else {
      this.unreadNotificationWidgetSettingsForm.get('counterValueFont').disable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterValueColor').disable({emitEvent});
      this.unreadNotificationWidgetSettingsForm.get('counterColor').disable({emitEvent});
    }
  }

  private _countPreviewFn(): string {
    return this.unreadNotificationWidgetSettingsForm.get('maxNotificationDisplay').value?.toString() || '6';
  }

}
