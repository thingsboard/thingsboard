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

import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormGroup,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { AdvancedGradient, ColorGradientSettings, ValueSourceType } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { IAliasController } from '@core/api/widget-api.models';
import { DomSanitizer } from '@angular/platform-browser';
import { coerceBoolean } from '@shared/decorators/coercion';
import { isDefinedAndNotNull } from '@core/utils';
import { DataKeysCallbacks } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { Datasource } from '@shared/models/widget.models';

@Component({
    selector: 'tb-gradient',
    templateUrl: './gradient.component.html',
    styleUrls: ['color-settings-panel.component.scss', 'gradient.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => GradientComponent),
            multi: true
        }
    ],
    standalone: false
})
export class GradientComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @ViewChild('gradient') gradient: ElementRef;

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
  minValue: number;

  @Input()
  maxValue: number;

  @Input()
  @coerceBoolean()
  advancedMode = true;

  modelValue: any;

  gradientFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.gradientFormGroup = this.fb.group({
      advancedMode: [false],
      gradient: this.fb.group({
        start: ['rgba(0, 255, 0, 1)'],
        gradientList: this.fb.array([]),
        end: ['rgba(255, 0, 0, 1)']
      }),
      gradientAdvanced: this.fb.group({
        start: this.fb.group({
          source: [{type: ValueSourceType.constant}],
          color: ['rgba(0, 255, 0, 1)']
        }),
        gradientList: this.fb.array([]),
        end: this.fb.group({
          source: [{type: ValueSourceType.constant}],
          color: ['rgba(255, 0, 0, 1)']
        })
      }),
      minValue: {value: 0, disabled: isFinite(this.minValue)},
      maxValue: {value: 100, disabled: isFinite(this.maxValue)}
    });

    this.gradientFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
    this.gradientFormGroup.get('advancedMode').valueChanges.pipe(
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

  writeValue(value: ColorGradientSettings): void {
    if (isDefinedAndNotNull(value)) {
      this.gradientFormGroup.get('advancedMode').patchValue(value.advancedMode, {emitEvent: false});
      this.gradientFormGroup.get('minValue').patchValue(isFinite(this.minValue) ? this.minValue : value.minValue, {emitEvent: false});
      this.gradientFormGroup.get('maxValue').patchValue(isFinite(this.maxValue) ? this.maxValue : value.maxValue, {emitEvent: false});
      if (value?.gradient?.length) {
        this.gradientFormGroup.get('gradient').get('start').patchValue(value.gradient[0], {emitEvent: false});
        this.gradientFormGroup.get('gradient').get('end').patchValue(value.gradient[value.gradient.length - 1], {emitEvent: false});
        value.gradient.slice(1, -1).forEach(r => this.gradientListFormArray.push(this.colorGradientControl(r), {emitEvent: false}));
      }
      if (value?.gradientAdvanced?.length) {
        this.gradientFormGroup.get('gradientAdvanced').get('start').patchValue(value.gradientAdvanced[0], {emitEvent: false});
        this.gradientFormGroup.get('gradientAdvanced').get('end').patchValue(
          value.gradientAdvanced[value.gradientAdvanced.length - 1], {emitEvent: false}
        );
        value.gradientAdvanced.slice(1, -1).forEach(
          r => this.advancedGradientListFormArray.push(this.advancedGradientControl(r), {emitEvent: false})
        );
      }
    }
  }

  get generatePointers() {
    if (this.gradientFormGroup.get('advancedMode').value) {
      const shift = 100 / (this.advancedGradientListFormArray.value.length + 1);
      return `<div class="pointer start"></div>` +
        this.advancedGradientListFormArray.value.map((v, i) => this.pointer(shift * (i + 1), i+1, null, true)).join('') +
        `<div class="pointer end"></div>`;
    } else {
      const min = this.gradientFormGroup.get('minValue').value || 0;
      const max = this.gradientFormGroup.get('maxValue').value || 100;
      const point = (+max - +min) / (this.gradientListFormArray.value.length + 1);
      const shift = 100 / (this.gradientListFormArray.value.length + 1);
      return `<div class="pointer start"><div class="pointer-value"><span class="pointer-value-text">${min}</span></div></div>` +
        this.gradientListFormArray.value.map((v, i) => this.pointer(shift * (i + 1), i+1, point)).join('') +
        `<div class="pointer end"><div class="pointer-value"><span class="pointer-value-text">${max}</span></div></div>`;
    }
  }

  pointer(shift: number, index?: number, value?: number, advanced = false) {
    if (advanced) {
      return `<div class="pointer" style="left: ${shift}%"></div>`;
    } else {
      return `<div class="pointer" style="left: ${shift}%">` +
        `<div class="pointer-value"><span class="pointer-value-text">` +
        `${Math.floor(+this.gradientFormGroup.get('minValue').value + (value * index))}</span></div></div>`;
    }
  }

  get linearGradient() {
    const gradient = this.gradientFormGroup.get('advancedMode').value ?
      [this.gradientFormGroup.value.gradientAdvanced.start.color,
        ...this.gradientFormGroup.value.gradientAdvanced.gradientList.map(item => item.color),
        this.gradientFormGroup.value.gradientAdvanced.end.color].join(', ') :
      [this.gradientFormGroup.value.gradient.start,
      ...this.gradientFormGroup.value.gradient.gradientList.map(item => item.color),
      this.gradientFormGroup.value.gradient.end].join(', ');
    return this.sanitizer.bypassSecurityTrustStyle(`background-image: linear-gradient(90deg, ${gradient})`);
  }

  private colorGradientControl(gradient: string): UntypedFormGroup {
    return this.fb.group({
      color: [gradient, []]
    });
  }

  get gradientListFormArray(): UntypedFormArray {
    return this.gradientFormGroup.get('gradient.gradientList') as UntypedFormArray;
  }
  get gradientListFormGroups(): FormGroup[] {
    return this.gradientListFormArray.controls as FormGroup[];
  }

  private advancedGradientControl(gradient: AdvancedGradient): UntypedFormGroup {
    return this.fb.group({
      source: [gradient.source, []],
      color: [gradient.color, []]
    });
  }

  get advancedGradientListFormArray(): UntypedFormArray {
    return this.gradientFormGroup.get('gradientAdvanced.gradientList') as UntypedFormArray;
  }
  get advancedGradientListFormGroups(): FormGroup[] {
    return this.advancedGradientListFormArray.controls as FormGroup[];
  }

  trackByGradient(index: number, gradientControl: AbstractControl): any {
    return gradientControl;
  }

  removeGradient(index: number, advanced = false) {
    if (advanced) {
      this.advancedGradientListFormArray.removeAt(index);
    } else {
      this.gradientListFormArray.removeAt(index);
    }
    this.gradientFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  gradientDrop(event: CdkDragDrop<string[]>, advanced = false) {
    const gradientColorsArray = advanced ? this.advancedGradientListFormArray : this.gradientListFormArray;
    const gradientColor = gradientColorsArray.at(event.previousIndex);
    gradientColorsArray.removeAt(event.previousIndex);
    gradientColorsArray.insert(event.currentIndex, gradientColor);
  }

  addGradient(advanced = false) {
    if (advanced) {
      this.advancedGradientListFormArray.push(
        this.advancedGradientControl({source: {type: ValueSourceType.constant}, color: 'rgba(0,0,0,0.87)'})
      );
    } else {
      this.gradientListFormArray.push(this.colorGradientControl('rgba(0,0,0,0.87)'));
    }
    this.gradientFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  updateModel() {
    const gradient = this.gradientFormGroup.getRawValue();
    this.propagateChange(
      {
        advancedMode: gradient.advancedMode,
        gradient: [gradient.gradient.start, ...gradient.gradient.gradientList.map(item => item.color), gradient.gradient.end],
        gradientAdvanced: [gradient.gradientAdvanced.start, ...gradient.gradientAdvanced.gradientList, gradient.gradientAdvanced.end],
        minValue: gradient.minValue,
        maxValue: gradient.maxValue
      }
    );
  }
}
