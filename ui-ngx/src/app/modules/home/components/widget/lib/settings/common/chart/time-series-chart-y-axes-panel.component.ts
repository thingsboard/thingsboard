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

import {
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import {
  defaultTimeSeriesChartYAxisSettings,
  getNextTimeSeriesYAxisId,
  TimeSeriesChartYAxes,
  TimeSeriesChartYAxisId,
  TimeSeriesChartYAxisSettings,
  timeSeriesChartYAxisValid,
  timeSeriesChartYAxisValidator
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { mergeDeep } from '@core/utils';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { IAliasController } from '@app/core/public-api';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { DataKey, DataKeyType, Datasource, ValueSourceConfig, ValueSourceType } from '@app/shared/public-api';

@Component({
  selector: 'tb-time-series-chart-y-axes-panel',
  templateUrl: './time-series-chart-y-axes-panel.component.html',
  styleUrls: ['./time-series-chart-y-axes-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartYAxesPanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TimeSeriesChartYAxesPanelComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartYAxesPanelComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  advanced = false;

  @Input()
  @coerceBoolean()
  supportsUnitConversion = false;

  @Output()
  axisRemoved = new EventEmitter<TimeSeriesChartYAxisId>();

  yAxesFormGroup: UntypedFormGroup;

  get dragEnabled(): boolean {
    return this.axesFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.yAxesFormGroup = this.fb.group({
      axes: [this.fb.array([]), []]
    });
    this.yAxesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let axes: TimeSeriesChartYAxisSettings[] = this.yAxesFormGroup.get('axes').value;
        for (let i = 0; i < axes.length; i++) {
          axes[i].order = i;
        }
        if (axes) {
          axes = axes.filter(axis => timeSeriesChartYAxisValid(axis));
        }
        const yAxes: TimeSeriesChartYAxes = {};
        for (const axis of axes) {
          yAxes[axis.id] = axis;
        }
        this.updateLatestDataKeys(Object.values(yAxes));
        this.propagateChange(yAxes);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.yAxesFormGroup.disable({emitEvent: false});
    } else {
      this.yAxesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TimeSeriesChartYAxes | undefined): void {
    const yAxes: TimeSeriesChartYAxes = this.checkLatestDataKeys(value || {});
    if (!yAxes.default) {
      yAxes.default = mergeDeep({} as TimeSeriesChartYAxisSettings, defaultTimeSeriesChartYAxisSettings,
        {id: 'default', order: 0} as TimeSeriesChartYAxisSettings);
    }
    const yAxisSettingsList = Object.values(yAxes);
    yAxisSettingsList.sort((a1, a2) => a1.order - a2.order);
    this.yAxesFormGroup.setControl('axes', this.prepareAxesFormArray(yAxisSettingsList), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.yAxesFormGroup.valid;
    return valid ? null : {
      yAxes: {
        valid: false,
      },
    };
  }

  axisDrop(event: CdkDragDrop<string[]>) {
    const axesArray = this.yAxesFormGroup.get('axes') as UntypedFormArray;
    const axis = axesArray.at(event.previousIndex);
    axesArray.removeAt(event.previousIndex);
    axesArray.insert(event.currentIndex, axis);
  }

  axesFormArray(): UntypedFormArray {
    return this.yAxesFormGroup.get('axes') as UntypedFormArray;
  }

  trackByAxis(index: number, axisControl: AbstractControl): any {
    return axisControl;
  }

  removeAxis(index: number) {
    const axis =
      (this.yAxesFormGroup.get('axes') as UntypedFormArray).at(index).value as TimeSeriesChartYAxisSettings;
    (this.yAxesFormGroup.get('axes') as UntypedFormArray).removeAt(index);
    this.axisRemoved.emit(axis.id);
  }

  addAxis() {
    const axis = mergeDeep<TimeSeriesChartYAxisSettings>({} as TimeSeriesChartYAxisSettings,
      defaultTimeSeriesChartYAxisSettings);
    const axes: TimeSeriesChartYAxisSettings[] = this.yAxesFormGroup.get('axes').value;
    axis.id = getNextTimeSeriesYAxisId(axes);
    axis.order = axes.length;
    axis.min = this.normalizeAxisLimit(axis.min);
    axis.max = this.normalizeAxisLimit(axis.max);
    const axesArray = this.yAxesFormGroup.get('axes') as UntypedFormArray;
    const axisControl = this.fb.control(axis, [timeSeriesChartYAxisValidator]);
    axesArray.push(axisControl);
  }

  private prepareAxesFormArray(axes: TimeSeriesChartYAxisSettings[]): UntypedFormArray {
    const axesControls: Array<AbstractControl> = [];
    axes.forEach((axis) => {
      axesControls.push(this.fb.control(axis, [timeSeriesChartYAxisValidator]));
    });
    return this.fb.array(axesControls);
  }

  private checkLatestDataKeys(yAxes: TimeSeriesChartYAxes): TimeSeriesChartYAxes {
    const latestKeys = this.datasource?.latestDataKeys || [];
    const result: TimeSeriesChartYAxes = {};

    for (const [id, axis] of Object.entries(yAxes)) {
      axis.min = this.normalizeAxisLimit(axis.min);
      axis.max = this.normalizeAxisLimit(axis.max);
      const minCfg = axis.min;
      const maxCfg = axis.max;

      const minValid = !!minCfg && (
        minCfg.type !== ValueSourceType.latestKey ||
        latestKeys.some(k => this.isYAxisKey(k, minCfg))
      );

      const maxValid = !!maxCfg && (
        maxCfg.type !== ValueSourceType.latestKey ||
        latestKeys.some(k => this.isYAxisKey(k, maxCfg))
      );

      if (minValid && maxValid) {
        result[id] = axis;
      }
    }

    return result;
  }

  private updateLatestDataKeys(yAxes: TimeSeriesChartYAxisSettings[]) {
    if (this.datasource) {
      let latestKeys = this.datasource.latestDataKeys;
      if (!latestKeys) {
        latestKeys = [];
        this.datasource.latestDataKeys = latestKeys;
      }
      const existingYAxisKeys = latestKeys.filter(k => k.settings?.__yAxisMinKey === true || k.settings?.__yAxisMaxKey === true);
      const foundYAxisKeys: DataKey[] = [];

      for(const yAxis of yAxes) {
        const min = yAxis.min as ValueSourceConfig;
        const max = yAxis.max as ValueSourceConfig;
        if (min.type === ValueSourceType.latestKey) {
          const found = existingYAxisKeys.find(k => this.isYAxisKey(k, min));
          if (!found) {
            const newKey = this.dataKeyCallbacks.generateDataKey(min.latestKey, min.latestKeyType,
              null, true, null);
            newKey.settings.__yAxisMinKey = true;
            latestKeys.push(newKey);
          } else if (foundYAxisKeys.indexOf(found) === -1) {
            foundYAxisKeys.push(found);
          }
        }
        if (max.type === ValueSourceType.latestKey) {
          const found = existingYAxisKeys.find(k => this.isYAxisKey(k, max));
          if (!found) {
            const newKey = this.dataKeyCallbacks.generateDataKey(max.latestKey, max.latestKeyType,
              null, true, null);
            newKey.settings.__yAxisMaxKey = true;
            latestKeys.push(newKey);
          } else if (foundYAxisKeys.indexOf(found) === -1) {
            foundYAxisKeys.push(found);
          }
        }
      }
      const toRemove = existingYAxisKeys.filter(k => foundYAxisKeys.indexOf(k) === -1);
      for (const key of toRemove) {
        const index = latestKeys.indexOf(key);
        if (index > -1) {
          latestKeys.splice(index, 1);
        }
      }
    }
  }

  private isYAxisKey(d: DataKey, limit: ValueSourceConfig): boolean {
    return (d.type === DataKeyType.function && d.label === limit.latestKey) ||
      (d.type !== DataKeyType.function && d.name === limit.latestKey &&
        d.type === limit.latestKeyType);
  }

  private normalizeAxisLimit(limit: string | number | ValueSourceConfig): ValueSourceConfig {
    if (!limit) {
      return {
        type: ValueSourceType.constant,
        value: null,
        entityAlias: null
      };
    } else if (typeof limit === 'number' || typeof limit === 'string') {
      return {
        type: ValueSourceType.constant,
        value: Number(limit),
        entityAlias: null
      };
    }
    return limit;
  }
}
