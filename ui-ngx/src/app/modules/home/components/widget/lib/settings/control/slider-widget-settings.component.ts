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
import { ValueType } from '@shared/models/constants';
import {
  SliderLayout,
  sliderLayoutImages,
  sliderLayouts,
  sliderLayoutTranslations,
  sliderWidgetDefaultSettings
} from '@home/components/widget/lib/rpc/slider-widget.models';
import { formatValue } from '@core/utils';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-slider-widget-settings',
  templateUrl: './slider-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class SliderWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    return this.widgetConfig?.config?.targetDevice;
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }

  sliderLayout = SliderLayout;

  sliderLayouts = sliderLayouts;

  sliderLayoutTranslationMap = sliderLayoutTranslations;
  sliderLayoutImageMap = sliderLayoutImages;

  valueType = ValueType;

  sliderWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.sliderWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return sliderWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.sliderWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      valueChange: [settings.valueChange, []],
      disabledState: [settings.disabledState, []],

      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showValue: [settings.showValue, []],
      valueUnits: [settings.valueUnits, []],
      valueDecimals: [settings.valueDecimals, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      tickMin: [settings.tickMin, []],
      tickMax: [settings.tickMax, []],

      showTicks: [settings.showTicks, []],
      ticksFont: [settings.ticksFont, []],
      ticksColor: [settings.ticksColor, []],

      showTickMarks: [settings.showTickMarks, []],
      tickMarksCount: [settings.tickMarksCount, [Validators.min(2)]],
      tickMarksColor: [settings.tickMarksColor, []],

      mainColor: [settings.mainColor, []],
      backgroundColor: [settings.backgroundColor, []],

      mainColorDisabled: [settings.mainColorDisabled, []],
      backgroundColorDisabled: [settings.backgroundColorDisabled, []],

      leftIconSize: [settings.leftIconSize, [Validators.min(0)]],
      leftIconSizeUnit: [settings.leftIconSizeUnit, []],
      leftIcon: [settings.leftIcon, []],
      leftIconColor: [settings.leftIconColor, []],

      rightIconSize: [settings.rightIconSize, [Validators.min(0)]],
      rightIconSizeUnit: [settings.rightIconSizeUnit, []],
      rightIcon: [settings.rightIcon, []],
      rightIconColor: [settings.rightIconColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showValue', 'showTicks', 'showTickMarks', 'layout'];
  }

  protected updateValidators(_emitEvent: boolean): void {
    const showValue: boolean = this.sliderWidgetSettingsForm.get('showValue').value;
    const showTicks: boolean = this.sliderWidgetSettingsForm.get('showTicks').value;
    const showTickMarks: boolean = this.sliderWidgetSettingsForm.get('showTickMarks').value;
    const layout: SliderLayout = this.sliderWidgetSettingsForm.get('layout').value;

    const valueEnabled = layout !== SliderLayout.simplified;
    const leftRightIconsEnabled = layout === SliderLayout.extended;

    if (valueEnabled && showValue) {
      this.sliderWidgetSettingsForm.get('valueUnits').enable();
      this.sliderWidgetSettingsForm.get('valueDecimals').enable();
      this.sliderWidgetSettingsForm.get('valueFont').enable();
      this.sliderWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.sliderWidgetSettingsForm.get('valueUnits').disable();
      this.sliderWidgetSettingsForm.get('valueDecimals').disable();
      this.sliderWidgetSettingsForm.get('valueFont').disable();
      this.sliderWidgetSettingsForm.get('valueColor').disable();
    }

    if (showTicks) {
      this.sliderWidgetSettingsForm.get('ticksFont').enable();
      this.sliderWidgetSettingsForm.get('ticksColor').enable();
    } else {
      this.sliderWidgetSettingsForm.get('ticksFont').disable();
      this.sliderWidgetSettingsForm.get('ticksColor').disable();
    }

    if (showTickMarks) {
      this.sliderWidgetSettingsForm.get('tickMarksCount').enable();
      this.sliderWidgetSettingsForm.get('tickMarksColor').enable();
    } else {
      this.sliderWidgetSettingsForm.get('tickMarksCount').disable();
      this.sliderWidgetSettingsForm.get('tickMarksColor').disable();
    }

    if (leftRightIconsEnabled) {
      this.sliderWidgetSettingsForm.get('leftIconSize').enable();
      this.sliderWidgetSettingsForm.get('leftIconSizeUnit').enable();
      this.sliderWidgetSettingsForm.get('leftIcon').enable();
      this.sliderWidgetSettingsForm.get('leftIconColor').enable();
      this.sliderWidgetSettingsForm.get('rightIconSize').enable();
      this.sliderWidgetSettingsForm.get('rightIconSizeUnit').enable();
      this.sliderWidgetSettingsForm.get('rightIcon').enable();
      this.sliderWidgetSettingsForm.get('rightIconColor').enable();
    } else {
      this.sliderWidgetSettingsForm.get('leftIconSize').disable();
      this.sliderWidgetSettingsForm.get('leftIconSizeUnit').disable();
      this.sliderWidgetSettingsForm.get('leftIcon').disable();
      this.sliderWidgetSettingsForm.get('leftIconColor').disable();
      this.sliderWidgetSettingsForm.get('rightIconSize').disable();
      this.sliderWidgetSettingsForm.get('rightIconSizeUnit').disable();
      this.sliderWidgetSettingsForm.get('rightIcon').disable();
      this.sliderWidgetSettingsForm.get('rightIconColor').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.sliderWidgetSettingsForm.get('valueUnits').value);
    const decimals: number = this.sliderWidgetSettingsForm.get('valueDecimals').value;
    return formatValue(48, decimals, units, false);
  }
}
