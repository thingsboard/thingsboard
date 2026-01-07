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
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
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
import { MatDialog } from '@angular/material/dialog';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { dataKeyRowValidator, dataKeyValid } from '@home/components/widget/config/basic/common/data-key-row.component';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { DataKeysCallbacks, DataKeySettingsFunction } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TimeSeriesChartYAxisId } from '@home/components/widget/lib/chart/time-series-chart.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-data-keys-panel',
  templateUrl: './data-keys-panel.component.html',
  styleUrls: ['./data-keys-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataKeysPanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DataKeysPanelComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class DataKeysPanelComponent implements ControlValueAccessor, OnInit, OnChanges, Validator {

  @Input()
  disabled: boolean;

  @Input()
  panelTitle: string;

  @Input()
  addKeyTitle: string;

  @Input()
  keySettingsTitle: string;

  @Input()
  removeKeyTitle: string;

  @Input()
  noKeysText: string;

  @Input()
  requiredKeysText: string;

  @Input()
  datasourceType: DatasourceType;

  @Input()
  entityAliasId: string;

  @Input()
  deviceId: string;

  @Input()
  @coerceBoolean()
  hidePanel = false;

  @Input()
  @coerceBoolean()
  hideDataKeyColor = false;

  @Input()
  @coerceBoolean()
  hideUnits = false;

  @Input()
  @coerceBoolean()
  hideDecimals = false;

  @Input()
  @coerceBoolean()
  hideDataKeyUnits = false;

  @Input()
  @coerceBoolean()
  hideDataKeyDecimals = false;

  @Input()
  @coerceBoolean()
  hideSourceSelection = false;

  @Input()
  @coerceBoolean()
  timeSeriesChart = false;

  @Input()
  @coerceBoolean()
  showTimeSeriesType = false;

  @Input()
  yAxisIds: TimeSeriesChartYAxisId[];

  dataKeyType: DataKeyType;

  keysListFormGroup: UntypedFormGroup;

  errorText = '';

  get widgetType(): widgetType {
    return this.widgetConfigComponent.widgetType;
  }

  get callbacks(): DataKeysCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  get hasAdditionalLatestDataKeys(): boolean {
    return !this.hideSourceSelection && this.widgetConfigComponent.widgetType === widgetType.timeseries &&
      this.widgetConfigComponent.modelValue?.typeParameters?.hasAdditionalLatestDataKeys;
  }

  get dataKeySettingsForm(): FormProperty[] {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsForm;
  }

  get dataKeySettingsFunction(): DataKeySettingsFunction {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsFunction;
  }

  get dragEnabled(): boolean {
    return this.keysFormArray().controls.length > 1;
  }

  get noKeys(): boolean {
    let keys: DataKey[] = this.keysListFormGroup.get('keys').value;
    if (this.hasAdditionalLatestDataKeys) {
      keys = keys.filter(k => !(k as any)?.latest);
    }
    return keys.length === 0;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private utils: UtilsService,
              private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.keysListFormGroup = this.fb.group({
      keys: [this.fb.array([]), []]
    });
    this.keysListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let keys: DataKey[] = this.keysListFormGroup.get('keys').value;
        if (keys) {
          keys = keys.filter(k => dataKeyValid(k));
        }
        this.propagateChange(keys);
      }
    );
    this.updateParams();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['datasourceType'].includes(propName)) {
            this.updateParams();
        }
      }
    }
  }

  private updateParams() {
    if (this.datasourceType === DatasourceType.function) {
      this.dataKeyType = DataKeyType.function;
    } else {
      if (this.widgetType !== widgetType.latest && this.widgetType !== widgetType.alarm) {
        this.dataKeyType = DataKeyType.timeseries;
      } else {
        this.dataKeyType = null;
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
      this.keysListFormGroup.disable({emitEvent: false});
    } else {
      this.keysListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DataKey[] | undefined): void {
    this.keysListFormGroup.setControl('keys', this.prepareKeysFormArray(value), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    this.errorText = '';
    let valid = this.keysListFormGroup.valid;
    if (this.noKeys && this.requiredKeysText) {
      valid = false;
      this.errorText = this.requiredKeysText;
    }
    return valid ? null : {
      dataKeyRows: {
        valid: false,
      },
    };
  }

  keyDrop(event: CdkDragDrop<string[]>) {
    const keysArray = this.keysListFormGroup.get('keys') as UntypedFormArray;
    const key = keysArray.at(event.previousIndex);
    keysArray.removeAt(event.previousIndex);
    keysArray.insert(event.currentIndex, key);
  }

  keysFormArray(): UntypedFormArray {
    return this.keysListFormGroup.get('keys') as UntypedFormArray;
  }

  trackByKey(index: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  removeKey(index: number) {
    (this.keysListFormGroup.get('keys') as UntypedFormArray).removeAt(index);
  }

  addKey() {
    const dataKey = this.callbacks.generateDataKey('', null, this.dataKeySettingsForm,
      false, this.dataKeySettingsFunction);
    dataKey.label = '';
    dataKey.decimals = 0;
    if (this.hasAdditionalLatestDataKeys) {
      (dataKey as any).latest = false;
    }
    const keysArray = this.keysListFormGroup.get('keys') as UntypedFormArray;
    const keyControl = this.fb.control(dataKey, [dataKeyRowValidator]);
    keysArray.push(keyControl);
  }

  private prepareKeysFormArray(keys: DataKey[] | undefined): UntypedFormArray {
    const keysControls: Array<AbstractControl> = [];
    if (keys) {
      keys.forEach((key) => {
        keysControls.push(this.fb.control(key, [dataKeyRowValidator]));
      });
    }
    return this.fb.array(keysControls);
  }

}
