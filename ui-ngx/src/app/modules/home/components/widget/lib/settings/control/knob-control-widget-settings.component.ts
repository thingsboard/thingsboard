// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
