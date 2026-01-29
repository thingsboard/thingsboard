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
  ChangeDetectorRef,
  Component,
  DestroyRef,
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
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {
  ScadaSymbolBehavior,
  ScadaSymbolBehaviorType,
  scadaSymbolBehaviorTypes,
  scadaSymbolBehaviorTypeTranslations, updateBehaviorDefaultSettings
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { deepClone, isUndefinedOrNull } from '@core/utils';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  ScadaSymbolBehaviorPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-panel.component';
import { ValueToDataType } from '@shared/models/action-widget-settings.models';
import {
  ScadaSymbolBehaviorsComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behaviors.component';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { isNotEmptyTbFunction } from '@shared/models/js-function.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export const behaviorValid = (behavior: ScadaSymbolBehavior): boolean => {
  if (!behavior.id || !behavior.name || !behavior.type) {
    return false;
  }
  switch (behavior.type) {
    case ScadaSymbolBehaviorType.value:
      if (!behavior.valueType || !behavior.defaultGetValueSettings) {
        return false;
      }
      break;
    case ScadaSymbolBehaviorType.action:
      if (!behavior.defaultSetValueSettings) {
        return false;
      }
      if (behavior.defaultSetValueSettings.valueToData?.type === ValueToDataType.CONSTANT
        && isUndefinedOrNull(behavior.defaultSetValueSettings.valueToData?.constantValue)) {
        return false;
      }
      if (behavior.defaultSetValueSettings.valueToData?.type === ValueToDataType.FUNCTION
        && !isNotEmptyTbFunction(behavior.defaultSetValueSettings.valueToData?.valueToDataFunction)) {
        return false;
      }
      break;
    case ScadaSymbolBehaviorType.widgetAction:
      break;
  }
  return true;
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
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ScadaSymbolBehaviorRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ScadaSymbolBehaviorRowComponent implements ControlValueAccessor, OnInit, Validator {

  @ViewChild('idInput')
  idInput: ElementRef<HTMLInputElement>;

  @ViewChild('editButton')
  editButton: MatButton;

  scadaSymbolBehaviorTypes = scadaSymbolBehaviorTypes;
  scadaSymbolBehaviorTypeTranslations = scadaSymbolBehaviorTypeTranslations;

  @Input()
  disabled: boolean;

  @Input()
  index: number;

  @Input()
  aliasController: IAliasController;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Output()
  behaviorRemoved = new EventEmitter();

  behaviorRowFormGroup: UntypedFormGroup;

  modelValue: ScadaSymbolBehavior;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private behaviorsComponent: ScadaSymbolBehaviorsComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.behaviorRowFormGroup = this.fb.group({
      id: [null, [this.behaviorIdValidator()]],
      name: [null, [Validators.required]],
      type: [null, [Validators.required]]
    });
    this.behaviorRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.behaviorRowFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((newType: ScadaSymbolBehaviorType) => {
      this.onTypeChanged(newType);
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.behaviorRowFormGroup.disable({emitEvent: false});
    } else {
      this.behaviorRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ScadaSymbolBehavior): void {
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
      const scadaSymbolBehaviorPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ScadaSymbolBehaviorPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          isAdd: add,
          disabled: this.disabled,
          aliasController: this.aliasController,
          callbacks: this.callbacks,
          behavior: deepClone(this.modelValue)
        },
        isModal: true
      });
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

  public validate(_c: UntypedFormControl) {
    const idControl = this.behaviorRowFormGroup.get('id');
    if (idControl.hasError('behaviorIdNotUnique')) {
      idControl.updateValueAndValidity({onlySelf: false, emitEvent: false});
    }
    if (idControl.hasError('behaviorIdNotUnique')) {
      this.behaviorRowFormGroup.get('id').markAsTouched();
      return {
        behaviorIdNotUnique: true
      };
    }
    const behavior: ScadaSymbolBehavior = {...this.modelValue, ...this.behaviorRowFormGroup.value};
    if (!behaviorValid(behavior)) {
      return {
        behavior: true
      };
    }
    return null;
  }

  private behaviorIdValidator(): ValidatorFn {
    return control => {
      if (!control.value) {
        return {
          required: true
        };
      }
      if (!this.behaviorsComponent.behaviorIdUnique(control.value, this.index)) {
        return {
          behaviorIdNotUnique: true
        };
      }
      return null;
    };
  }

  private onTypeChanged(newType: ScadaSymbolBehaviorType) {
    const prevModel = deepClone(this.modelValue);
    this.modelValue = {...this.modelValue, ...{type: newType}};
    this.modelValue = updateBehaviorDefaultSettings(this.modelValue);
    if (!behaviorValid(this.modelValue)) {
      this.editBehavior(null, this.editButton, false, () => {
        this.modelValue = prevModel;
        this.behaviorRowFormGroup.patchValue(
          {
            type: prevModel.type
          }, {emitEvent: true}
        );
      });
    }
  }

  private updateModel() {
    const value: ScadaSymbolBehavior = this.behaviorRowFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }

}
