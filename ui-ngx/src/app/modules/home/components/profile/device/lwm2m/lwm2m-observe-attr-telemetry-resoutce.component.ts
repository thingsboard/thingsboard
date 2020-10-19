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

import {Component, EventEmitter, forwardRef, Inject, Input, OnInit, Output} from "@angular/core";
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR
} from "@angular/forms";
import {PageComponent} from "@shared/components/page.component";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {
  DeviceCredentialsDialogLwm2mData,
  ResourceLwM2M
} from "@home/pages/device/lwm2m/security-config.models";
import {MatCheckboxChange} from "@angular/material/checkbox";

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-resource',
  templateUrl: './lwm2m-observe-attr-telemetry-resource.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourceComponent),
      multi: true
    }
  ]
})
export class Lwm2mObserveAttrTelemetryResourceComponent implements ControlValueAccessor, OnInit {

  @Input() i: number;
  @Input() y: number;
  @Input() resourceFormGroup: FormGroup;
  @Input() resourceValue: ResourceLwM2M[];
  @Output() valueCheckBoxChange = new EventEmitter<{}>()


  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: ResourceLwM2M[]): void {
  }

  resourceLwm2mFormArray(instance: AbstractControl): FormArray {
    return instance.get('resources') as FormArray;
  }

  changeInstanceResourcesCheckBox(value: boolean, objInd: number, insInd: number, restInd: number, nameFrom?: string): void {
    this.valueCheckBoxChange.emit({
      value:value,
      objInd: objInd,
      instInd:insInd,
      resInd:restInd,
      nameFrom: nameFrom
    });
  }
}
