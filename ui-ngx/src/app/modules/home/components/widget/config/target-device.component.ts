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
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { TargetDevice, TargetDeviceType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityAliasSelectCallbacks } from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-target-device',
  templateUrl: './target-device.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TargetDeviceComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TargetDeviceComponent),
      multi: true,
    }
  ]
})
export class TargetDeviceComponent implements ControlValueAccessor, OnInit, Validator {

  public get aliasController(): IAliasController {
    return this.widgetConfigComponent.aliasController;
  }

  public get entityAliasSelectCallbacks(): EntityAliasSelectCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  public get targetDeviceOptional(): boolean {
    return this.widgetConfigComponent.modelValue?.typeParameters?.targetDeviceOptional;
  }

  targetDeviceType = TargetDeviceType;

  entityType = EntityType;

  @Input()
  disabled: boolean;

  widgetEditMode = this.utils.widgetEditMode;

  targetDeviceFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private utils: UtilsService,
              public translate: TranslateService,
              private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (!this.targetDeviceFormGroup.valid) {
      setTimeout(() => {
        this.targetDeviceUpdated(this.targetDeviceFormGroup.value);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.targetDeviceFormGroup.disable({emitEvent: false});
    } else {
      this.targetDeviceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  ngOnInit() {
    this.targetDeviceFormGroup = this.fb.group({
      type: [null, (!this.widgetEditMode && !this.targetDeviceOptional) ? [Validators.required] : []],
      deviceId: [null, (!this.widgetEditMode && !this.targetDeviceOptional) ? [Validators.required] : []],
      entityAliasId: [null, (!this.widgetEditMode && !this.targetDeviceOptional) ? [Validators.required] : []]
    });
    this.targetDeviceFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.targetDeviceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.targetDeviceUpdated(this.targetDeviceFormGroup.value);
      }
    );
  }

  writeValue(targetDevice?: TargetDevice): void {
    this.targetDeviceFormGroup.patchValue({
      type: targetDevice?.type || TargetDeviceType.device,
      deviceId: targetDevice?.deviceId,
      entityAliasId: targetDevice?.entityAliasId
    }, {emitEvent: false});
    this.updateValidators();
    if (targetDevice?.type === TargetDeviceType.entity && targetDevice?.entityAliasId) {
      const entityAliases = this.aliasController.getEntityAliases();
      if (!entityAliases[targetDevice.entityAliasId]) {
        this.targetDeviceFormGroup.get('entityAliasId').patchValue(null, {emitEvent: false});
        setTimeout(() => {
          this.targetDeviceUpdated(this.targetDeviceFormGroup.value);
        }, 0);
      }
    }
  }

  validate(c: UntypedFormControl) {
    return (this.targetDeviceFormGroup.valid) ? null : {
      targetDevice: {
        valid: false,
      },
    };
  }

  private targetDeviceUpdated(targetDevice: TargetDevice) {
    this.propagateChange(targetDevice);
  }

  private updateValidators() {
    const type: TargetDeviceType = this.targetDeviceFormGroup.get('type').value;
    if (!this.widgetEditMode && type) {
      if (type === TargetDeviceType.device) {
        this.targetDeviceFormGroup.get('deviceId').enable({emitEvent: false});
        this.targetDeviceFormGroup.get('entityAliasId').disable({emitEvent: false});
      } else {
        this.targetDeviceFormGroup.get('deviceId').disable({emitEvent: false});
        this.targetDeviceFormGroup.get('entityAliasId').enable({emitEvent: false});
      }
    } else {
      this.targetDeviceFormGroup.get('deviceId').disable({emitEvent: false});
      this.targetDeviceFormGroup.get('entityAliasId').disable({emitEvent: false});
    }
  }

}
