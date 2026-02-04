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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
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
  AbstractControl,
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKey, DataKeyConfigMode, DatasourceType, Widget, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DataKeySettingsFunction } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { merge } from 'rxjs';
import {
  DataKeyConfigDialogComponent,
  DataKeyConfigDialogData
} from '@home/components/widget/lib/settings/common/key/data-key-config-dialog.component';
import { deepClone } from '@core/utils';
import { Dashboard } from '@shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  timeSeriesChartSeriesTypeIcons,
  timeSeriesChartSeriesTypes,
  timeSeriesChartSeriesTypeTranslations,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export const dataKeyValid = (key: DataKey): boolean => !!key && !!key.type && !!key.name;

export const dataKeyRowValidator = (control: AbstractControl): ValidationErrors | null => {
  const dataKey: DataKey = control.value;
  if (!dataKeyValid(dataKey)) {
    return {
      dataKey: true
    };
  }
  return null;
};

@Component({
    selector: 'tb-data-key-row',
    templateUrl: './data-key-row.component.html',
    styleUrls: ['./data-key-row.component.scss', '../../../lib/settings/common/key/data-keys.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DataKeyRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DataKeyRowComponent implements ControlValueAccessor, OnInit, OnChanges {

  timeSeriesChartSeriesTypes = timeSeriesChartSeriesTypes;
  timeSeriesChartSeriesTypeTranslations = timeSeriesChartSeriesTypeTranslations;
  timeSeriesChartSeriesTypeIcons = timeSeriesChartSeriesTypeIcons;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  datasourceType: DatasourceType;

  @Input()
  entityAliasId: string;

  @Input()
  deviceId: string;

  @Input()
  @coerceBoolean()
  hasAdditionalLatestDataKeys = false;

  @Input()
  @coerceBoolean()
  hideDataKeyLabel = false;

  @Input()
  @coerceBoolean()
  hideDataKeyColor = false;

  @Input()
  @coerceBoolean()
  hideDataKeyUnits = false;

  @Input()
  @coerceBoolean()
  hideDataKeyDecimals = false;

  @Input()
  @coerceBoolean()
  hideUnits = false;

  @Input()
  @coerceBoolean()
  hideDecimals = false;

  @Input()
  @coerceBoolean()
  timeSeriesChart = false;

  @Input()
  @coerceBoolean()
  showTimeSeriesType = false;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  @Input()
  @coerceBoolean()
  singleRow = true;

  @Input()
  dataKeyType: DataKeyType;

  @Input()
  keySettingsTitle: string;

  @Input()
  removeKeyTitle: string;

  @Output()
  keyRemoved = new EventEmitter();

  keyFormControl: UntypedFormControl;

  keyRowFormGroup: UntypedFormGroup;

  modelValue: DataKey;

  generateDataKey = this._generateDataKey.bind(this);

  get widgetType(): widgetType {
    return this.widgetConfigComponent.widgetType;
  }

  get callbacks(): WidgetConfigCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  get widget(): Widget {
    return this.widgetConfigComponent.widget;
  }

  get dashboard(): Dashboard {
    return this.widgetConfigComponent.dashboard;
  }

  get aliasController(): IAliasController {
    return this.widgetConfigComponent.aliasController;
  }

  get dataKeySettingsForm(): FormProperty[] {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsForm;
  }

  get dataKeySettingsDirective(): string {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsDirective;
  }

  get latestDataKeySettingsForm(): FormProperty[] {
    return this.widgetConfigComponent.modelValue?.latestDataKeySettingsForm;
  }

  get latestDataKeySettingsDirective(): string {
    return this.widgetConfigComponent.modelValue?.latestDataKeySettingsDirective;
  }

  get dataKeySettingsFunction(): DataKeySettingsFunction {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsFunction;
  }

  get isEntityDatasource(): boolean {
    return [DatasourceType.device, DatasourceType.entity].includes(this.datasourceType);
  }

  get displayUnitsOrDigits() {
    return this.modelValue?.type && ![ DataKeyType.alarm, DataKeyType.entityField, DataKeyType.count ].includes(this.modelValue?.type);
  }

  get isLatestDataKeys(): boolean {
    return this.hasAdditionalLatestDataKeys && this.keyRowFormGroup.get('latest').value === true;
  }

  get supportsUnitConversion(): boolean {
    return this.widgetConfigComponent.modelValue?.typeParameters?.supportsUnitConversion ?? false;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.keyFormControl = this.fb.control(null, this.required ? [Validators.required] : []);
    this.keyRowFormGroup = this.fb.group({
      label: [null, []],
      color: [null, []],
      units: [null, []],
      decimals: [null, []],
    });
    if (this.hasAdditionalLatestDataKeys) {
      this.keyRowFormGroup.addControl('latest', this.fb.control(false));
    }
    if (this.timeSeriesChart) {
      this.keyRowFormGroup.addControl('yAxis', this.fb.control(null));
      this.keyRowFormGroup.addControl('timeSeriesType', this.fb.control(null));
    }
    merge(this.keyFormControl.valueChanges, this.keyRowFormGroup.valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (this.timeSeriesChart && ['yAxisIds'].includes(propName)) {
          if (this.modelValue?.settings?.yAxisId &&
            !this.yAxisIds.includes(this.modelValue.settings.yAxisId)) {
            this.keyRowFormGroup.patchValue({yAxis: 'default'}, {emitEvent: true});
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
      this.keyFormControl.disable({emitEvent: false});
      this.keyRowFormGroup.disable({emitEvent: false});
    } else {
      this.keyFormControl.enable({emitEvent: false});
      this.keyRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DataKey): void {
    this.modelValue = (value?.name && value?.type) ? value : null;
    this.keyRowFormGroup.patchValue(
      {
        label: value?.label,
        color: value?.color,
        units: value?.units,
        decimals: value?.decimals
      }, {emitEvent: false}
    );
    if (this.hasAdditionalLatestDataKeys) {
      this.keyRowFormGroup.patchValue({
        latest: (value as any)?.latest
      }, {emitEvent: false});
    }
    if (this.timeSeriesChart) {
      const settings = value?.settings as TimeSeriesChartKeySettings;
      const yAxis = settings?.yAxisId || 'default';
      const timeSeriesType = settings?.type || TimeSeriesChartSeriesType.line;
      this.keyRowFormGroup.patchValue({
        yAxis,
        timeSeriesType
      }, {emitEvent: false});
    }
    this.keyFormControl.patchValue(deepClone(this.modelValue), {emitEvent: false});
    this.cd.markForCheck();
  }

  editKey(advanced = false) {
    this.dialog.open<DataKeyConfigDialogComponent, DataKeyConfigDialogData, DataKey>(DataKeyConfigDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dataKey: deepClone(this.modelValue),
          dataKeyConfigMode: advanced ? DataKeyConfigMode.advanced : DataKeyConfigMode.general,
          dataKeySettingsForm: this.isLatestDataKeys ? this.latestDataKeySettingsForm : this.dataKeySettingsForm,
          dataKeySettingsDirective: this.isLatestDataKeys ? this.latestDataKeySettingsDirective : this.dataKeySettingsDirective,
          dashboard: this.dashboard,
          aliasController: this.aliasController,
          widget: this.widget,
          widgetType: this.widgetType,
          deviceId: this.deviceId,
          entityAliasId: this.entityAliasId,
          showPostProcessing: this.widgetType !== widgetType.alarm,
          callbacks: this.callbacks,
          hideDataKeyLabel: this.hideDataKeyLabel,
          hideDataKeyColor: this.hideDataKeyColor,
          hideDataKeyUnits: this.hideDataKeyUnits || !this.displayUnitsOrDigits,
          hideDataKeyDecimals: this.hideDataKeyDecimals || !this.displayUnitsOrDigits,
          supportsUnitConversion: this.supportsUnitConversion
        }
      }).afterClosed().subscribe((updatedDataKey) => {
      if (updatedDataKey) {
        this.modelValue = updatedDataKey;
        this.keyRowFormGroup.get('label').patchValue(this.modelValue.label, {emitEvent: false});
        this.keyRowFormGroup.get('color').patchValue(this.modelValue.color, {emitEvent: false});
        this.keyRowFormGroup.get('units').patchValue(this.modelValue.units, {emitEvent: false});
        this.keyRowFormGroup.get('decimals').patchValue(this.modelValue.decimals, {emitEvent: false});
        if (this.timeSeriesChart) {
          this.keyRowFormGroup.get('yAxis').patchValue(this.modelValue.settings?.yAxisId, {emitEvent: false});
          this.keyRowFormGroup.get('timeSeriesType').patchValue(this.modelValue.settings?.type, {emitEvent: false});
        }
        this.keyFormControl.patchValue(deepClone(this.modelValue), {emitEvent: false});
        this.updateModel();
        this.cd.markForCheck();
      }
    });
  }

  private _generateDataKey(key: DataKey): DataKey {
    key = this.callbacks.generateDataKey(key.name, key.type, this.dataKeySettingsForm, this.isLatestDataKeys,
      this.dataKeySettingsFunction);
    if (!this.keyRowFormGroup.get('label').value) {
      this.keyRowFormGroup.get('label').patchValue(key.label, {emitEvent: false});
    }
    if (this.timeSeriesChart) {
      this.keyRowFormGroup.get('yAxis').patchValue(key.settings?.yAxisId, {emitEvent: false});
      this.keyRowFormGroup.get('timeSeriesType').patchValue(key.settings?.type, {emitEvent: false});
    }
    return key;
  }

  private updateModel() {
    this.modelValue = this.keyFormControl.value;
    if (this.modelValue !== null) {
      const value: DataKey = this.keyRowFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      if (this.timeSeriesChart) {
        this.modelValue.settings.yAxisId = (this.modelValue as any).yAxis;
        this.modelValue.settings.type = (this.modelValue as any).timeSeriesType;
        delete (this.modelValue as any).yAxis;
        delete (this.modelValue as any).timeSeriesType;
      }
    }
    this.propagateChange(this.modelValue);
  }

}
