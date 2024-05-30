///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  ChangeDetectorRef,
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  TimeSeriesChartThreshold,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  TimeSeriesChartThresholdsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-thresholds-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKey, Datasource, DatasourceType, WidgetConfig } from '@shared/models/widget.models';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { deepClone } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  ValueSourceTypes,
  ValueSourceType,
  ValueSourceTypeTranslation
} from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-time-series-chart-threshold-row',
  templateUrl: './time-series-chart-threshold-row.component.html',
  styleUrls: ['./time-series-chart-threshold-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartThresholdRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartThresholdRowComponent implements ControlValueAccessor, OnInit, OnChanges {

  DataKeyType = DataKeyType;

  DatasourceType = DatasourceType;

  TimeSeriesChartThresholdType = ValueSourceType;

  timeSeriesThresholdTypes = ValueSourceTypes;

  timeSeriesThresholdTypeTranslations = ValueSourceTypeTranslation;

  get aliasController(): IAliasController {
    return this.thresholdsPanel.aliasController;
  }

  get dataKeyCallbacks(): DataKeysCallbacks {
    return this.thresholdsPanel.dataKeyCallbacks;
  }

  get datasource(): Datasource {
    return this.thresholdsPanel.datasource;
  }

  get widgetConfig(): WidgetConfig {
    return this.thresholdsPanel.widgetConfig;
  }

  @Input()
  disabled: boolean;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Input()
  @coerceBoolean()
  hideYAxis = false;

  @Output()
  thresholdRemoved = new EventEmitter();

  thresholdFormGroup: UntypedFormGroup;

  modelValue: TimeSeriesChartThreshold;

  latestKeyFormControl: UntypedFormControl;

  entityKeyFormControl: UntypedFormControl;

  thresholdSettingsFormControl: UntypedFormControl;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private thresholdsPanel: TimeSeriesChartThresholdsPanelComponent,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.thresholdFormGroup = this.fb.group({
      type: [null, []],
      value: [null, [Validators.required]],
      entityAlias: [null, [Validators.required]],
      yAxisId: [null, [Validators.required]],
      lineColor: [null, []],
      units: [null, []],
      decimals: [null, []]
    });
    this.latestKeyFormControl = this.fb.control(null, [Validators.required]);
    this.entityKeyFormControl = this.fb.control(null, [Validators.required]);
    this.thresholdSettingsFormControl = this.fb.control(null);
    this.thresholdFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.latestKeyFormControl.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.entityKeyFormControl.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.thresholdSettingsFormControl.valueChanges.subscribe((thresholdSettings: Partial<TimeSeriesChartThreshold>) => {
      this.modelValue = {...this.modelValue, ...thresholdSettings};
      this.thresholdFormGroup.patchValue(
        {
          yAxisId: this.modelValue.yAxisId,
          units: this.modelValue.units,
          decimals: this.modelValue.decimals,
          lineColor: this.modelValue.lineColor
        },
        {emitEvent: false});
      this.propagateChange(this.modelValue);
    });
    this.thresholdFormGroup.get('type').valueChanges.subscribe(() => {
      this.updateValidators();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['yAxisIds'].includes(propName)) {
          if (this.modelValue?.yAxisId &&
            !this.yAxisIds.includes(this.modelValue.yAxisId)) {
            this.thresholdFormGroup.patchValue({yAxisId: 'default'}, {emitEvent: true});
          }
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.thresholdFormGroup.disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
      this.thresholdSettingsFormControl.disable({emitEvent: false});
    } else {
      this.thresholdFormGroup.enable({emitEvent: false});
      this.thresholdSettingsFormControl.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartThreshold): void {
    this.modelValue = value;
    this.thresholdFormGroup.patchValue(
      {
        type: value.type,
        value: value.value,
        entityAlias: value.entityAlias,
        yAxisId: value.yAxisId,
        lineColor: value.lineColor,
        units: value.units,
        decimals: value.decimals,
      }, {emitEvent: false}
    );
    if (value.type === ValueSourceType.latestKey) {
      this.latestKeyFormControl.patchValue({
        type: value.latestKeyType,
        name: value.latestKey
      }, {emitEvent: false});
    } else if (value.type === ValueSourceType.entity) {
      this.entityKeyFormControl.patchValue({
        type: value.entityKeyType,
        name: value.entityKey
      }, {emitEvent: false});
    }
    this.thresholdSettingsFormControl.patchValue(deepClone(this.modelValue),
      {emitEvent: false});
    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateValidators() {
    const type: ValueSourceType = this.thresholdFormGroup.get('type').value;
    if (type === ValueSourceType.constant) {
      this.thresholdFormGroup.get('value').enable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceType.latestKey) {
      this.thresholdFormGroup.get('value').disable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.enable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceType.entity) {
      this.thresholdFormGroup.get('value').disable({emitEvent: false});
      this.thresholdFormGroup.get('entityAlias').enable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.enable({emitEvent: false});
    }
  }

  private updateModel() {
    const value = this.thresholdFormGroup.value;
    this.modelValue.type = value.type;
    this.modelValue.value = value.value;
    this.modelValue.entityAlias = value.entityAlias;
    this.modelValue.yAxisId = value.yAxisId;
    this.modelValue.lineColor = value.lineColor;
    this.modelValue.units = value.units;
    this.modelValue.decimals = value.decimals;
    if (value.type === ValueSourceType.latestKey) {
      const latestKey: DataKey = this.latestKeyFormControl.value;
      this.modelValue.latestKey = latestKey?.name;
      this.modelValue.latestKeyType = (latestKey?.type as any);
    } else if (value.type === ValueSourceType.entity) {
      const entityKey: DataKey = this.entityKeyFormControl.value;
      this.modelValue.entityKey = entityKey?.name;
      this.modelValue.entityKeyType = (entityKey?.type as any);
    }
    this.thresholdSettingsFormControl.patchValue(deepClone(this.modelValue),
      {emitEvent: false});
    this.propagateChange(this.modelValue);
  }
}
