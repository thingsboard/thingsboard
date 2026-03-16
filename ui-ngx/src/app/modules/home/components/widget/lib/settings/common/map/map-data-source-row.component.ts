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
import { MapDataSourceSettings } from '@shared/models/widget/maps/map.models';
import { DatasourceType, datasourceTypeTranslationMap, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
    selector: 'tb-map-data-source-row',
    templateUrl: './map-data-source-row.component.html',
    styleUrls: ['./map-data-source-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MapDataSourceRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MapDataSourceRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;

  EntityType = EntityType;

  widgetType = widgetType;

  datasourceTypes: Array<DatasourceType> = [DatasourceType.device, DatasourceType.entity];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  @Output()
  dataSourceRemoved = new EventEmitter();

  dataSourceFormGroup: UntypedFormGroup;

  modelValue: MapDataSourceSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.dataSourceFormGroup = this.fb.group({
      dsType: [null, [Validators.required]],
      dsDeviceId: [null, [Validators.required]],
      dsEntityAliasId: [null, [Validators.required]],
      dsFilterId: [null, []]
    });
    this.dataSourceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.dataSourceFormGroup.get('dsType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateValidators()
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

  writeValue(value: MapDataSourceSettings): void {
    this.modelValue = value;
    this.dataSourceFormGroup.patchValue(
      {
        dsType: value?.dsType,
        dsDeviceId: value?.dsDeviceId,
        dsEntityAliasId: value?.dsEntityAliasId,
        dsFilterId: value?.dsFilterId
      }, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }


  private updateValidators() {
    const dsType: DatasourceType = this.dataSourceFormGroup.get('dsType').value;
    if (dsType === DatasourceType.device) {
      this.dataSourceFormGroup.get('dsDeviceId').enable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else {
      this.dataSourceFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataSourceFormGroup.value};
    this.propagateChange(this.modelValue);
  }
}
