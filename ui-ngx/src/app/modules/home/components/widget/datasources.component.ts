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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor, FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR, UntypedFormArray,
  UntypedFormBuilder, UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { Datasource, DatasourceType, JsonSettingsSchema, widgetType } from '@shared/models/widget.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { deepClone } from '@core/utils';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { DataKeysCallbacks } from '@home/components/widget/data-keys.component.models';

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
export class DatasourcesComponent implements ControlValueAccessor, OnInit, Validator {

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

  datasourcesFormGroup: UntypedFormGroup;

  timeseriesKeyError = false;

  datasourceError: string[] = [];

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private utils: UtilsService,
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

  writeValue(datasources?: Datasource[]): void {
    this.datasourcesFormArray.clear({emitEvent: false});
    if (datasources) {
      datasources.forEach((datasource) => {
        this.datasourcesFormArray.push(this.fb.control(datasource, []), {emitEvent: false});
      });
    }
    if (this.singleDatasource && !this.datasourcesFormArray.length) {
      this.addDatasource(false);
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
      newDatasource = { type: DatasourceType.entity,
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
