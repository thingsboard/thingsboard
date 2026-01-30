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
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdditionalMapDataSourceSettings, updateDataKeyToNewDsType } from '@shared/models/widget/maps/map.models';
import { DataKey, DatasourceType, datasourceTypeTranslationMap, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { genNextLabelForDataKeys } from '@core/utils';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
    selector: 'tb-additional-map-data-source-row',
    templateUrl: './additional-map-data-source-row.component.html',
    styleUrls: ['./additional-map-data-source-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AdditionalMapDataSourceRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class AdditionalMapDataSourceRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;
  DataKeyType = DataKeyType;

  EntityType = EntityType;

  widgetType = widgetType;

  datasourceTypes: Array<DatasourceType> = [];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  @Output()
  dataSourceRemoved = new EventEmitter();

  dataSourceFormGroup: UntypedFormGroup;

  generateAdditionalDataKey = this.generateDataKey.bind(this);

  modelValue: AdditionalMapDataSourceSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    if (this.context.functionsOnly) {
      this.datasourceTypes = [DatasourceType.function];
    } else {
      this.datasourceTypes = [DatasourceType.function, DatasourceType.device, DatasourceType.entity];
    }
    this.dataSourceFormGroup = this.fb.group({
      dsType: [null, [Validators.required]],
      dsLabel: [null, []],
      dsDeviceId: [null, [Validators.required]],
      dsEntityAliasId: [null, [Validators.required]],
      dataKeys: [null, [Validators.required]]
    });
    this.dataSourceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.dataSourceFormGroup.get('dsType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (newDsType: DatasourceType) => this.onDsTypeChanged(newDsType)
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataSourceFormGroup.disable({emitEvent: false});
    } else {
      this.dataSourceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: AdditionalMapDataSourceSettings): void {
    this.modelValue = value;
    this.dataSourceFormGroup.patchValue(
      {
        dsType: value?.dsType,
        dsLabel: value?.dsLabel,
        dsDeviceId: value?.dsDeviceId,
        dsEntityAliasId: value?.dsEntityAliasId,
        dataKeys: value?.dataKeys
      }, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  private generateDataKey(key: DataKey): DataKey {
    const dataKey = this.context.callbacks.generateDataKey(key.name, key.type, null, false, null);
    const dataKeys: DataKey[] = this.dataSourceFormGroup.get('dataKeys').value || [];
    dataKey.label = genNextLabelForDataKeys(dataKey.label, dataKeys);
    return dataKey;
  }

  private onDsTypeChanged(newDsType: DatasourceType) {
    let updateModel = false;
    const dataKeys: DataKey[] = this.dataSourceFormGroup.get('dataKeys').value;
    if (dataKeys?.length) {
      for (const key of dataKeys) {
        updateModel = updateDataKeyToNewDsType(key, newDsType) || updateModel;
      }
      if (updateModel) {
        this.dataSourceFormGroup.get('dataKeys').patchValue(dataKeys, {emitEvent: false});
      }
    }
    this.updateValidators();
    if (updateModel) {
      this.updateModel();
    }
  }

  private updateValidators() {
    const dsType: DatasourceType = this.dataSourceFormGroup.get('dsType').value;
    if (dsType === DatasourceType.function) {
      this.dataSourceFormGroup.get('dsLabel').enable({emitEvent: false});
      this.dataSourceFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else if (dsType === DatasourceType.device) {
      this.dataSourceFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsDeviceId').enable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else {
      this.dataSourceFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataSourceFormGroup.value};
    this.propagateChange(this.modelValue);
  }
}
