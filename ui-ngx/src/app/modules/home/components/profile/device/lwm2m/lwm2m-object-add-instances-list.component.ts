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
  Output,
  EventEmitter
} from "@angular/core";
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR, Validators
} from "@angular/forms";
import {coerceBooleanProperty} from "@angular/cdk/coercion";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../../../core/core.state";
import {MatChipList} from '@angular/material/chips';
import {MatAutocomplete} from "@angular/material/autocomplete";
import {Observable} from "rxjs";
import {filter, map, mergeMap, share, tap} from 'rxjs/operators';
import {ObjectLwM2M} from "./profile-config.models";
import {TranslateService} from "@ngx-translate/core";
import {DeviceProfileService} from "../../../../../../core/http/device-profile.service";
import {PageLink} from "../../../../../../shared/models/page/page-link";
import {Direction} from "../../../../../../shared/models/page/sort-order";
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-profile-lwm2m-object-add-instances-list',
  templateUrl: './lwm2m-object-add-instances-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObjectAddInstancesListComponent),
      multi: true
    }]
})
export class Lwm2mObjectAddInstancesListComponent implements ControlValueAccessor, OnInit, Validators {

  lwm2mObjectListFormGroup: FormGroup;
  private requiredValue: boolean;
  private instancesIdsList: Set<number> | null;
  maxInstanceIdValue = 22 as number;
  minInstanceIdValue = 1 as number;
  filteredObjectsList:  Array<number>;
  private disabled = false as boolean;
  searchText: number;
  private dirty = false as boolean;

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

  @Output()
  addList = new EventEmitter<any>();

  @Output()
  removeList = new EventEmitter<any>();

  @ViewChild('objectInput') objectInput: ElementRef<HTMLInputElement>;
  @ViewChild('objectAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipList;

  private propagateChange = (v: any) => {
  };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder) {
    this.lwm2mObjectListFormGroup = this.fb.group({
      instancesIdsList: [this.instancesIdsList, ],
      objectLwm2m: [new Set(),[Validators.min(this.minInstanceIdValue),
                               Validators.max(this.maxInstanceIdValue)]]
    });
    this.lwm2mObjectListFormGroup.get('objectLwm2m').valueChanges.subscribe(value => {
      console.warn(value);
      // if (value){
      //   if (value > this.maxInstanceIdValue) {
      //     this.validateMax = false;
      //   }
      //   else {
      //     this.validateMax = true;
      //   }
      // }
      // else {
      //   this.validateMax = false;
      // }
    })
  }

  updateValidators() {
    this.lwm2mObjectListFormGroup.get('objectLwm2m').setValidators(this.required ? [Validators.required,
      Validators.min(this.maxInstanceIdValue),
      Validators.max(this.maxInstanceIdValue)] : []);
    this.lwm2mObjectListFormGroup.get('objectLwm2m').updateValueAndValidity();
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

  writeValue(value: any): void {
    this.searchText = 1;
    if (value) {
      // this.modelValue = [...value];
      this.instancesIdsList = new Set();
      value.forEach(item => this.instancesIdsList.add(item));
      // this.modelValue = deepClone(this.instancesIdsList )
      this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
    } else {
      this.instancesIdsList = null;
      this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
      // this.modelValue = null;
    }
    this.dirty = true;

  }

  // reset() {
  //   this.instancesIdsList = [];
  //   this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
  //   this.modelValue = null;
  //   if (this.objectInput) {
  //     this.objectInput.nativeElement.value = '0';
  //   }
  //   this.lwm2mObjectListFormGroup.get('objectLwm2m').patchValue('', {emitEvent: false});
  //   this.propagateChange(this.modelValue);
  //   this.dirty = true;
  // }

  add(value: number): void {
    if (value && value >= this.minInstanceIdValue && value <= this.maxInstanceIdValue) {
        debugger
        // if (!this.modelValue || this.modelValue.indexOf(value) === -1) {
        //   if (!this.modelValue) {
        //     this.modelValue = null;
        //   }
      this.instancesIdsList.add(value);
     // console.warn( this.modelValue, "add");
          // this.modelValue = deepClone(this.instancesIdsList);
          // this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
          this.lwm2mObjectListFormGroup.patchValue({
              instancesIdsList: this.instancesIdsList
            },
            {emitEvent: false});
          // this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
          this.propagateChange(this.instancesIdsList);
          this.addList.next(this.instancesIdsList);
          // this.clear();
        }
        // this.propagateChange(this.modelValue);
   //  }
   // else {
      this.clear();
    // }
  }

  remove(object: number) {
    // let index = this.instancesIdsList.indexOf(object);
    // if (index >= 0) {
    //   this.instancesIdsList.splice(index, 1);
    this.instancesIdsList.delete(object);
    this.lwm2mObjectListFormGroup.get('instancesIdsList').setValue(this.instancesIdsList);
    // this.modelValue.delete(object)
    // let index = this.modelValue.indexOf(object);
    // this.modelValue.splice(index, 1);
    this.removeList.next(object);
    // if (!this.modelValue.size) {
    //   this.modelValue = new Set<number>();
    // }
    this.propagateChange(this.instancesIdsList);
    this.clear();
    // }
  }

  displayFn(object?: number): number | undefined {
    return object ? object : undefined;
  }

  // fetchListObjects(searchText?: number): Array<number> {
  // // fetchListObjects(searchText?: number): void {
  //   debugger
  //   this.searchText = searchText;
  //   let data = [] as Array<number>;
  //   data.push(1);
  //   data.push(12);
  //   return data;
  //   // const pageLink = new PageLink(10, 0, searchText, {
  //   //   property: 'name',
  //   //   direction: Direction.ASC
  //   // });
  //   // return this.deviceProfileService.getLwm2mObjectsPage(pageLink, {ignoreLoading: true}).pipe(
  //   //   map(pageData => {
  //   //     let data = pageData.data;
  //   //     return data;
  //   //   })
  //   // );
  // }

  onFocus() {
    // if (this.dirty) {
    //
    //   this.lwm2mObjectListFormGroup.get('objectLwm2m').updateValueAndValidity({onlySelf: true, emitEvent: true});
    //   this.dirty = false;
    // }
    // this.updateValidators();
  }

  clear() {
    // this.objectInput.nativeElement.value = value;
    this.lwm2mObjectListFormGroup.get('objectLwm2m').patchValue(null, {emitEvent: true});
    this.objectInput.nativeElement.value = "";
    setTimeout(() => {
      this.objectInput.nativeElement.blur();
      this.objectInput.nativeElement.focus();
    }, 0);
  }

}
