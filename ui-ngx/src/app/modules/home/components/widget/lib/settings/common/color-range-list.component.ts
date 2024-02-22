///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormGroup,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { ColorRange } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-color-range-list',
  templateUrl: './color-range-list.component.html',
  styleUrls: ['color-settings-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorRangeListComponent),
      multi: true
    }
  ]
})
export class ColorRangeListComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent;

  @Input()
  panelTitle: string;

  modelValue: any;

  colorRangeListFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.colorRangeListFormGroup = this.fb.group({
        rangeList: this.fb.array([])
    });

    this.colorRangeListFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: any): void {
    if (value && value?.length) {
      value.forEach((r) => this.rangeListFormArray.push(this.colorRangeControl(r), {emitEvent: false}));
    }
  }

  private colorRangeControl(range: ColorRange): UntypedFormGroup {
    return this.fb.group({
      from: [range?.from, []],
      to: [range?.to, []],
      color: [range?.color, []]
    });
  }

  get rangeListFormArray(): UntypedFormArray {
    return this.colorRangeListFormGroup.get('rangeList') as UntypedFormArray;
  }

  get rangeListFormGroups(): FormGroup[] {
    return this.rangeListFormArray.controls as FormGroup[];
  }

  trackByRange(index: number, rangeControl: AbstractControl): any {
    return rangeControl;
  }

  removeRange(index: number) {
    this.rangeListFormArray.removeAt(index);
    this.colorRangeListFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  addRange() {
    const newRange: ColorRange = {
      color: 'rgba(0,0,0,0.87)'
    };
    this.rangeListFormArray.push(this.colorRangeControl(newRange));
    this.colorRangeListFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  updateModel() {
    this.propagateChange(this.colorRangeListFormGroup.get('rangeList').value);
  }

}
