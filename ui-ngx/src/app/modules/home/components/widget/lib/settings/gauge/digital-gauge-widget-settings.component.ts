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

import { Datasource, WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { Component } from '@angular/core';
import {
  AbstractControl,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GaugeType } from '@home/components/widget/lib/canvas-digital-gauge';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  backwardCompatibilityFixedLevelColors,
  backwardCompatibilityTicks,
  digitalGaugeLayoutImages,
  digitalGaugeLayouts,
  digitalGaugeLayoutTranslations,
  DigitalGaugeType
} from '@home/components/widget/lib/digital-gauge.models';
import { formatValue, isDefined } from '@core/utils';
import {
  ColorSettings,
  ColorType,
  constantColor,
  simpleDateFormat,
  ValueSourceConfig,
  ValueSourceType
} from '@shared/models/widget-settings.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
    selector: 'tb-digital-gauge-widget-settings',
    templateUrl: './digital-gauge-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class DigitalGaugeWidgetSettingsComponent extends WidgetSettingsComponent {

  digitalGaugeType = DigitalGaugeType;
  digitalGaugeLayouts = digitalGaugeLayouts;

  digitalGaugeLayoutTranslationMap = digitalGaugeLayoutTranslations;
  digitalGaugeLayoutImageMap = digitalGaugeLayoutImages;

  digitalGaugeWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this, true);
  previewFn = this._valuePreviewFn.bind(this, false);

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
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
    if (!settings.barColor) {
      settings.barColor = constantColor(settings.defaultColor || '#2196f3');

      if (settings.fixedLevelColors.length) {
        settings.barColor.rangeList = {
          advancedMode: settings.useFixedLevelColor,
          range: null,
          rangeAdvanced: backwardCompatibilityFixedLevelColors(settings.fixedLevelColors)
        };
      }
      if (settings.levelColors.length) {
        settings.barColor.gradient = {
          advancedMode: false,
          gradient: settings.levelColors,
          gradientAdvanced: null
        };
      }
      if (settings.useFixedLevelColor) {
        settings.barColor.type = ColorType.range;
      } else if (settings.levelColors.length) {
        settings.barColor.type = ColorType.gradient;
      }
    }

    this.digitalGaugeWidgetSettingsForm = this.fb.group({

      gaugeType: [settings.gaugeType, []],
      donutStartAngle: [settings.donutStartAngle, []],
      showMinMax: [settings.showMinMax, []],
      minValue: [settings.minValue, []],
      maxValue: [settings.maxValue, [this.maxValueValidation()]],
      minMaxFont: [settings.minMaxFont, []],
      minMaxColor: [settings.minMaxFont.color, []],

      showValue: [settings.showValue, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueFont.color, []],

      showTitle: [settings.showTitle, []],
      title: [settings.title, []],
      titleFont: [settings.titleFont, []],
      titleColor: [settings.titleFont.color, []],

      showUnitTitle: [settings.showUnitTitle, []],
      unitTitle: [settings.unitTitle, []],
      showTimestamp: [settings.showTimestamp, []],
      timestampFormat: [simpleDateFormat(settings.timestampFormat), []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelFont.color, []],

      gaugeWidthScale: [settings.gaugeWidthScale, [Validators.min(0)]],
      neonGlowBrightness: [settings.neonGlowBrightness, [Validators.min(0), Validators.max(100)]],
      dashThickness: [settings.dashThickness, [Validators.min(0)]],
      roundedLineCap: [settings.roundedLineCap, []],

      gaugeColor: [settings.gaugeColor, []],
      barColor: [settings.barColor],

      showTicks: [settings.showTicks, []],
      tickWidth: [settings.tickWidth, [Validators.min(0)]],
      colorTicks: [settings.colorTicks, []],
      ticksValue: this.prepareTicksValueFormArray(backwardCompatibilityTicks(settings.ticksValue)),

      animation: [settings.animation, []],
      animationDuration: [settings.animationDuration, [Validators.min(0)]],
      animationRule: [settings.animationRule, []]
    });
  }

  private maxValueValidation(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value: string = control.value;
      if (value) {
        if (value < control.parent?.get('minValue').value) {
          return {maxValue: true};
        }
      }
      return null;
    };
  }

  protected prepareOutputSettings(settings) {

    const barColor: ColorSettings = this.digitalGaugeWidgetSettingsForm.get('barColor').value;

    if (barColor.type === ColorType.range) {
      settings.useFixedLevelColor = true;
      settings.fixedLevelColors = barColor.rangeList.advancedMode ? barColor.rangeList.rangeAdvanced : barColor.rangeList.range;
    } else {
      settings.useFixedLevelColor = false;
    }
    if (barColor.gradient?.gradient?.length) {
      settings.levelColors = barColor.gradient.gradient;
    }
    settings.barColor = this.digitalGaugeWidgetSettingsForm.get('barColor').value;
    settings.timestampFormat = this.digitalGaugeWidgetSettingsForm.get('timestampFormat').value.format;
    settings.minMaxFont.color = this.digitalGaugeWidgetSettingsForm.get('minMaxColor').value;
    settings.valueFont.color = this.digitalGaugeWidgetSettingsForm.get('valueColor').value;
    settings.titleFont.color = this.digitalGaugeWidgetSettingsForm.get('titleColor').value;
    settings.labelFont.color = this.digitalGaugeWidgetSettingsForm.get('labelColor').value;

    if (isDefined(settings.decimals)) {
      delete settings.decimals;
    }

    return settings;
  }

  protected validatorTriggers(): string[] {
    return ['gaugeType', 'showTitle', 'showUnitTitle', 'showValue', 'showMinMax', 'showTimestamp', 'showTicks', 'animation', 'minValue'];
  }

  protected updateValidators(emitEvent: boolean, trigger: string) {
    if (trigger === 'minValue') {
      this.digitalGaugeWidgetSettingsForm.get('maxValue').updateValueAndValidity({emitEvent: true});
      this.digitalGaugeWidgetSettingsForm.get('maxValue').markAsTouched({onlySelf: true});
      return;
    }
    const gaugeType: GaugeType = this.digitalGaugeWidgetSettingsForm.get('gaugeType').value;
    const showTitle: boolean = this.digitalGaugeWidgetSettingsForm.get('showTitle').value;
    const showUnitTitle: boolean = this.digitalGaugeWidgetSettingsForm.get('showUnitTitle').value;
    const showValue: boolean = this.digitalGaugeWidgetSettingsForm.get('showValue').value;
    const showMinMax: boolean = this.digitalGaugeWidgetSettingsForm.get('showMinMax').value;
    const showTimestamp: boolean = this.digitalGaugeWidgetSettingsForm.get('showTimestamp').value;
    const showTicks: boolean = this.digitalGaugeWidgetSettingsForm.get('showTicks').value;
    const animation: boolean = this.digitalGaugeWidgetSettingsForm.get('animation').value;


    if (gaugeType === 'donut') {
      this.digitalGaugeWidgetSettingsForm.get('donutStartAngle').enable();

      this.digitalGaugeWidgetSettingsForm.get('showMinMax').disable({emitEvent: false});
      this.digitalGaugeWidgetSettingsForm.get('minValue').enable({emitEvent: false});
      this.digitalGaugeWidgetSettingsForm.get('maxValue').enable({emitEvent: false});
      this.digitalGaugeWidgetSettingsForm.get('minMaxFont').disable({emitEvent: false});
      this.digitalGaugeWidgetSettingsForm.get('minMaxColor').disable({emitEvent: false});
    } else {
      this.digitalGaugeWidgetSettingsForm.get('donutStartAngle').disable();

      this.digitalGaugeWidgetSettingsForm.get('showMinMax').enable({emitEvent: false});
      if (showMinMax) {
        this.digitalGaugeWidgetSettingsForm.get('minValue').enable();
        this.digitalGaugeWidgetSettingsForm.get('maxValue').enable();
        this.digitalGaugeWidgetSettingsForm.get('minMaxFont').enable();
        this.digitalGaugeWidgetSettingsForm.get('minMaxColor').enable();
      } else {
        this.digitalGaugeWidgetSettingsForm.get('minValue').disable();
        this.digitalGaugeWidgetSettingsForm.get('maxValue').disable();
        this.digitalGaugeWidgetSettingsForm.get('minMaxFont').disable();
        this.digitalGaugeWidgetSettingsForm.get('minMaxColor').disable();
      }
    }

    if (showTitle) {
      this.digitalGaugeWidgetSettingsForm.get('title').enable();
      this.digitalGaugeWidgetSettingsForm.get('titleFont').enable();
      this.digitalGaugeWidgetSettingsForm.get('titleColor').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('title').disable();
      this.digitalGaugeWidgetSettingsForm.get('titleFont').disable();
      this.digitalGaugeWidgetSettingsForm.get('titleColor').disable();
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
      this.digitalGaugeWidgetSettingsForm.get('labelColor').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('labelFont').disable();
      this.digitalGaugeWidgetSettingsForm.get('labelColor').disable();
    }
    if (showValue) {
      this.digitalGaugeWidgetSettingsForm.get('valueFont').enable();
      this.digitalGaugeWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.digitalGaugeWidgetSettingsForm.get('valueFont').disable();
      this.digitalGaugeWidgetSettingsForm.get('valueColor').disable();
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
    this.digitalGaugeWidgetSettingsForm.get('titleColor').updateValueAndValidity({emitEvent});

    this.digitalGaugeWidgetSettingsForm.get('unitTitle').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('timestampFormat').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('labelFont').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('labelColor').updateValueAndValidity({emitEvent});

    this.digitalGaugeWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('valueColor').updateValueAndValidity({emitEvent});

    this.digitalGaugeWidgetSettingsForm.get('minValue').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('maxValue').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('minMaxFont').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('minMaxColor').updateValueAndValidity({emitEvent});

    this.digitalGaugeWidgetSettingsForm.get('tickWidth').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('colorTicks').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('ticksValue').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('animationDuration').updateValueAndValidity({emitEvent});
    this.digitalGaugeWidgetSettingsForm.get('animationRule').updateValueAndValidity({emitEvent});
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
    settingsForm.setControl('ticksValue', this.prepareTicksValueFormArray(settings.ticksValue), {emitEvent: false});
  }

  private prepareTicksValueFormArray(ticksValue: ValueSourceConfig[] | undefined): UntypedFormArray {
    const ticksValueControls: Array<AbstractControl> = [];
    if (ticksValue) {
      ticksValue.forEach((tickValue) => {
        ticksValueControls.push(this.fb.control(tickValue, [Validators.required]));
      });
    }
    return this.fb.array(ticksValueControls);
  }

  tickValuesFormArray(): UntypedFormArray {
    return this.digitalGaugeWidgetSettingsForm.get('ticksValue') as UntypedFormArray;
  }

  public trackByTickValue(index: number, tickValueControl: AbstractControl): any {
    return tickValueControl;
  }

  public removeTickValue(index: number) {
    (this.digitalGaugeWidgetSettingsForm.get('ticksValue') as UntypedFormArray).removeAt(index);
  }

  public addTickValue() {
    const tickValue: ValueSourceConfig = {
      type: ValueSourceType.constant
    };
    const tickValuesArray = this.digitalGaugeWidgetSettingsForm.get('ticksValue') as UntypedFormArray;
    const tickValueControl = this.fb.control(tickValue, []);
    (tickValueControl as any).new = true;
    tickValuesArray.push(tickValueControl);
    this.digitalGaugeWidgetSettingsForm.updateValueAndValidity();
  }

  tickValueDrop(event: CdkDragDrop<string[]>) {
    const tickValuesArray = this.digitalGaugeWidgetSettingsForm.get('ticksValue') as UntypedFormArray;
    const tickValue = tickValuesArray.at(event.previousIndex);
    tickValuesArray.removeAt(event.previousIndex);
    tickValuesArray.insert(event.currentIndex, tickValue);
  }

  private _valuePreviewFn(units: boolean): string {
    return formatValue(22, 0, units ? getSourceTbUnitSymbol(this.widget.config.units) : null, true);
  }
}
