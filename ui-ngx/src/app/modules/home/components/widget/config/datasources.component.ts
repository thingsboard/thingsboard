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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import {
  Datasource,
  DatasourceType,
  JsonSettingsSchema,
  WidgetConfigMode,
  widgetType
} from '@shared/models/widget.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { deepClone } from '@core/utils';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { DataKeysCallbacks } from '@home/components/widget/data-keys.component.models';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-datasources',
  templateUrl: './datasources.component.html',
  styleUrls: ['./datasources.component.scss', 'widget-config.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatasourcesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DatasourcesComponent),
      multi: true,
    }
  ]
})
export class DatasourcesComponent implements ControlValueAccessor, OnInit, Validator, OnChanges {

  datasourceType = DatasourceType;

  public get basicMode(): boolean {
    return !this.widgetConfigComponent.widgetEditMode && this.configMode === WidgetConfigMode.basic;
  }

  public get maxDatasources(): number {
    return this.widgetConfigComponent.modelValue?.typeParameters?.maxDatasources;
  }

  public get singleDatasource(): boolean {
    return this.maxDatasources === 1;
  }

  public get showAddDatasource(): boolean {
   return this.widgetConfigComponent.modelValue?.typeParameters &&
    (this.maxDatasources === -1 || this.datasourcesFormArray.length < this.maxDatasources);
  }

  public get dragDisabled(): boolean {
    return this.disabled || this.singleDatasource || this.datasourcesFormArray.length < 2;
  }

  @Input()
  disabled: boolean;

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
  configMode: WidgetConfigMode;

  datasourcesFormGroup: UntypedFormGroup;

  timeseriesKeyError = false;

  datasourceError: string[] = [];

  datasourcesMode: DatasourceType;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private utils: UtilsService,
              public translate: TranslateService,
              private widgetConfigComponent: WidgetConfigComponent) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.validate(null)) {
      setTimeout(() => {
        this.datasourcesUpdated(this.datasourcesFormGroup.get('datasources').value);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.datasourcesFormGroup.disable({emitEvent: false});
    } else {
      this.datasourcesFormGroup.enable({emitEvent: false});
    }
  }

  ngOnInit() {
    this.datasourcesFormGroup = this.fb.group({
      datasources: this.fb.array([])
    });
    this.datasourcesFormGroup.valueChanges.subscribe(
      () => {
        this.datasourcesUpdated(this.datasourcesFormGroup.get('datasources').value);
      }
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'configMode') {
          this.configModeChanged();
        }
      }
    }
  }

  writeValue(datasources?: Datasource[]): void {
    this.datasourcesFormArray.clear({emitEvent: false});
    this.datasourcesMode = this.detectDatasourcesMode(datasources);
    let changed = false;
    if (datasources) {
      datasources.forEach((datasource) => {
        if (this.basicMode && datasource.type !== this.datasourcesMode) {
          datasource.type = this.datasourcesMode;
          changed = true;
        }
        this.datasourcesFormArray.push(this.fb.control(datasource, []), {emitEvent: false});
      });
    }
    if (this.singleDatasource && !this.datasourcesFormArray.length) {
      this.addDatasource(false);
    }
    if (changed) {
      setTimeout(() => {
        this.datasourcesUpdated(this.datasourcesFormGroup.get('datasources').value);
      }, 0);
    }
  }

  validate(c: UntypedFormControl) {
    this.timeseriesKeyError = false;
    this.datasourceError = [];
    if (!this.datasourcesFormGroup.valid) {
      return {
        datasources: {
          valid: false,
        }
      };
    }
    const datasources: Datasource[] = this.datasourcesFormGroup.get('datasources').value;
    if (!this.datasourcesOptional && (!datasources || !datasources.length)) {
      return {
        datasources: {
          valid: false
        }
      };
    }
    if (this.hasAdditionalLatestDataKeys) {
      let valid = datasources.filter(datasource => datasource?.dataKeys?.length).length > 0;
      if (!valid) {
        this.timeseriesKeyError = true;
        return {
          timeseriesDataKeys: {
            valid: false
          }
        };
      } else {
        const emptyDatasources = datasources.filter(datasource => !datasource?.dataKeys?.length &&
          !datasource?.latestDataKeys?.length);
        valid = emptyDatasources.length === 0;
        if (!valid) {
          for (const emptyDatasource of emptyDatasources) {
            const i = datasources.indexOf(emptyDatasource);
            this.datasourceError[i] = 'At least one data key should be specified';
          }
          return {
            dataKeys: {
              valid: false
            }
          };
        }
      }
    }
    return null;
  }

  datasourcesModeChange(datasourcesMode: DatasourceType) {
    this.datasourcesMode = datasourcesMode;
    if (this.basicMode) {
      for (const datasourceControl of this.datasourcesControls) {
        const datasource: Datasource = datasourceControl.value;
        if (datasource.type !== datasourcesMode) {
          datasource.type = datasourcesMode;
          datasourceControl.patchValue(datasource);
        }
      }
    }
  }

  private configModeChanged() {
    if (this.basicMode) {
      let datasourcesMode = this.detectDatasourcesMode(this.datasourcesFormGroup.get('datasources').value);
      this.datasourcesModeChange(datasourcesMode);
    }
  }

  private detectDatasourcesMode(datasources?: Datasource[]) {
    let datasourcesMode = DatasourceType.device;
    if (datasources && datasources.length) {
      datasourcesMode = datasources[0].type;
    }
    if (datasourcesMode !== DatasourceType.device && datasourcesMode !== DatasourceType.entity) {
      datasourcesMode = DatasourceType.device;
    }
    return datasourcesMode;
  }

  get datasourcesFormArray(): UntypedFormArray {
    return this.datasourcesFormGroup.get('datasources') as UntypedFormArray;
  }

  get datasourcesControls(): FormControl[] {
    return this.datasourcesFormArray.controls as FormControl[];
  }

  public trackByDatasource(index: number, datasourceControl: AbstractControl): any {
    return datasourceControl;
  }

  private datasourcesUpdated(datasources: Datasource[]) {
    this.propagateChange(datasources);
  }

  public onDatasourceDrop(event: CdkDragDrop<string[]>) {
    const datasourceForm = this.datasourcesFormArray.at(event.previousIndex);
    this.datasourcesFormArray.removeAt(event.previousIndex);
    this.datasourcesFormArray.insert(event.currentIndex, datasourceForm);
  }

  public removeDatasource(index: number) {
    this.datasourcesFormArray.removeAt(index);
  }

  public addDatasource(emitEvent = true) {
    let newDatasource: Datasource;
    if (this.widgetConfigComponent.functionsOnly) {
      newDatasource = deepClone(this.utils.getDefaultDatasource(this.dataKeySettingsSchema.schema));
      newDatasource.dataKeys = [this.dataKeysCallbacks.generateDataKey('Sin', DataKeyType.function, this.dataKeySettingsSchema)];
    } else {
      const type = this.basicMode ? this.datasourcesMode : DatasourceType.entity;
      newDatasource = { type,
        dataKeys: []
      };
    }
    if (this.hasAdditionalLatestDataKeys) {
      newDatasource.latestDataKeys = [];
    }
    this.datasourcesFormArray.push(this.fb.control(newDatasource, []), {emitEvent});
  }

  private get dataKeySettingsSchema(): JsonSettingsSchema {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsSchema;
  }

  private get dataKeysCallbacks(): DataKeysCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  private get hasAdditionalLatestDataKeys(): boolean {
    return this.widgetConfigComponent.widgetType === widgetType.timeseries &&
      this.widgetConfigComponent.modelValue?.typeParameters?.hasAdditionalLatestDataKeys;
  }

  private get datasourcesOptional(): boolean {
    return this.widgetConfigComponent.modelValue?.typeParameters?.datasourcesOptional;
  }
}
