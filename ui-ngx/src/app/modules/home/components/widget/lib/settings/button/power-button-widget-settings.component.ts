// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ValueType } from '@shared/models/constants';
import {
  powerButtonDefaultSettings,
  powerButtonLayoutImages,
  powerButtonLayouts,
  powerButtonLayoutTranslations
} from '@home/components/widget/lib/rpc/power-button-widget.models';

@Component({
    selector: 'tb-power-button-widget-settings',
    templateUrl: './power-button-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class PowerButtonWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  powerButtonLayouts = powerButtonLayouts;

  powerButtonLayoutTranslationMap = powerButtonLayoutTranslations;
  powerButtonLayoutImageMap = powerButtonLayoutImages;

  valueType = ValueType;

  powerButtonWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.powerButtonWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return powerButtonDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.powerButtonWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      onUpdateState: [settings.onUpdateState, []],
      offUpdateState: [settings.offUpdateState, []],
      disabledState: [settings.disabledState, []],

      layout: [settings.layout, []],

      mainColorOn: [settings.mainColorOn, []],
      backgroundColorOn: [settings.backgroundColorOn, []],

      mainColorOff: [settings.mainColorOff, []],
      backgroundColorOff: [settings.backgroundColorOff, []],

      mainColorDisabled: [settings.mainColorDisabled, []],
      backgroundColorDisabled: [settings.backgroundColorDisabled, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }
}
