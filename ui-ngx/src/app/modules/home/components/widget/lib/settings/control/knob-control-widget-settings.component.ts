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
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { knobWidgetDefaultSettings, prepareKnobSettings } from '@shared/models/widget/rpc/knob.component.models';
import { ValueType } from '@shared/models/constants';
import { deepClone } from '@core/utils';

@Component({
    selector: 'tb-knob-control-widget-settings',
    templateUrl: './knob-control-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class KnobControlWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  valueType = ValueType;

  knobControlWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.knobControlWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return knobWidgetDefaultSettings;
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const knobSettings = prepareKnobSettings(deepClone(settings) as any) as WidgetSettings;
    knobSettings.valueDecimals = this.widgetConfig?.config?.decimals;
    knobSettings.valueUnits = deepClone(this.widgetConfig?.config?.units);
    return super.prepareInputSettings(knobSettings);
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    const newSettings = deepClone(settings);
    if (this.widgetConfig?.config) {
      this.widgetConfig.config.units = settings.valueUnits;
      this.widgetConfig.config.decimals = settings.valueDecimals;
    }
    delete newSettings.valueUnits;
    delete newSettings.valueDecimals;
    return super.prepareOutputSettings(newSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.knobControlWidgetSettingsForm = this.fb.group({

      // Common settings

      title: [settings.title, []],

      // Value settings

      initialState: [settings.initialState, []],
      valueChange: [settings.valueChange, []],

      minValue: [settings.minValue, [Validators.required]],
      maxValue: [settings.maxValue, [Validators.required]],

      valueUnits: [settings.valueUnits, []],
      valueDecimals: [settings.valueDecimals, []],
      initialValue: [settings.initialValue, []],

    });
  }
}
