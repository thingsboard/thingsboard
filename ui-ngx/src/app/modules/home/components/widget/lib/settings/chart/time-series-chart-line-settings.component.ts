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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  LineSeriesSettings,
  lineSeriesStepTypes,
  lineSeriesStepTypeTranslations,
  TimeSeriesChartType
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { merge } from 'rxjs';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { DataKeyConfigComponent } from '@home/components/widget/lib/settings/common/key/data-key-config.component';
import {
  chartLabelPositions,
  chartLabelPositionTranslations,
  chartLineTypes,
  chartLineTypeTranslations,
  chartShapes,
  chartShapeTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getSourceTbUnitSymbol, isNotEmptyTbUnits } from '@shared/models/unit.models';

@Component({
  selector: 'tb-time-series-chart-line-settings',
  templateUrl: './time-series-chart-line-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartLineSettingsComponent),
      multi: true
    }
  ]
})
export class TimeSeriesChartLineSettingsComponent implements OnInit, ControlValueAccessor {

  TimeSeriesChartType = TimeSeriesChartType;

  lineSeriesStepTypes = lineSeriesStepTypes;

  lineSeriesStepTypeTranslations = lineSeriesStepTypeTranslations;

  chartLineTypes = chartLineTypes;

  chartLineTypeTranslations = chartLineTypeTranslations;

  chartLabelPositions = chartLabelPositions;

  chartLabelPositionTranslations = chartLabelPositionTranslations;

  chartShapes = chartShapes;

  chartShapeTranslations = chartShapeTranslations;

  pointLabelPreviewFn = this._pointLabelPreviewFn.bind(this);

  @Input()
  disabled: boolean;

  @Input()
  chartType: TimeSeriesChartType;

  private modelValue: LineSeriesSettings;

  private propagateChange = null;

  public lineSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private dataKeyConfigComponent: DataKeyConfigComponent,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.lineSettingsFormGroup = this.fb.group({
      showLine: [null, []],
      step: [null, []],
      stepType: [null, []],
      smooth: [null, []],
      lineType: [null, []],
      lineWidth: [null, [Validators.min(0)]],
      showPoints: [null, []],
      showPointLabel: [null, []],
      pointLabelPosition: [null, []],
      pointLabelFont: [null, []],
      pointLabelColor: [null, []],
      enablePointLabelBackground: [null, []],
      pointLabelBackground: [null, []],
      pointShape: [null, []],
      pointSize: [null, [Validators.min(0)]],
      fillAreaSettings: [null, []]
    });
    this.lineSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.lineSettingsFormGroup.get('showLine').valueChanges,
      this.lineSettingsFormGroup.get('step').valueChanges,
      this.lineSettingsFormGroup.get('showPointLabel').valueChanges,
      this.lineSettingsFormGroup.get('enablePointLabelBackground').valueChanges
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

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.lineSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.lineSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: LineSeriesSettings): void {
    this.modelValue = value;
    this.lineSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const state = this.chartType === TimeSeriesChartType.state;
    const showLine: boolean = this.lineSettingsFormGroup.get('showLine').value;
    const step: boolean = this.lineSettingsFormGroup.get('step').value;
    const showPointLabel: boolean = this.lineSettingsFormGroup.get('showPointLabel').value;
    const enablePointLabelBackground: boolean = this.lineSettingsFormGroup.get('enablePointLabelBackground').value;
    if (state) {
      this.lineSettingsFormGroup.get('showLine').disable({emitEvent: false});
    } else {
      this.lineSettingsFormGroup.get('showLine').enable({emitEvent: false});
    }
    if (showLine) {
      if (state) {
        this.lineSettingsFormGroup.get('step').disable({emitEvent: false});
      } else {
        this.lineSettingsFormGroup.get('step').enable({emitEvent: false});
      }
      if (step) {
        this.lineSettingsFormGroup.get('stepType').enable({emitEvent: false});
        this.lineSettingsFormGroup.get('smooth').disable({emitEvent: false});
      } else {
        this.lineSettingsFormGroup.get('stepType').disable({emitEvent: false});
        this.lineSettingsFormGroup.get('smooth').enable({emitEvent: false});
      }
      this.lineSettingsFormGroup.get('lineType').enable({emitEvent: false});
      this.lineSettingsFormGroup.get('lineWidth').enable({emitEvent: false});
    } else {
      this.lineSettingsFormGroup.get('step').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('stepType').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('smooth').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('lineType').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('lineWidth').disable({emitEvent: false});
    }
    if (showPointLabel) {
      this.lineSettingsFormGroup.get('pointLabelPosition').enable({emitEvent: false});
      this.lineSettingsFormGroup.get('pointLabelFont').enable({emitEvent: false});
      this.lineSettingsFormGroup.get('pointLabelColor').enable({emitEvent: false});
      this.lineSettingsFormGroup.get('enablePointLabelBackground').enable({emitEvent: false});
      if (enablePointLabelBackground) {
        this.lineSettingsFormGroup.get('pointLabelBackground').enable({emitEvent: false});
      } else {
        this.lineSettingsFormGroup.get('pointLabelBackground').disable({emitEvent: false});
      }
    } else {
      this.lineSettingsFormGroup.get('pointLabelPosition').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('pointLabelFont').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('pointLabelColor').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('enablePointLabelBackground').disable({emitEvent: false});
      this.lineSettingsFormGroup.get('pointLabelBackground').disable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.lineSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private _pointLabelPreviewFn(): string {
    const dataKey = this.dataKeyConfigComponent.modelValue;
    const widgetConfig = this.dataKeyConfigComponent.widgetConfig;
    const units = isNotEmptyTbUnits(dataKey.units) ? dataKey.units : widgetConfig.config.units;
    const decimals = isDefinedAndNotNull(dataKey.decimals) ? dataKey.decimals :
      (isDefinedAndNotNull(widgetConfig.config.decimals) ? widgetConfig.config.decimals : 2);
    return formatValue(22, decimals, getSourceTbUnitSymbol(units), false);
  }
}
