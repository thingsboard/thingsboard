///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors, Validator,
  Validators
} from '@angular/forms';
import {
  AxisPosition, defaultXAxisTicksFormat,
  timeSeriesAxisPositionTranslations,
  TimeSeriesChartAxisSettings, TimeSeriesChartXAxisSettings,
  TimeSeriesChartYAxisSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { merge } from 'rxjs';
import { coerceBoolean } from '@shared/decorators/coercion';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { IAliasController } from '@app/core/public-api';
import { Datasource } from '@app/shared/public-api';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';

@Component({
  selector: 'tb-time-series-chart-axis-settings',
  templateUrl: './time-series-chart-axis-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartAxisSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TimeSeriesChartAxisSettingsComponent),
      multi: true
    }
  ]
})
export class TimeSeriesChartAxisSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  @coerceBoolean()
  alwaysExpanded = false;

  settingsExpanded = false;

  axisPositions: AxisPosition[];

  timeSeriesAxisPositionTranslations = timeSeriesAxisPositionTranslations;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  defaultXAxisTicksFormat = defaultXAxisTicksFormat;

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  disabled: boolean;

  @Input()
  axisType: 'xAxis' | 'yAxis' = 'xAxis';

  @Input()
  @coerceBoolean()
  advanced = false;

  @Input()
  @coerceBoolean()
  hideUnits = false;

  @Input()
  @coerceBoolean()
  hideDecimals = false;

  private modelValue: TimeSeriesChartXAxisSettings | TimeSeriesChartYAxisSettings;

  private propagateChange = null;

  public axisSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

    this.axisPositions = this.axisType === 'xAxis' ? [AxisPosition.top, AxisPosition.bottom] :
      [AxisPosition.left, AxisPosition.right];

    this.axisSettingsFormGroup = this.fb.group({
      show: [null, []],
      label: [null, []],
      labelFont: [null, []],
      labelColor: [null, []],
      position: [null, []],
      showTickLabels: [null, []],
      tickLabelFont: [null, []],
      tickLabelColor: [null, []],
      showTicks: [null, []],
      ticksColor: [null, []],
      showLine: [null, []],
      lineColor: [null, []],
      showSplitLines: [null, []],
      splitLinesColor: [null, []]
    });
    if (this.axisType === 'yAxis') {
      this.axisSettingsFormGroup.addControl('units', this.fb.control(null, []));
      this.axisSettingsFormGroup.addControl('decimals', this.fb.control(null, [Validators.min(0)]));
      this.axisSettingsFormGroup.addControl('ticksFormatter', this.fb.control(null, []));
      this.axisSettingsFormGroup.addControl('ticksGenerator', this.fb.control(null, []));
      this.axisSettingsFormGroup.addControl('interval', this.fb.control(null, [Validators.min(0)]));
      this.axisSettingsFormGroup.addControl('splitNumber', this.fb.control(null, [Validators.min(1)]));
      this.axisSettingsFormGroup.addControl('min', this.fb.control(null, []));
      this.axisSettingsFormGroup.addControl('max', this.fb.control(null, []));
    } else if (this.axisType === 'xAxis') {
      this.axisSettingsFormGroup.addControl('ticksFormat', this.fb.control(null, []));
    }
    this.axisSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.axisSettingsFormGroup.get('show').valueChanges,
          this.axisSettingsFormGroup.get('showTickLabels').valueChanges,
          this.axisSettingsFormGroup.get('showTicks').valueChanges,
          this.axisSettingsFormGroup.get('showLine').valueChanges,
          this.axisSettingsFormGroup.get('showSplitLines').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.axisSettingsFormGroup.valid ? null : {
      axisSettings: false
    };
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.axisSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.axisSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartAxisSettings | TimeSeriesChartYAxisSettings): void {
    this.modelValue = value;
    this.axisSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.axisSettingsFormGroup.get('show').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((show) => {
      this.settingsExpanded = show;
    });
  }

  private updateValidators() {
    const show: boolean = this.axisSettingsFormGroup.get('show').value;
    const showTickLabels: boolean = this.axisSettingsFormGroup.get('showTickLabels').value;
    const showTicks: boolean = this.axisSettingsFormGroup.get('showTicks').value;
    const showLine: boolean = this.axisSettingsFormGroup.get('showLine').value;
    const showSplitLines: boolean = this.axisSettingsFormGroup.get('showSplitLines').value;
    if (show) {
      this.axisSettingsFormGroup.enable({emitEvent: false});
      if (showTickLabels) {
        this.axisSettingsFormGroup.get('tickLabelFont').enable({emitEvent: false});
        this.axisSettingsFormGroup.get('tickLabelColor').enable({emitEvent: false});
        if (this.axisType === 'yAxis') {
          this.axisSettingsFormGroup.get('ticksFormatter').enable({emitEvent: false});
        }
        if (this.axisType === 'xAxis') {
          this.axisSettingsFormGroup.get('ticksFormat').enable({emitEvent: false});
        }
      } else {
        this.axisSettingsFormGroup.get('tickLabelFont').disable({emitEvent: false});
        this.axisSettingsFormGroup.get('tickLabelColor').disable({emitEvent: false});
        if (this.axisType === 'yAxis') {
          this.axisSettingsFormGroup.get('ticksFormatter').disable({emitEvent: false});
        }
        if (this.axisType === 'xAxis') {
          this.axisSettingsFormGroup.get('ticksFormat').disable({emitEvent: false});
        }
      }
      if (showTicks) {
        this.axisSettingsFormGroup.get('ticksColor').enable({emitEvent: false});
      } else {
        this.axisSettingsFormGroup.get('ticksColor').disable({emitEvent: false});
      }
      if (showLine) {
        this.axisSettingsFormGroup.get('lineColor').enable({emitEvent: false});
      } else {
        this.axisSettingsFormGroup.get('lineColor').disable({emitEvent: false});
      }
      if (showSplitLines) {
        this.axisSettingsFormGroup.get('splitLinesColor').enable({emitEvent: false});
      } else {
        this.axisSettingsFormGroup.get('splitLinesColor').disable({emitEvent: false});
      }
    } else {
      this.axisSettingsFormGroup.disable({emitEvent: false});
      this.axisSettingsFormGroup.get('show').enable({emitEvent: false});
      if (this.axisType === 'yAxis') {
        this.axisSettingsFormGroup.get('min').enable({emitEvent: false});
        this.axisSettingsFormGroup.get('max').enable({emitEvent: false});
      }
    }
  }

  private updateModel() {
    this.modelValue = this.axisSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
