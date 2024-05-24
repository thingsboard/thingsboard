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
  Renderer2,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {
  IotSvgBehavior,
  IotSvgBehaviorType,
  iotSvgBehaviorTypes,
  iotSvgBehaviorTypeTranslations
} from '@home/components/widget/lib/svg/iot-svg.models';
import { deepClone, isDefinedAndNotNull, isUndefinedOrNull } from '@core/utils';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  ScadaSymbolBehaviorPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-panel.component';
import { ValueToDataType } from '@shared/models/action-widget-settings.models';

export const behaviorValid = (behavior: IotSvgBehavior): boolean => {
  if (!behavior.id || !behavior.name || !behavior.type) {
    return false;
  }
  switch (behavior.type) {
    case IotSvgBehaviorType.value:
      if (!behavior.valueType || isUndefinedOrNull(behavior.defaultValue)) {
        return false;
      }
      break;
    case IotSvgBehaviorType.action:
      if (!behavior.valueToDataType) {
        return false;
      }
      if (behavior.valueToDataType === ValueToDataType.CONSTANT
        && isUndefinedOrNull(behavior.constantValue)) {
        return false;
      }
      if (behavior.valueToDataType === ValueToDataType.FUNCTION
        && isUndefinedOrNull(behavior.valueToDataFunction)) {
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

  @ViewChild('editButton')
  editButton: MatButton;

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
              private cd: ChangeDetectorRef,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    this.behaviorRowFormGroup = this.fb.group({
      id: [null, [Validators.required]],
      name: [null, [Validators.required]],
      type: [null, [Validators.required]]
    });
    this.behaviorRowFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.behaviorRowFormGroup.get('type').valueChanges.subscribe((newType: IotSvgBehaviorType) => {
      this.onTypeChanged(newType);
    });
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

  editBehavior($event: Event, matButton: MatButton, add = false, editCanceled = () => {}) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        isAdd: add,
        behavior: deepClone(this.modelValue)
      };
      const scadaSymbolBehaviorPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ScadaSymbolBehaviorPanelComponent, ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      scadaSymbolBehaviorPanelPopover.tbComponentRef.instance.popover = scadaSymbolBehaviorPanelPopover;
      scadaSymbolBehaviorPanelPopover.tbComponentRef.instance.behaviorSettingsApplied.subscribe((behavior) => {
        scadaSymbolBehaviorPanelPopover.hide();
        this.behaviorRowFormGroup.patchValue(
          {
            id: behavior.id,
            name: behavior.name,
            type: behavior.type
          }, {emitEvent: false}
        );
        this.modelValue = behavior;
        this.propagateChange(this.modelValue);
      });
      scadaSymbolBehaviorPanelPopover.tbDestroy.subscribe(() => {
        if (!behaviorValid(this.modelValue)) {
          editCanceled();
        }
      });
    }
  }

  focus() {
    this.idInput.nativeElement.scrollIntoView();
    this.idInput.nativeElement.focus();
  }

  onAdd(onCanceled: () => void) {
    this.idInput.nativeElement.scrollIntoView();
    this.editBehavior(null, this.editButton, true, onCanceled);
  }

  private onTypeChanged(newType: IotSvgBehaviorType) {
    const prevType = this.modelValue.type;
    this.modelValue = {...this.modelValue, ...{type: newType}};
    if (!behaviorValid(this.modelValue)) {
      this.editBehavior(null, this.editButton, false, () => {
        this.behaviorRowFormGroup.patchValue(
          {
            type: prevType
          }, {emitEvent: true}
        );
      });
    }
  }

  private updateModel() {
    const value: IotSvgBehavior = this.behaviorRowFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }

}
