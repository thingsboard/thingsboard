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
import { Datasource, WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue } from '@core/utils';
import {
  batteryLevelDefaultSettings,
  BatteryLevelLayout,
  batteryLevelLayoutImages,
  batteryLevelLayouts,
  batteryLevelLayoutTranslations
} from '@home/components/widget/lib/indicator/battery-level-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-battery-level-widget-settings',
  templateUrl: './battery-level-widget-settings.component.html',
  styleUrls: []
})
export class BatteryLevelWidgetSettingsComponent extends WidgetSettingsComponent {

  batteryLevelLayouts = batteryLevelLayouts;

  batteryLevelLayoutTranslationMap = batteryLevelLayoutTranslations;
  batteryLevelLayoutImageMap = batteryLevelLayoutImages;

  batteryLevelWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  get sectionsCountEnabled(): boolean {
    const layout: BatteryLevelLayout = this.batteryLevelWidgetSettingsForm.get('layout').value;
    return [BatteryLevelLayout.vertical_divided, BatteryLevelLayout.horizontal_divided].includes(layout);
  }

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.batteryLevelWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return batteryLevelDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.batteryLevelWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      sectionsCount: [settings.sectionsCount, [Validators.min(2), Validators.max(20)]],

      showValue: [settings.showValue, []],
      autoScaleValueSize: [settings.autoScaleValueSize, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      batteryLevelColor: [settings.batteryLevelColor, []],
      batteryShapeColor: [settings.batteryShapeColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showValue', 'layout'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showValue: boolean = this.batteryLevelWidgetSettingsForm.get('showValue').value;
    const layout: BatteryLevelLayout = this.batteryLevelWidgetSettingsForm.get('layout').value;
    const divided = [BatteryLevelLayout.vertical_divided, BatteryLevelLayout.horizontal_divided].includes(layout);

    if (showValue) {
      this.batteryLevelWidgetSettingsForm.get('autoScaleValueSize').enable();
      this.batteryLevelWidgetSettingsForm.get('valueFont').enable();
      this.batteryLevelWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.batteryLevelWidgetSettingsForm.get('autoScaleValueSize').disable();
      this.batteryLevelWidgetSettingsForm.get('valueFont').disable();
      this.batteryLevelWidgetSettingsForm.get('valueColor').disable();
    }

    if (divided) {
      this.batteryLevelWidgetSettingsForm.get('sectionsCount').enable();
    } else {
      this.batteryLevelWidgetSettingsForm.get('sectionsCount').disable();
    }

    this.batteryLevelWidgetSettingsForm.get('autoScaleValueSize').updateValueAndValidity({emitEvent});
    this.batteryLevelWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.batteryLevelWidgetSettingsForm.get('valueColor').updateValueAndValidity({emitEvent});
    this.batteryLevelWidgetSettingsForm.get('sectionsCount').updateValueAndValidity({emitEvent});
  }

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }

}
