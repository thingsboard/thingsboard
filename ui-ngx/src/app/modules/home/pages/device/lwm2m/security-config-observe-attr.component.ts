///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import {PageComponent} from "@shared/components/page.component";
import {Component, forwardRef, Inject, Input, OnInit, Output} from "@angular/core";
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {
  DeviceCredentialsDialogLwm2mData,
  ObjectLwM2M,
  Instance, ResourceLwM2M
} from "@home/pages/device/lwm2m/security-config.models";


@Component({
  selector: 'tb-security-config-observe-attr-lwm2m',
  templateUrl: './security-config-observe-attr.component.html',
  styleUrls: ['./security-config-observe-attr.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigObserveAttrComponent),
      multi: true
    }
  ]
})
export class SecurityConfigObserveAttrComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input() observeFormGroup: FormGroup;
  observeValue: ObjectLwM2M[];
  instance: FormArray;
  clientLwM2M: FormArray;

  constructor(protected store: Store<AppState>,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigObserveAttrComponent, object>,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {}

  registerOnChange(fn: any): void {
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: ObjectLwM2M[]): void {
    this.observeValue = value;
    if (this.observeValue && this.observeValue.length > 0) {
      this.buildClientObjectsLwM2M(this.observeValue)
    }
  }

  private buildClientObjectsLwM2M(objectsLwM2M: ObjectLwM2M []): void {
    this.observeFormGroup.addControl('clientLwM2M',
        this.createObjectsLwM2M(objectsLwM2M)
    );
  }

  createObjectsLwM2M(objectsLwM2MJson: ObjectLwM2M []): FormArray {
    return this.fb.array(objectsLwM2MJson.map(objectLwM2M => {
      return this.fb.group({
        id: objectLwM2M.id,
        name: objectLwM2M.name,
        instance: this.createInstanceLwM2M(objectLwM2M.instance)
      })
    }))
  }

  createInstanceLwM2M(instanceLwM2MJson: Instance []): FormArray {
    return this.fb.array(instanceLwM2MJson.map(instanceLwM2M => {
      return this.fb.group({
        id: instanceLwM2M.id,
        isObserv: instanceLwM2M.isObserv,
        resource: this.createResourceLwM2M(instanceLwM2M.resource)
      })
    }))
  }

  createResourceLwM2M(resourcesLwM2MJson: ResourceLwM2M []): FormArray {
    return this.fb.array(resourcesLwM2MJson.map(resourceLwM2M => {
      return this.fb.group({
        id: resourceLwM2M.id,
        isObserv: resourceLwM2M.isObserv,
        isAttr: resourceLwM2M.isAttr,
        name: resourceLwM2M.name
      })
    }))
  }

  clientLwM2MFormArray(formGroup: FormGroup): FormArray {
    return formGroup.get('clientLwM2M') as FormArray;
  }

  instanceLwm2mFormArray(objectLwM2M: AbstractControl): FormArray {
    return objectLwM2M.get('instance') as FormArray;
  }

  resourceLwm2mFormArray(instance: AbstractControl): FormArray {
    return instance.get('resource') as FormArray;
  }
}
