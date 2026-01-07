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

import { Component, forwardRef, Input, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormControl,
  FormGroup,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors
} from '@angular/forms';
import {
  AdvancedColorRange,
  ColorRange,
  ColorRangeSettings,
  ValueSourceType
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { deepClone, isDefinedAndNotNull, isUndefined } from '@core/utils';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { IAliasController } from '@core/api/widget-api.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { Datasource } from '@shared/models/widget.models';

export function advancedRangeValidator(control: AbstractControl): ValidationErrors | null {
  const range: AdvancedColorRange = control.value;
  if (!range || !range.color) {
    return {
      advancedRange: true
    };
  }
  return null;
}

@Component({
  selector: 'tb-color-range-list',
  templateUrl: './color-range-list.component.html',
  styleUrls: ['color-settings-panel.component.scss', 'color-range-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorRangeListComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ColorRangeListComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent;

  @Input()
  panelTitle: string;

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  @Input()
  @coerceBoolean()
  simpleRange = false;

  @Input()
  @coerceBoolean()
  advancedMode = false;

  modelValue: any;

  colorRangeListFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.colorRangeListFormGroup = this.fb.group({
      advancedMode: [false],
      range: this.fb.array([]),
      rangeAdvanced: this.fb.array([])
    });

    this.colorRangeListFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
    this.colorRangeListFormGroup.get('advancedMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => setTimeout(() => {this.popover?.updatePosition();}, 0));
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
    if (value) {
      let rangeList: ColorRangeSettings = {};
      if (this.simpleRange || (isUndefined(value?.advancedMode) && value?.length)) {
        rangeList.advancedMode = false;
        rangeList.range = value;
      } else {
        rangeList = deepClone(value);
      }
      this.colorRangeListFormGroup.get('advancedMode').patchValue(rangeList.advancedMode || false, {emitEvent: false});
      if (isDefinedAndNotNull(rangeList?.range)) {
        rangeList.range.forEach((r) => this.rangeListFormArray.push(this.colorRangeControl(r), {emitEvent: false}));
      }
      if (isDefinedAndNotNull(rangeList?.rangeAdvanced)) {
        rangeList.rangeAdvanced.forEach((r) => this.advancedRangeFormArray.push(this.fb.control(r), {emitEvent: false}));
      }
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
    return this.colorRangeListFormGroup.get('range') as UntypedFormArray;
  }

  get rangeListFormGroups(): FormGroup[] {
    return this.rangeListFormArray.controls as FormGroup[];
  }

  trackByRange(index: number, rangeControl: AbstractControl): any {
    return rangeControl;
  }

  public trackByAdvancedRange(index: number, advancedRangeControl: AbstractControl): any {
    return advancedRangeControl;
  }

  public removeAdvancedRange(index: number) {
    (this.colorRangeListFormGroup.get('rangeAdvanced') as UntypedFormArray).removeAt(index);
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  get advancedRangeFormArray(): UntypedFormArray {
    return this.colorRangeListFormGroup.get('rangeAdvanced') as UntypedFormArray;
  }

  get advancedRangeControls(): FormControl[] {
    return this.advancedRangeFormArray.controls as FormControl[];
  }

  removeRange(index: number) {
    this.rangeListFormArray.removeAt(index);
    this.colorRangeListFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  rangeDrop(event: CdkDragDrop<string[]>, range: string) {
    const rangeColorsArray = this.colorRangeListFormGroup.get(range) as UntypedFormArray;
    const rangeColor = rangeColorsArray.at(event.previousIndex);
    rangeColorsArray.removeAt(event.previousIndex);
    rangeColorsArray.insert(event.currentIndex, rangeColor);
  }

  public addAdvancedRange() {
    const advancedRange: AdvancedColorRange = {
      from: {
        type: ValueSourceType.constant
      },
      to: {
        type: ValueSourceType.constant
      },
      color: null
    };
    const advancedRangeColorsArray = this.colorRangeListFormGroup.get('rangeAdvanced') as UntypedFormArray;
    const advancedRangeColorControl = this.fb.control(advancedRange, [advancedRangeValidator]);
    advancedRangeColorsArray.push(advancedRangeColorControl);
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  addRange() {
    if (this.colorRangeListFormGroup.get('advancedMode').value) {
      this.addAdvancedRange();
    } else {
      const newRange: ColorRange = {
        color: 'rgba(0,0,0,0.87)'
      };
      this.rangeListFormArray.push(this.colorRangeControl(newRange));
      this.colorRangeListFormGroup.markAsDirty();
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    }
  }

  updateModel() {
    if (this.simpleRange) {
      this.propagateChange(this.colorRangeListFormGroup.get('range').value);
    } else {
      this.propagateChange(this.colorRangeListFormGroup.value);
    }
  }

}
