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

import {
  Component,
  forwardRef,
  Input,
  OnInit,
  ViewChild,
  ElementRef,
} from "@angular/core";
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup, NG_VALIDATORS,
  NG_VALUE_ACCESSOR, Validators
} from "@angular/forms";
import { coerceBooleanProperty } from "@angular/cdk/coercion";
import { Store } from "@ngrx/store";
import { AppState } from "../../../../../../core/core.state";
import { MatChipList } from '@angular/material/chips';
import {
  INSTANCES_ID_VALUE_MAX,
  INSTANCES_ID_VALUE_MIN
} from "./profile-config.models";
import { TranslateService } from "@ngx-translate/core";
import { DeviceProfileService } from "../../../../../../core/http/device-profile.service";

@Component({
  selector: 'tb-profile-lwm2m-object-add-instances-list',
  templateUrl: './lwm2m-object-add-instances-list.component.html',
  styleUrls: ['./lwm2m-object-add-instances-list.component.scss'],
  providers: [{
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObjectAddInstancesListComponent),
      multi: true
    }]
})
export class Lwm2mObjectAddInstancesListComponent implements ControlValueAccessor, OnInit, Validators {

  lwm2mObjectListFormGroup: FormGroup;
  private requiredValue: boolean;
  private instancesIdsList: Set<number> | null;
  filteredObjectsList: Array<number>;
  private disabled = false as boolean;
  private dirty = false as boolean;
  instanceIdValueMin = INSTANCES_ID_VALUE_MIN as number
  instanceIdValueMax = INSTANCES_ID_VALUE_MAX as number

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @ViewChild('instanceIdInput') instanceIdInput: ElementRef<HTMLInputElement>;
  @ViewChild('chipList', {static: true}) chipList: MatChipList;

  private propagateChange = (v: any) => {
  };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder) {
    this.lwm2mObjectListFormGroup = this.fb.group({
      instancesIdsList: [null],
      instanceIdInput: [null]
    });
  }

  updateValidators() {
    this.lwm2mObjectListFormGroup.get('instanceIdInput').setValidators([
      Validators.min(this.instanceIdValueMin),
      Validators.max(this.instanceIdValueMax)]);
    this.lwm2mObjectListFormGroup.get('instanceIdInput').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.lwm2mObjectListFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mObjectListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Set<number>): void {
    this.instancesIdsList = new Set<number>();
    if (value && value.size) {
      value.forEach(item => this.instancesIdsList.add(item));
      this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
    }
    this.updateValidators();
    this.dirty = false;
  }

  add(value: number): void {
    if (!isNaN(value) && this.lwm2mObjectListFormGroup.get('instanceIdInput').valid) {
      this.instancesIdsList.add(value);
      this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
      this.propagateChange(this.instancesIdsList);
      this.dirty = true
    }
    this.clear();
  }

  remove(object: number) {
    this.instancesIdsList.delete(object);
    this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
    this.propagateChange(this.instancesIdsList);
    this.dirty = true
    this.clear();
  }

  displayFn(object?: number): number | undefined {
    return object ? object : undefined;
  }

  clear() {
    this.lwm2mObjectListFormGroup.get('instanceIdInput').patchValue(null, {emitEvent: true});
    this.instanceIdInput.nativeElement.value = "";
    setTimeout(() => {
      this.instanceIdInput.nativeElement.blur();
      this.instanceIdInput.nativeElement.focus();
    }, 0);
  }

  onkeydown(e: KeyboardEvent) {
    if (e.keyCode == 189 || e.keyCode == 187 || e.keyCode == 109 || e.keyCode == 107) {
      return false;
    } else if (e.keyCode == 8) {
      if (this.lwm2mObjectListFormGroup.get('instanceIdInput').value == null) {
        this.clear();
      }
      this.instanceIdInput.nativeElement.focus();
    }
  }

  onFocus() {
    if (this.dirty) {
      this.lwm2mObjectListFormGroup.get('instanceIdInput').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }
}
