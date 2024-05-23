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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import {
  IotSvgBehavior,
  IotSvgBehaviorAction,
  IotSvgBehaviorType,
  iotSvgBehaviorTypes,
  iotSvgBehaviorTypeTranslations,
  IotSvgBehaviorValue
} from '@home/components/widget/lib/svg/iot-svg.models';

export const behaviorValid = (behavior: IotSvgBehavior): boolean => {
  if (!behavior.id || !behavior.name || !behavior.type) {
    return false;
  }
  switch (behavior.type) {
    case IotSvgBehaviorType.value:
      const valueBehavior = behavior as IotSvgBehaviorValue;
      if (!valueBehavior.valueType) {
        return false;
      }
      break;
    case IotSvgBehaviorType.action:
      const actionBehavior = behavior as IotSvgBehaviorAction;
      if (!actionBehavior.valueToDataType) {
        return false;
      }
      break;
    case IotSvgBehaviorType.widgetAction:
      break;
  }
  return true;
};

export const behaviorValidator = (control: AbstractControl): ValidationErrors | null => {
  const behavior: IotSvgBehavior = control.value;
  if (!behaviorValid(behavior)) {
    return {
      behavior: true
    };
  }
  return null;
};

@Component({
  selector: 'tb-scada-symbol-metadata-behavior-row',
  templateUrl: './scada-symbol-behavior-row.component.html',
  styleUrls: ['./scada-symbol-behavior-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolBehaviorRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolBehaviorRowComponent implements ControlValueAccessor, OnInit {

  @ViewChild('idInput')
  idInput: ElementRef<HTMLInputElement>;

  iotSvgBehaviorTypes = iotSvgBehaviorTypes;
  iotSvgBehaviorTypeTranslations = iotSvgBehaviorTypeTranslations;

  @Input()
  disabled: boolean;

  @Output()
  behaviorRemoved = new EventEmitter();

  behaviorRowFormGroup: UntypedFormGroup;

  modelValue: IotSvgBehavior;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.behaviorRowFormGroup = this.fb.group({
      id: [null, []],
      name: [null, []],
      type: [null, []]
    });
    this.behaviorRowFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.behaviorRowFormGroup.disable({emitEvent: false});
    } else {
      this.behaviorRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: IotSvgBehavior): void {
    this.modelValue = value;
    this.behaviorRowFormGroup.patchValue(
      {
        id: value?.id,
        name: value?.name,
        type: value?.type
      }, {emitEvent: false}
    );
    this.cd.markForCheck();
  }

  editBehavior() {

  }

  focus() {
    this.idInput.nativeElement.scrollIntoView();
    this.idInput.nativeElement.focus();
  }

  private updateModel() {
    const value: IotSvgBehavior = this.behaviorRowFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }

}
