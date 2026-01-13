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
import { getTargetDeviceFromDatasources } from '@shared/models/widget-settings.models';
import {
  valueStepperDefaultSettings,
  valueStepperTypeImages,
  valueStepperTypes,
  valueStepperTypeTranslations
} from '@home/components/widget/lib/rpc/value-stepper-widget.models';
import { formatValue } from '@core/utils';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

type ButtonAppearanceType = 'left' | 'right';

@Component({
  selector: 'tb-value-stepper-widget-settings',
  templateUrl: './value-stepper-widget-settings.component.html',
  styleUrls: ['../widget-settings.scss']
})
export class ValueStepperWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    const datasources = this.widgetConfig?.config?.datasources;
    return getTargetDeviceFromDatasources(datasources);
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }
  get borderRadius(): string {
    return this.widgetConfig?.config?.borderRadius;
  }

  valueType = ValueType;

  valueStepperWidgetSettingsForm: UntypedFormGroup;

  valueStepperTypeTranslationMap = valueStepperTypeTranslations;
  valueStepperTypes = valueStepperTypes;
  valueStepperTypeImageMap = valueStepperTypeImages;

  buttonAppearanceType: ButtonAppearanceType = 'left';

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.valueStepperWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return valueStepperDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.valueStepperWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      leftButtonClick: [settings.leftButtonClick, []],
      rightButtonClick: [settings.rightButtonClick, []],
      disabledState: [settings.disabledState, []],

      appearance: this.fb.group({
        type: [settings.appearance.type, []],
        autoScale: [settings.appearance.autoScale, []],
        minValueRange: [settings.appearance.minValueRange, []],
        maxValueRange: [settings.appearance.maxValueRange, []],
        valueStep: [settings.appearance.valueStep, [Validators.min(0)]],
        showValueBox: [settings.appearance.showValueBox, []],
        valueUnits: [settings.appearance.valueUnits, []],
        valueDecimals: [settings.appearance.valueDecimals, []],
        valueFont: [settings.appearance.valueFont, []],
        valueColor: [settings.appearance.valueColor],
        valueBoxBackground: [settings.appearance.valueBoxBackground, []],
        showBorder: [settings.appearance.showBorder, []],
        borderWidth: [settings.appearance.borderWidth, []],
        borderColor: [settings.appearance.borderColor, []]
      }),

      buttonAppearance: this.fb.group({
        leftButton: this.fb.group({
          showButton: [settings.buttonAppearance.leftButton.showButton],
          icon: [settings.buttonAppearance.leftButton.icon],
          iconSize: [settings.buttonAppearance.leftButton.iconSize],
          iconSizeUnit: [settings.buttonAppearance.leftButton.iconSizeUnit],
          mainColorOn: [settings.buttonAppearance.leftButton.mainColorOn, []],
          backgroundColorOn: [settings.buttonAppearance.leftButton.backgroundColorOn, []],
          mainColorDisabled: [settings.buttonAppearance.leftButton.mainColorDisabled, []],
          backgroundColorDisabled: [settings.buttonAppearance.leftButton.backgroundColorDisabled, []]
        }),
        rightButton: this.fb.group({
          showButton: [settings.buttonAppearance.rightButton.showButton],
          icon: [settings.buttonAppearance.rightButton.icon],
          iconSize: [settings.buttonAppearance.rightButton.iconSize],
          iconSizeUnit: [settings.buttonAppearance.rightButton.iconSizeUnit],
          mainColorOn: [settings.buttonAppearance.rightButton.mainColorOn, []],
          backgroundColorOn: [settings.buttonAppearance.rightButton.backgroundColorOn, []],
          mainColorDisabled: [settings.buttonAppearance.rightButton.mainColorDisabled, []],
          backgroundColorDisabled: [settings.buttonAppearance.rightButton.backgroundColorDisabled, []]
        })
      })
    });
  }


  protected validatorTriggers(): string[] {
    return ['appearance.showValueBox', 'appearance.showBorder',
      'buttonAppearance.leftButton.showButton', 'buttonAppearance.rightButton.showButton'];
  }

  protected updateValidators(_emitEvent: boolean): void {
    const showValueBox: boolean = this.valueStepperWidgetSettingsForm.get('appearance').get('showValueBox').value;
    const showBorder: boolean = this.valueStepperWidgetSettingsForm.get('appearance').get('showBorder').value;
    const showLeftButton: boolean = this.valueStepperWidgetSettingsForm.get('buttonAppearance').get('leftButton').get('showButton').value;
    const showRightButton: boolean = this.valueStepperWidgetSettingsForm.get('buttonAppearance').get('rightButton').get('showButton').value;
    if (showValueBox) {
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueUnits').enable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueDecimals').enable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueFont').enable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueColor').enable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueBoxBackground').enable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('showBorder').enable({emitEvent: false});
      if (showBorder) {
        this.valueStepperWidgetSettingsForm.get('appearance').get('borderWidth').enable();
        this.valueStepperWidgetSettingsForm.get('appearance').get('borderColor').enable();
      } else {
        this.valueStepperWidgetSettingsForm.get('appearance').get('borderWidth').disable();
        this.valueStepperWidgetSettingsForm.get('appearance').get('borderColor').disable();
      }
    } else {
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueUnits').disable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueDecimals').disable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueFont').disable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueColor').disable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('valueBoxBackground').disable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('showBorder').disable({emitEvent: false});
      this.valueStepperWidgetSettingsForm.get('appearance').get('borderWidth').disable();
      this.valueStepperWidgetSettingsForm.get('appearance').get('borderColor').disable();
    }
    this.buttonValidators(showLeftButton, 'leftButton');
    this.buttonValidators(showRightButton, 'rightButton');
  }

  private buttonValidators(showButtonValue: boolean, button: string) {
    if (showButtonValue) {
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('icon').enable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('iconSize').enable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('iconSizeUnit').enable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('mainColorOn').enable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('backgroundColorOn').enable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('mainColorDisabled').enable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('backgroundColorDisabled').enable()
    } else {
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('icon').disable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('iconSize').disable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('iconSizeUnit').disable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('mainColorOn').disable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('backgroundColorOn').disable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('mainColorDisabled').disable()
      this.valueStepperWidgetSettingsForm.get('buttonAppearance').get(button).get('backgroundColorDisabled').disable()
    }
  }

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.valueStepperWidgetSettingsForm.get('appearance').get('valueUnits').value);
    const decimals: number = this.valueStepperWidgetSettingsForm.get('appearance').get('valueDecimals').value;
    return formatValue(48, decimals, units, false);
  }
}
