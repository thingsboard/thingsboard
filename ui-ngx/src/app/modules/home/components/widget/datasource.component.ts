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
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import {
  Datasource,
  DatasourceType,
  datasourceTypeTranslationMap,
  JsonSettingsSchema,
  Widget, WidgetConfigMode,
  widgetType
} from '@shared/models/widget.models';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityAliasSelectCallbacks } from '@home/components/alias/entity-alias-select.component.models';
import { FilterSelectCallbacks } from '@home/components/filter/filter-select.component.models';
import { DataKeysCallbacks } from '@home/components/widget/data-keys.component.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-datasource',
  templateUrl: './datasource.component.html',
  styleUrls: ['./datasource.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatasourceComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DatasourceComponent),
      multi: true,
    }
  ]
})
export class DatasourceComponent implements ControlValueAccessor, OnInit, Validator {

  public get basicMode(): boolean {
    return !this.widgetConfigComponent.widgetEditMode && this.widgetConfigComponent.widgetConfigMode === WidgetConfigMode.basic;
  }

  public get widgetType(): widgetType {
    return this.widgetConfigComponent.widgetType;
  }

  public get aliasController(): IAliasController {
    return this.widgetConfigComponent.aliasController;
  }

  public get entityAliasSelectCallbacks(): EntityAliasSelectCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  public get filterSelectCallbacks(): FilterSelectCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  public get dataKeysCallbacks(): DataKeysCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  public get hasAdditionalLatestDataKeys(): boolean {
    return this.widgetConfigComponent.widgetType === widgetType.timeseries &&
      this.widgetConfigComponent.modelValue?.typeParameters?.hasAdditionalLatestDataKeys;
  }

  public get dataKeysOptional(): boolean {
    return this.widgetConfigComponent.modelValue?.typeParameters?.dataKeysOptional;
  }

  public get maxDataKeys(): number {
    return this.widgetConfigComponent.modelValue?.typeParameters?.maxDataKeys;
  }

  public get dataKeySettingsSchema(): JsonSettingsSchema {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsSchema;
  }

  public get dataKeySettingsDirective(): string {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsDirective;
  }

  public get latestDataKeySettingsSchema(): JsonSettingsSchema {
    return this.widgetConfigComponent.modelValue?.latestDataKeySettingsSchema;
  }

  public get latestDataKeySettingsDirective(): string {
    return this.widgetConfigComponent.modelValue?.latestDataKeySettingsDirective;
  }

  public get dashboard(): Dashboard {
    return this.widgetConfigComponent.dashboard;
  }

  public get widget(): Widget {
    return this.widgetConfigComponent.widget;
  }

  @Input()
  disabled: boolean;

  widgetTypes = widgetType;

  entityType = EntityType;

  datasourceType = DatasourceType;
  datasourceTypes: Array<DatasourceType> = [];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  datasourceFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private widgetConfigComponent: WidgetConfigComponent) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (!this.datasourceFormGroup.valid) {
      setTimeout(() => {
        this.datasourceUpdated(this.datasourceFormGroup.value);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.datasourceFormGroup.disable({emitEvent: false});
    } else {
      this.datasourceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  ngOnInit() {
    if (this.widgetConfigComponent.functionsOnly) {
      this.datasourceTypes = [DatasourceType.function];
    } else {
      this.datasourceTypes = [DatasourceType.function, DatasourceType.device, DatasourceType.entity];
      if (this.widgetConfigComponent.widgetType === widgetType.latest) {
        this.datasourceTypes.push(DatasourceType.entityCount);
        this.datasourceTypes.push(DatasourceType.alarmCount);
      }
    }

    this.datasourceFormGroup = this.fb.group(
      {
        type: [null, [Validators.required]],
        name: [null, []],
        deviceId: [null, []],
        entityAliasId: [null, []],
        filterId: [null, []],
        dataKeys: [null, []],
        alarmFilterConfig: [null, []]
      }
    );
    if (this.hasAdditionalLatestDataKeys) {
      this.datasourceFormGroup.addControl('latestDataKeys', this.fb.control(null));
    }
    this.datasourceFormGroup.get('type').valueChanges.subscribe(() => {
      this.updateValidators();
    });
    this.datasourceFormGroup.valueChanges.subscribe(
      () => {
        this.datasourceUpdated(this.datasourceFormGroup.value);
      }
    );
  }

  writeValue(datasource?: Datasource): void {
    this.datasourceFormGroup.patchValue({
      type: datasource?.type,
      name: datasource?.name,
      deviceId: datasource?.deviceId,
      entityAliasId: datasource?.entityAliasId,
      filterId: datasource?.filterId,
      dataKeys: datasource?.dataKeys,
      alarmFilterConfig: datasource?.alarmFilterConfig ?
        datasource?.alarmFilterConfig : { statusList: [AlarmSearchStatus.ACTIVE] }
    }, {emitEvent: false});
    if (this.hasAdditionalLatestDataKeys) {
      this.datasourceFormGroup.patchValue({
        latestDataKeys: datasource?.latestDataKeys
      }, {emitEvent: false});
    }
    this.updateValidators();
  }

  validate(c: UntypedFormControl) {
    return (this.datasourceFormGroup.valid) ? null : {
      datasource: {
        valid: false,
      },
    };
  }

  public isDataKeysOptional(type?: DatasourceType): boolean {
    if (this.hasAdditionalLatestDataKeys) {
      return true;
    } else {
      return this.dataKeysOptional
        && type !== DatasourceType.entityCount && type !== DatasourceType.alarmCount;
    }
  }

  private datasourceUpdated(datasource: Datasource) {
    this.propagateChange(datasource);
  }

  private updateValidators() {
    const type: DatasourceType = this.datasourceFormGroup.get('type').value;
    this.datasourceFormGroup.get('deviceId').setValidators(
      type === DatasourceType.device ? [Validators.required] : []
    );
    this.datasourceFormGroup.get('entityAliasId').setValidators(
      (type === DatasourceType.entity || type === DatasourceType.entityCount) ? [Validators.required] : []
    );
    const newDataKeysRequired = !this.isDataKeysOptional(type);
    this.datasourceFormGroup.get('dataKeys').setValidators(newDataKeysRequired ? [Validators.required] : []);
    this.datasourceFormGroup.get('deviceId').updateValueAndValidity({emitEvent: false});
    this.datasourceFormGroup.get('entityAliasId').updateValueAndValidity({emitEvent: false});
    this.datasourceFormGroup.get('dataKeys').updateValueAndValidity({emitEvent: false});
  }

}
