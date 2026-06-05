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

import { Component, Injector } from '@angular/core';
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  statusWidgetDefaultSettings,
  statusWidgetLayoutImages,
  statusWidgetLayouts,
  statusWidgetLayoutTranslations
} from '@home/components/widget/lib/indicator/status-widget.models';
import { ValueType } from '@shared/models/constants';

@Component({
    selector: 'tb-status-widget-settings',
    templateUrl: './status-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class StatusWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  statusWidgetLayouts = statusWidgetLayouts;

  statusWidgetLayoutTranslationMap = statusWidgetLayoutTranslations;
  statusWidgetLayoutImageMap = statusWidgetLayoutImages;

  valueType = ValueType;

  statusWidgetSettingsForm: UntypedFormGroup;

  cardStyleMode = 'on';

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.statusWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return statusWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.statusWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      disabledState: [settings.disabledState, []],
      layout: [settings.layout, []],
      onState: [settings.onState, []],
      offState: [settings.offState, []],
      padding: [settings.padding, []]
    });
  }
}
