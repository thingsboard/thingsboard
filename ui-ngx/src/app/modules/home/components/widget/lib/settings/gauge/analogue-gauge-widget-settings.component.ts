///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GaugeHighlight } from '@home/components/widget/lib/settings/gauge/gauge-highlight.component';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

export class AnalogueGaugeWidgetSettingsComponent extends WidgetSettingsComponent {

  analogueGaugeWidgetSettingsForm: FormGroup;

  ctx = {
    settingsForm: null
  };

  constructor(protected store: Store<AppState>,
              protected fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.analogueGaugeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      startAngle: 45,
      ticksAngle: 270,
      needleCircleSize: 10,
      minValue: 0,
      maxValue: 100,
      showUnitTitle: true,
      unitTitle: null,
      majorTicksCount: null,
      minorTicks: 2,
      valueBox: true,
      valueInt: 3,
      defaultColor: null,
      colorPlate: '#fff',
      colorMajorTicks: '#444',
      colorMinorTicks: '#666',
      colorNeedle: null,
      colorNeedleEnd: null,
      colorNeedleShadowUp: 'rgba(2,255,255,0.2)',
      colorNeedleShadowDown: 'rgba(188,143,143,0.45)',
      colorValueBoxRect: '#888',
      colorValueBoxRectEnd: '#666',
      colorValueBoxBackground: '#babab2',
      colorValueBoxShadow: 'rgba(0,0,0,1)',
      highlights: [],
      highlightsWidth: 15,
      showBorder: true,
      numbersFont: {
        family: 'Roboto',
        size: 18,
        style: 'normal',
        weight: '500',
        color: null
      },
      titleFont: {
        family: 'Roboto',
        size: 24,
        style: 'normal',
        weight: '500',
        color: '#888'
      },
      unitsFont: {
        family: 'Roboto',
        size: 22,
        style: 'normal',
        weight: '500',
        color: '#888'
      },
      valueFont: {
        family: 'Roboto',
        size: 40,
        style: 'normal',
        weight: '500',
        color: '#444',
        shadowColor: 'rgba(0,0,0,0.3)'
      },
      animation: true,
      animationDuration: 500,
      animationRule: 'cycle'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.analogueGaugeWidgetSettingsForm = this.fb.group({

      // Radial gauge settings
      startAngle: [settings.startAngle, [Validators.min(0), Validators.max(360)]],
      ticksAngle: [settings.ticksAngle, [Validators.min(0), Validators.max(360)]],
      needleCircleSize: [settings.needleCircleSize, [Validators.min(0)]],

      defaultColor: [settings.defaultColor, []],

      // Ticks settings
      minValue: [settings.minValue, []],
      maxValue: [settings.maxValue, []],
      majorTicksCount: [settings.majorTicksCount, [Validators.min(0)]],
      colorMajorTicks: [settings.colorMajorTicks, []],
      minorTicks: [settings.majorTicksCount, [Validators.min(0)]],
      colorMinorTicks: [settings.colorMinorTicks, []],
      numbersFont: [settings.numbersFont, []],

      // Unit title settings
      showUnitTitle: [settings.showUnitTitle, []],
      unitTitle: [settings.unitTitle, []],
      titleFont: [settings.titleFont, []],

      // Units settings
      unitsFont: [settings.unitsFont, []],

      // Value box settings
      valueBox: [settings.valueBox, []],
      valueInt: [settings.valueInt, [Validators.min(0)]],
      valueFont: [settings.valueFont, []],
      colorValueBoxRect: [settings.colorValueBoxRect, []],
      colorValueBoxRectEnd: [settings.colorValueBoxRectEnd, []],
      colorValueBoxBackground: [settings.colorValueBoxBackground, []],
      colorValueBoxShadow: [settings.colorValueBoxShadow, []],

      // Plate settings
      showBorder: [settings.showBorder, []],
      colorPlate: [settings.colorPlate, []],

      // Needle settings
      colorNeedle: [settings.colorNeedle, []],
      colorNeedleEnd: [settings.colorNeedleEnd, []],
      colorNeedleShadowUp: [settings.colorNeedleShadowUp, []],
      colorNeedleShadowDown: [settings.colorNeedleShadowDown, []],

      // Highlights settings
      highlightsWidth: [settings.highlightsWidth, [Validators.min(0)]],
      highlights: this.prepareHighlightsFormArray(settings.highlights),

      // Animation settings
      animation: [settings.animation, []],
      animationDuration: [settings.animationDuration, [Validators.min(0)]],
      animationRule: [settings.animationRule, []],
    });
    this.ctx.settingsForm = this.analogueGaugeWidgetSettingsForm;
  }

  protected validatorTriggers(): string[] {
    return ['showUnitTitle', 'valueBox', 'animation'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showUnitTitle: boolean = this.analogueGaugeWidgetSettingsForm.get('showUnitTitle').value;
    const valueBox: boolean = this.analogueGaugeWidgetSettingsForm.get('valueBox').value;
    const animation: boolean = this.analogueGaugeWidgetSettingsForm.get('animation').value;
    if (showUnitTitle) {
      this.analogueGaugeWidgetSettingsForm.get('unitTitle').enable();
      this.analogueGaugeWidgetSettingsForm.get('titleFont').enable();
    } else {
      this.analogueGaugeWidgetSettingsForm.get('unitTitle').disable();
      this.analogueGaugeWidgetSettingsForm.get('titleFont').disable();
    }
    if (valueBox) {
      this.analogueGaugeWidgetSettingsForm.get('valueInt').enable();
      this.analogueGaugeWidgetSettingsForm.get('valueFont').enable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxRect').enable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxRectEnd').enable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxBackground').enable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxShadow').enable();
    } else {
      this.analogueGaugeWidgetSettingsForm.get('valueInt').disable();
      this.analogueGaugeWidgetSettingsForm.get('valueFont').disable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxRect').disable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxRectEnd').disable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxBackground').disable();
      this.analogueGaugeWidgetSettingsForm.get('colorValueBoxShadow').disable();
    }
    if (animation) {
      this.analogueGaugeWidgetSettingsForm.get('animationDuration').enable();
      this.analogueGaugeWidgetSettingsForm.get('animationRule').enable();
    } else {
      this.analogueGaugeWidgetSettingsForm.get('animationDuration').disable();
      this.analogueGaugeWidgetSettingsForm.get('animationRule').disable();
    }
    this.analogueGaugeWidgetSettingsForm.get('unitTitle').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('titleFont').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('valueInt').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('colorValueBoxRect').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('colorValueBoxRectEnd').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('colorValueBoxBackground').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('colorValueBoxShadow').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('animationDuration').updateValueAndValidity({emitEvent});
    this.analogueGaugeWidgetSettingsForm.get('animationRule').updateValueAndValidity({emitEvent});
  }

  protected doUpdateSettings(settingsForm: FormGroup, settings: WidgetSettings) {
    settingsForm.setControl('highlights', this.prepareHighlightsFormArray(settings.highlights), {emitEvent: false});
  }

  private prepareHighlightsFormArray(highlights: GaugeHighlight[] | undefined): FormArray {
    const highlightsControls: Array<AbstractControl> = [];
    if (highlights) {
      highlights.forEach((highlight) => {
        highlightsControls.push(this.fb.control(highlight, [Validators.required]));
      });
    }
    return this.fb.array(highlightsControls);
  }

  highlightsFormArray(): FormArray {
    return this.analogueGaugeWidgetSettingsForm.get('highlights') as FormArray;
  }

  public trackByHighlightControl(index: number, highlightControl: AbstractControl): any {
    return highlightControl;
  }

  public removeHighlight(index: number) {
    (this.analogueGaugeWidgetSettingsForm.get('highlights') as FormArray).removeAt(index);
  }

  public addHighlight() {
    const highlight: GaugeHighlight = {
      from: null,
      to: null,
      color: null
    };
    const highlightsArray = this.analogueGaugeWidgetSettingsForm.get('highlights') as FormArray;
    const highlightControl = this.fb.control(highlight, [Validators.required]);
    (highlightControl as any).new = true;
    highlightsArray.push(highlightControl);
    this.analogueGaugeWidgetSettingsForm.updateValueAndValidity();
  }

  highlightDrop(event: CdkDragDrop<string[]>) {
    const highlightsArray = this.analogueGaugeWidgetSettingsForm.get('highlights') as FormArray;
    const highlight = highlightsArray.at(event.previousIndex);
    highlightsArray.removeAt(event.previousIndex);
    highlightsArray.insert(event.currentIndex, highlight);
  }

}
