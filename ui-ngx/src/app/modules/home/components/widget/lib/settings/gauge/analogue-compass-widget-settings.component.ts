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

import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Component } from '@angular/core';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';

@Component({
  selector: 'tb-analogue-compass-widget-settings',
  templateUrl: './analogue-compass-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class AnalogueCompassWidgetSettingsComponent extends WidgetSettingsComponent {

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  analogueCompassWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.analogueCompassWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      majorTicks: [],
      minorTicks: 22,
      showStrokeTicks: false,
      needleCircleSize: 10,
      showBorder: true,
      borderOuterWidth: 10,
      colorPlate: '#222',
      colorMajorTicks: '#f5f5f5',
      colorMinorTicks: '#ddd',
      colorNeedle: '#f08080',
      colorNeedleCircle: '#e8e8e8',
      colorBorder: '#ccc',
      majorTickFont: {
        family: 'Roboto',
        size: 20,
        style: 'normal',
        weight: '500',
        color: '#ccc'
      },
      animation: true,
      animationDuration: 500,
      animationRule: 'cycle',
      animationTarget: 'needle'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.analogueCompassWidgetSettingsForm = this.fb.group({

      // Ticks settings
      majorTicks: [settings.majorTicks, []],
      colorMajorTicks: [settings.colorMajorTicks, []],
      minorTicks: [settings.minorTicks, [Validators.min(0)]],
      colorMinorTicks: [settings.colorMinorTicks, []],
      showStrokeTicks: [settings.showStrokeTicks, []],
      majorTickFont: [settings.majorTickFont, []],
      majorTickColor: [settings.majorTickFont.color, []],

      // Plate settings
      colorPlate: [settings.colorPlate, []],
      showBorder: [settings.showBorder, []],
      colorBorder: [settings.colorBorder, []],
      borderOuterWidth: [settings.borderOuterWidth, [Validators.min(0)]],

      // Needle settings
      needleCircleSize: [settings.needleCircleSize, [Validators.min(0)]],
      colorNeedle: [settings.colorNeedle, []],
      colorNeedleCircle: [settings.colorNeedleCircle, []],

      // Animation settings
      animation: [settings.animation, []],
      animationDuration: [settings.animationDuration, [Validators.min(0)]],
      animationRule: [settings.animationRule, []],
      animationTarget: [settings.animationTarget, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showBorder', 'animation'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showBorder: boolean = this.analogueCompassWidgetSettingsForm.get('showBorder').value;
    const animation: boolean = this.analogueCompassWidgetSettingsForm.get('animation').value;
    if (showBorder) {
      this.analogueCompassWidgetSettingsForm.get('colorBorder').enable();
      this.analogueCompassWidgetSettingsForm.get('borderOuterWidth').enable();
    } else {
      this.analogueCompassWidgetSettingsForm.get('colorBorder').disable();
      this.analogueCompassWidgetSettingsForm.get('borderOuterWidth').disable();
    }
    if (animation) {
      this.analogueCompassWidgetSettingsForm.get('animationDuration').enable();
      this.analogueCompassWidgetSettingsForm.get('animationRule').enable();
      this.analogueCompassWidgetSettingsForm.get('animationTarget').enable();
    } else {
      this.analogueCompassWidgetSettingsForm.get('animationDuration').disable();
      this.analogueCompassWidgetSettingsForm.get('animationRule').disable();
      this.analogueCompassWidgetSettingsForm.get('animationTarget').disable();
    }
    this.analogueCompassWidgetSettingsForm.get('colorBorder').updateValueAndValidity({emitEvent});
    this.analogueCompassWidgetSettingsForm.get('borderOuterWidth').updateValueAndValidity({emitEvent});
    this.analogueCompassWidgetSettingsForm.get('animationDuration').updateValueAndValidity({emitEvent});
    this.analogueCompassWidgetSettingsForm.get('animationRule').updateValueAndValidity({emitEvent});
    this.analogueCompassWidgetSettingsForm.get('animationTarget').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputSettings(settings) {
    settings.majorTickFont.color = this.analogueCompassWidgetSettingsForm.get('majorTickColor').value;
    return settings;
  }
}
