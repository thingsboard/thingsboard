///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { Component } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GaugeType } from '@home/components/widget/lib/canvas-digital-gauge';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  FixedColorLevel,
  fixedColorLevelValidator
} from '@home/components/widget/lib/settings/gauge/fixed-color-level.component';
import { ValueSourceProperty } from '@home/components/widget/lib/settings/common/value-source.component';

@Component({
  selector: 'tb-digital-gauge-widget-settings',
  templateUrl: './digital-gauge-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class DigitalGaugeWidgetSettingsComponent extends WidgetSettingsComponent {

  digitalGaugeWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              protected fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.digitalGaugeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      minValue: 0,
      maxValue: 100,
      gaugeType: 'arc',
      donutStartAngle: 90,
      neonGlowBrightness: 0,
      dashThickness: 0,
      roundedLineCap: false,
      title: null,
      showTitle: false,
      unitTitle: null,
      showUnitTitle: false,
      showTimestamp: false,
      timestampFormat: 'yyyy-MM-dd HH:mm:ss',
      showValue: true,
      showMinMax: true,
      gaugeWidthScale: 0.75,
      defaultColor: null,
      gaugeColor: null,
      useFixedLevelColor: false,
      levelColors: [],
      fixedLevelColors: [],
      showTicks: false,
      tickWidth: 4,
      colorTicks: '#666',
      ticksValue: [],
      animation: true,
      animationDuration: 500,
      animationRule: 'linear',
      titleFont: {
        family: 'Roboto',
        size: 12,
        style: 'normal',
        weight: '500',
        color: null
      },
      labelFont: {
        family: 'Roboto',
        size: 8,
        style: 'normal',
        weight: '500',
        color: null
      },
      valueFont: {
        family: 'Roboto',
        size: 18,
        style: 'normal',
        weight: '500',
        color: null
      },
      minMaxFont: {
        family: 'Roboto',
        size: 10,
        style: 'normal',
        weight: '500',
        color: null
      }
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.digitalGaugeWidgetSettingsForm = this.fb.group({

      // Common gauge settings
      minValue: [settings.minValue, []],
      maxValue: [settings.maxValue, []],
      gaugeType: [settings.gaugeType, []],
      donutStartAngle: [settings.donutStartAngle, []],
      defaultColor: [settings.defaultColor, []],

      // Gauge bar settings
      gaugeWidthScale: [settings.gaugeWidthScale, [Validators.min(0)]],
      neonGlowBrightness: [settings.neonGlowBrightness, [Validators.min(0), Validators.max(100)]],
      dashThickness: [settings.dashThickness, [Validators.min(0)]],
      roundedLineCap: [settings.roundedLineCap, []],

      // Gauge bar colors settings
      gaugeColor: [settings.gaugeColor, []],
      useFixedLevelColor: [settings.useFixedLevelColor, []],
      levelColors: this.prepareLevelColorFormArray(settings.levelColors),
      fixedLevelColors: this.prepareFixedLevelColorFormArray(settings.fixedLevelColors),

      // Title settings
      showTitle: [settings.showTitle, []],
      title: [settings.title, []],
      titleFont: [settings.titleFont, []],

      // Unit title/timestamp settings
      showUnitTitle: [settings.showUnitTitle, []],
      unitTitle: [settings.unitTitle, []],
      showTimestamp: [settings.showTimestamp, []],
      timestampFormat: [settings.timestampFormat, []],
      labelFont: [settings.labelFont, []],

      // Value settings
      showValue: [settings.showValue, []],
      valueFont: [settings.valueFont, []],

      // Min/max labels settings
      showMinMax: [settings.showMinMax, []],
      minMaxFont: [settings.minMaxFont, []],

      // Ticks settings
      showTicks: [settings.showTicks, []],
      tickWidth: [settings.tickWidth, [Validators.min(0)]],
      colorTicks: [settings.colorTicks, []],
      ticksValue: this.prepareTicksValueFormArray(settings.ticksValue),

      // Animation settings
      animation: [settings.animation, []],
      animationDuration: [settings.animationDuration, [Validators.min(0)]],
      animationRule: [settings.animationRule, []]

    });
  }

  protected validatorTriggers(): string[] {
    return ['gaugeType', 'showTitle', 'showUnitTitle', 'showValue', 'showMinMax', 'showTimestamp', 'useFixedLevelColor', 'showTicks', 'animation'];
  }

  protected updateValidators(emitEvent: boolean) {
    const gaugeType: GaugeType = this.digitalGaugeWidgetSettingsForm.get('gaugeType').value;
    const showTitle: boolean = this.digitalGaugeWidgetSettingsForm.get('showTitle').value;
    const showUnitTitle: boolean = this.digitalGaugeWidgetSettingsForm.get('showUnitTitle').value;
    const showValue: boolean = this.digitalGaugeWidgetSettingsForm.get('showValue').value;
    const showMinMax: boolean = this.digitalGaugeWidgetSettingsForm.get('showMinMax').value;
    const showTimestamp: boolean = this.digitalGaugeWidgetSettingsForm.get('showTimestamp').value;
    const useFixedLevelColor: boolean = this.digitalGaugeWidgetSettingsForm.get('useFixedLevelColor').value;
    const showTicks: boolean = this.digitalGaugeWidgetSettingsForm.get('showTicks').value;
    const animation: boolean = this.digitalGaugeWidgetSettingsForm.get('animation').value;

    if (gaugeType === 'donut') {
      this.digitalGaugeWidgetSettingsForm.get('donutStartAngle').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('donutStartAngle').disable();
    }
    if (showTitle) {
      this.digitalGaugeWidgetSettingsForm.get('title').enable();
      this.digitalGaugeWidgetSettingsForm.get('titleFont').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('title').disable();
      this.digitalGaugeWidgetSettingsForm.get('titleFont').disable();
    }
    if (showUnitTitle) {
      this.digitalGaugeWidgetSettingsForm.get('unitTitle').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('unitTitle').disable();
    }
    if (showTimestamp) {
      this.digitalGaugeWidgetSettingsForm.get('timestampFormat').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('timestampFormat').disable();
    }
    if (showUnitTitle || showTimestamp) {
      this.digitalGaugeWidgetSettingsForm.get('labelFont').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('labelFont').disable();
    }
    if (showValue) {
      this.digitalGaugeWidgetSettingsForm.get('valueFont').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('valueFont').disable();
    }
    if (showMinMax) {
      this.digitalGaugeWidgetSettingsForm.get('minMaxFont').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('minMaxFont').disable();
    }
    if (useFixedLevelColor) {
      this.digitalGaugeWidgetSettingsForm.get('fixedLevelColors').enable();
      this.digitalGaugeWidgetSettingsForm.get('levelColors').disable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('fixedLevelColors').disable();
      this.digitalGaugeWidgetSettingsForm.get('levelColors').enable();
    }
    if (showTicks) {
      this.digitalGaugeWidgetSettingsForm.get('tickWidth').enable();
      this.digitalGaugeWidgetSettingsForm.get('colorTicks').enable();
      this.digitalGaugeWidgetSettingsForm.get('ticksValue').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('tickWidth').disable();
      this.digitalGaugeWidgetSettingsForm.get('colorTicks').disable();
      this.digitalGaugeWidgetSettingsForm.get('ticksValue').disable();
    }
    if (animation) {
      this.digitalGaugeWidgetSettingsForm.get('animationDuration').enable();
      this.digitalGaugeWidgetSettingsForm.get('animationRule').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('animationDuration').disable();
      this.digitalGaugeWidgetSettingsForm.get('animationRule').disable();
    }
    this.digitalGaugeWidgetSettingsForm.get('donutStartAngle').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('title').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('unitTitle').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('timestampFormat').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('labelFont').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('minMaxFont').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('fixedLevelColors').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('levelColors').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('tickWidth').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('colorTicks').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('ticksValue').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('animationDuration').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('animationRule').updateValueAndValidity({emitEvent});
  }

  protected doUpdateSettings(settingsForm: FormGroup, settings: WidgetSettings) {
    settingsForm.setControl('levelColors', this.prepareLevelColorFormArray(settings.levelColors), {emitEvent: false});
    settingsForm.setControl('fixedLevelColors', this.prepareFixedLevelColorFormArray(settings.fixedLevelColors), {emitEvent: false});
    settingsForm.setControl('ticksValue', this.prepareTicksValueFormArray(settings.ticksValue), {emitEvent: false});
  }

  private prepareLevelColorFormArray(levelColors: string[] | undefined): FormArray {
    const levelColorsControls: Array<AbstractControl> = [];
    if (levelColors) {
      levelColors.forEach((levelColor) => {
        levelColorsControls.push(this.fb.control(levelColor, [Validators.required]));
      });
    }
    return this.fb.array(levelColorsControls);
  }

  private prepareFixedLevelColorFormArray(fixedLevelColors: FixedColorLevel[] | undefined): FormArray {
    const fixedLevelColorsControls: Array<AbstractControl> = [];
    if (fixedLevelColors) {
      fixedLevelColors.forEach((fixedLevelColor) => {
        fixedLevelColorsControls.push(this.fb.control(fixedLevelColor, [fixedColorLevelValidator]));
      });
    }
    return this.fb.array(fixedLevelColorsControls);
  }

  private prepareTicksValueFormArray(ticksValue: ValueSourceProperty[] | undefined): FormArray {
    const ticksValueControls: Array<AbstractControl> = [];
    if (ticksValue) {
      ticksValue.forEach((tickValue) => {
        ticksValueControls.push(this.fb.control(tickValue, [Validators.required]));
      });
    }
    return this.fb.array(ticksValueControls);
  }

  levelColorsFormArray(): FormArray {
    return this.digitalGaugeWidgetSettingsForm.get('levelColors') as FormArray;
  }

  public trackByLevelColor(index: number, levelColorControl: AbstractControl): any {
    return levelColorControl;
  }

  public removeLevelColor(index: number) {
    (this.digitalGaugeWidgetSettingsForm.get('levelColors') as FormArray).removeAt(index);
  }

  public addLevelColor() {
    const levelColorsArray = this.digitalGaugeWidgetSettingsForm.get('levelColors') as FormArray;
    const levelColorControl = this.fb.control(null, []);
    levelColorsArray.push(levelColorControl);
    this.digitalGaugeWidgetSettingsForm.updateValueAndValidity();
  }

  levelColorDrop(event: CdkDragDrop<string[]>) {
    const levelColorsArray = this.digitalGaugeWidgetSettingsForm.get('levelColors') as FormArray;
    const levelColor = levelColorsArray.at(event.previousIndex);
    levelColorsArray.removeAt(event.previousIndex);
    levelColorsArray.insert(event.currentIndex, levelColor);
  }

  fixedLevelColorFormArray(): FormArray {
    return this.digitalGaugeWidgetSettingsForm.get('fixedLevelColors') as FormArray;
  }

  public trackByFixedLevelColor(index: number, fixedLevelColorControl: AbstractControl): any {
    return fixedLevelColorControl;
  }

  public removeFixedLevelColor(index: number) {
    (this.digitalGaugeWidgetSettingsForm.get('fixedLevelColors') as FormArray).removeAt(index);
  }

  public addFixedLevelColor() {
    const fixedLevelColor: FixedColorLevel = {
      from: {
        valueSource: 'predefinedValue'
      },
      to: {
        valueSource: 'predefinedValue'
      },
      color: null
    };
    const fixedLevelColorsArray = this.digitalGaugeWidgetSettingsForm.get('fixedLevelColors') as FormArray;
    const fixedLevelColorControl = this.fb.control(fixedLevelColor, [fixedColorLevelValidator]);
    (fixedLevelColorControl as any).new = true;
    fixedLevelColorsArray.push(fixedLevelColorControl);
    this.digitalGaugeWidgetSettingsForm.updateValueAndValidity();
    if (!this.digitalGaugeWidgetSettingsForm.valid) {
      this.onSettingsChanged(this.digitalGaugeWidgetSettingsForm.value);
    }
  }

  fixedLevelColorDrop(event: CdkDragDrop<string[]>) {
    const fixedLevelColorsArray = this.digitalGaugeWidgetSettingsForm.get('fixedLevelColors') as FormArray;
    const fixedLevelColor = fixedLevelColorsArray.at(event.previousIndex);
    fixedLevelColorsArray.removeAt(event.previousIndex);
    fixedLevelColorsArray.insert(event.currentIndex, fixedLevelColor);
  }

  tickValuesFormArray(): FormArray {
    return this.digitalGaugeWidgetSettingsForm.get('ticksValue') as FormArray;
  }

  public trackByTickValue(index: number, tickValueControl: AbstractControl): any {
    return tickValueControl;
  }

  public removeTickValue(index: number) {
    (this.digitalGaugeWidgetSettingsForm.get('ticksValue') as FormArray).removeAt(index);
  }

  public addTickValue() {
    const tickValue: ValueSourceProperty = {
      valueSource: 'predefinedValue'
    };
    const tickValuesArray = this.digitalGaugeWidgetSettingsForm.get('ticksValue') as FormArray;
    const tickValueControl = this.fb.control(tickValue, []);
    (tickValueControl as any).new = true;
    tickValuesArray.push(tickValueControl);
    this.digitalGaugeWidgetSettingsForm.updateValueAndValidity();
  }

  tickValueDrop(event: CdkDragDrop<string[]>) {
    const tickValuesArray = this.digitalGaugeWidgetSettingsForm.get('ticksValue') as FormArray;
    const tickValue = tickValuesArray.at(event.previousIndex);
    tickValuesArray.removeAt(event.previousIndex);
    tickValuesArray.insert(event.currentIndex, tickValue);
  }

}
