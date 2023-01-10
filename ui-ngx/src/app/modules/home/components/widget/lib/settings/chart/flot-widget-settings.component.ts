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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { ChartType, TbFlotSettings } from '@home/components/widget/lib/flot-widget.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { ComparisonDuration } from '@shared/models/time/time.models';
import { WidgetService } from '@core/http/widget.service';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  LabelDataKey,
  labelDataKeyValidator
} from '@home/components/widget/lib/settings/chart/label-data-key.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

export function flotDefaultSettings(chartType: ChartType): Partial<TbFlotSettings> {
  const settings: Partial<TbFlotSettings> = {
    stack: false,
    fontColor: '#545454',
    fontSize: 10,
    showTooltip: true,
    tooltipIndividual: false,
    tooltipCumulative: false,
    hideZeros: false,
    tooltipValueFormatter: '',
    grid: {
      verticalLines: true,
      horizontalLines: true,
      outlineWidth: 1,
      color: '#545454',
      backgroundColor: null,
      tickColor: '#DDDDDD'
    },
    xaxis: {
      title: null,
      showLabels: true,
      color: null
    },
    yaxis: {
      min: null,
      max: null,
      title: null,
      showLabels: true,
      color: null,
      tickSize: null,
      tickDecimals: 0,
      ticksFormatter: ''
    }
  };
  if (chartType === 'graph') {
    settings.smoothLines = false;
    settings.shadowSize = 4;
  }
  if (chartType === 'bar') {
    settings.defaultBarWidth = 600;
    settings.barAlignment = 'left';
  }
  if (chartType === 'graph' || chartType === 'bar') {
    settings.thresholdsLineWidth = null;
    settings.comparisonEnabled = false;
    settings.timeForComparison = 'previousInterval';
    settings.comparisonCustomIntervalValue = 7200000;
    settings.xaxisSecond = {
      title: null,
      axisPosition: 'top',
      showLabels: true
    };
    settings.customLegendEnabled = false;
    settings.dataKeysListForLabels = [];
  }
  return settings;
}

@Component({
  selector: 'tb-flot-widget-settings',
  templateUrl: './flot-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FlotWidgetSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => FlotWidgetSettingsComponent),
      multi: true,
    }
  ]
})
export class FlotWidgetSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  chartType: ChartType;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: TbFlotSettings;

  private propagateChange = null;

  public flotSettingsFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.flotSettingsFormGroup = this.fb.group({

      // Common settings

      stack: [false, []],
      fontSize: [10, [Validators.min(0)]],
      fontColor: ['#545454', []],

      // Tooltip settings

      showTooltip: [true, []],
      tooltipIndividual: [false, []],
      tooltipCumulative: [false, []],
      hideZeros: [false, []],
      tooltipValueFormatter: ['', []],

      // Grid settings

      grid: this.fb.group({
        verticalLines: [true, []],
        horizontalLines: [true, []],
        outlineWidth: [1, [Validators.min(0)]],
        color: ['#545454', []],
        backgroundColor: [null, []],
        tickColor: ['#DDDDDD', []]
      }),

      // X axis settings

      xaxis: this.fb.group({
        title: [null, []],

        // --> X axis tick labels settings

        showLabels: [true, []],
        color: [null, []],
      }),

      // Y axis settings

      yaxis: this.fb.group({
        min: [null, []],
        max: [null, []],
        title: [null, []],

        // --> Y axis tick labels settings

        showLabels: [true, []],
        color: [null, []],
        tickSize: [null, [Validators.min(0)]],
        tickDecimals: [0, [Validators.min(0)]],
        ticksFormatter: ['', []]
      })
    });
    if (this.chartType === 'graph') {
      // Common settings
      this.flotSettingsFormGroup.addControl('shadowSize', this.fb.control(4, [Validators.min(0)]));
      this.flotSettingsFormGroup.addControl('smoothLines', this.fb.control(false, []));
    } else if (this.chartType === 'bar') {
      // Common settings
      this.flotSettingsFormGroup.addControl('defaultBarWidth', this.fb.control(600, [Validators.min(0)]));
      this.flotSettingsFormGroup.addControl('barAlignment', this.fb.control('left', []));
    }
    if (this.chartType === 'graph' || this.chartType === 'bar') {
      // Common settings
      this.flotSettingsFormGroup.addControl('thresholdsLineWidth', this.fb.control(null, [Validators.min(0)]));

      // Comparison settings

      this.flotSettingsFormGroup.addControl('comparisonEnabled', this.fb.control(false, []));
      this.flotSettingsFormGroup.addControl('timeForComparison', this.fb.control('previousInterval', []));
      this.flotSettingsFormGroup.addControl('comparisonCustomIntervalValue', this.fb.control(7200000, [Validators.min(0)]));

      // --> Comparison X axis settings

      this.flotSettingsFormGroup.addControl('xaxisSecond', this.fb.group({
        axisPosition: ['top', []],
        title: [null, []],
        showLabels: [true, []]
      }));

      // Custom legend settings

      this.flotSettingsFormGroup.addControl('customLegendEnabled', this.fb.control(false, []));
      this.flotSettingsFormGroup.addControl('dataKeysListForLabels', this.fb.control(this.fb.array([]), []));
    }

    this.flotSettingsFormGroup.get('showTooltip').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });

    this.flotSettingsFormGroup.get('xaxis.showLabels').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });

    this.flotSettingsFormGroup.get('yaxis.showLabels').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });

    if (this.chartType === 'graph' || this.chartType === 'bar') {
      this.flotSettingsFormGroup.get('comparisonEnabled').valueChanges.subscribe(() => {
        this.updateValidators(true);
      });
      this.flotSettingsFormGroup.get('timeForComparison').valueChanges.subscribe(() => {
        this.updateValidators(true);
      });
      this.flotSettingsFormGroup.get('customLegendEnabled').valueChanges.subscribe(() => {
        this.updateValidators(true);
      });
    }

    this.flotSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });

    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.flotSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.flotSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TbFlotSettings): void {
    const dataKeysListForLabels = value?.dataKeysListForLabels;
    this.modelValue = value;
    this.flotSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    const dataKeysListForLabelsControls: Array<AbstractControl> = [];
    if (dataKeysListForLabels && dataKeysListForLabels.length) {
      dataKeysListForLabels.forEach((labelDataKey) => {
        dataKeysListForLabelsControls.push(this.fb.control(labelDataKey, [labelDataKeyValidator]));
      });
    }
    this.flotSettingsFormGroup.setControl('dataKeysListForLabels', this.fb.array(dataKeysListForLabelsControls), {emitEvent: false});
    this.updateValidators(false);
  }

  validate(c: FormControl) {
    return (this.flotSettingsFormGroup.valid) ? null : {
      flotSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: TbFlotSettings = this.flotSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showTooltip: boolean = this.flotSettingsFormGroup.get('showTooltip').value;
    const xaxisShowLabels: boolean = this.flotSettingsFormGroup.get('xaxis.showLabels').value;
    const yaxisShowLabels: boolean = this.flotSettingsFormGroup.get('yaxis.showLabels').value;

    if (showTooltip) {
      this.flotSettingsFormGroup.get('tooltipIndividual').enable({emitEvent});
      this.flotSettingsFormGroup.get('tooltipCumulative').enable({emitEvent});
      this.flotSettingsFormGroup.get('hideZeros').enable({emitEvent});
      this.flotSettingsFormGroup.get('tooltipValueFormatter').enable({emitEvent});
    } else {
      this.flotSettingsFormGroup.get('tooltipIndividual').disable({emitEvent});
      this.flotSettingsFormGroup.get('tooltipCumulative').disable({emitEvent});
      this.flotSettingsFormGroup.get('hideZeros').disable({emitEvent});
      this.flotSettingsFormGroup.get('tooltipValueFormatter').disable({emitEvent});
    }

    if (xaxisShowLabels) {
      this.flotSettingsFormGroup.get('xaxis.color').enable({emitEvent});
    } else {
      this.flotSettingsFormGroup.get('xaxis.color').disable({emitEvent});
    }

    if (yaxisShowLabels) {
      this.flotSettingsFormGroup.get('yaxis.color').enable({emitEvent});
      this.flotSettingsFormGroup.get('yaxis.tickSize').enable({emitEvent});
      this.flotSettingsFormGroup.get('yaxis.tickDecimals').enable({emitEvent});
      this.flotSettingsFormGroup.get('yaxis.ticksFormatter').enable({emitEvent});
    } else {
      this.flotSettingsFormGroup.get('yaxis.color').disable({emitEvent});
      this.flotSettingsFormGroup.get('yaxis.tickSize').disable({emitEvent});
      this.flotSettingsFormGroup.get('yaxis.tickDecimals').disable({emitEvent});
      this.flotSettingsFormGroup.get('yaxis.ticksFormatter').disable({emitEvent});
    }

    this.flotSettingsFormGroup.get('tooltipIndividual').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('tooltipCumulative').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('hideZeros').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('tooltipValueFormatter').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('xaxis.color').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('yaxis.color').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('yaxis.tickSize').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('yaxis.tickDecimals').updateValueAndValidity({emitEvent: false});
    this.flotSettingsFormGroup.get('yaxis.ticksFormatter').updateValueAndValidity({emitEvent: false});

    if (this.chartType === 'graph' || this.chartType === 'bar') {
      const comparisonEnabled: boolean = this.flotSettingsFormGroup.get('comparisonEnabled').value;
      const timeForComparison: ComparisonDuration = this.flotSettingsFormGroup.get('timeForComparison').value;
      const customLegendEnabled: boolean = this.flotSettingsFormGroup.get('customLegendEnabled').value;
      if (comparisonEnabled) {
        this.flotSettingsFormGroup.get('timeForComparison').enable({emitEvent: false});
        if (timeForComparison === 'customInterval') {
          this.flotSettingsFormGroup.get('comparisonCustomIntervalValue').enable({emitEvent});
        } else {
          this.flotSettingsFormGroup.get('comparisonCustomIntervalValue').disable({emitEvent});
        }
      } else {
        this.flotSettingsFormGroup.get('timeForComparison').disable({emitEvent: false});
        this.flotSettingsFormGroup.get('comparisonCustomIntervalValue').disable({emitEvent});
      }
      if (customLegendEnabled) {
        this.flotSettingsFormGroup.get('dataKeysListForLabels').enable({emitEvent});
      } else {
        this.flotSettingsFormGroup.get('dataKeysListForLabels').disable({emitEvent});
      }

      this.flotSettingsFormGroup.get('timeForComparison').updateValueAndValidity({emitEvent: false});
      this.flotSettingsFormGroup.get('comparisonCustomIntervalValue').updateValueAndValidity({emitEvent: false});
      this.flotSettingsFormGroup.get('dataKeysListForLabels').updateValueAndValidity({emitEvent: false});
    }
  }

  dataKeysListForLabelsFormArray(): FormArray {
    return this.flotSettingsFormGroup.get('dataKeysListForLabels') as FormArray;
  }

  public trackByLabelDataKey(index: number, labelDataKeyControl: AbstractControl): any {
    return labelDataKeyControl;
  }

  public removeLabelDataKey(index: number) {
    (this.flotSettingsFormGroup.get('dataKeysListForLabels') as FormArray).removeAt(index);
  }

  public addLabelDataKey() {
    const labelDataKey: LabelDataKey = {
      name: null,
      type: DataKeyType.attribute
    };
    const dataKeysListForLabelsArray = this.flotSettingsFormGroup.get('dataKeysListForLabels') as FormArray;
    const labelDataKeyControl = this.fb.control(labelDataKey, [labelDataKeyValidator]);
    (labelDataKeyControl as any).new = true;
    dataKeysListForLabelsArray.push(labelDataKeyControl);
    this.flotSettingsFormGroup.updateValueAndValidity();
  }

  labelDataKeyDrop(event: CdkDragDrop<string[]>) {
    const dataKeysListForLabelsArray = this.flotSettingsFormGroup.get('dataKeysListForLabels') as FormArray;
    const labelDataKey = dataKeysListForLabelsArray.at(event.previousIndex);
    dataKeysListForLabelsArray.removeAt(event.previousIndex);
    dataKeysListForLabelsArray.insert(event.currentIndex, labelDataKey);
  }

}
