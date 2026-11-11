// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { booleanAttribute, Component, forwardRef, Input, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { SignUpField, SignUpFieldId, SignUpFieldMap } from '@shared/models/self-register.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
    selector: 'tb-mobile-registration-fields-panel',
    templateUrl: './mobile-registration-fields-panel.component.html',
    styleUrls: ['./mobile-registration-fields-panel.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MobileRegistrationFieldsPanelComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MobileRegistrationFieldsPanelComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MobileRegistrationFieldsPanelComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  registrationFields = this.fb.array<SignUpField>([]);

  allowRegistrationFields: SignUpFieldId[];

  readonly maxFields = Array.from(SignUpFieldMap.keys()).length;
  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.registrationFields.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.propagateChange(value);
      this.calculateAllowFields();
    })
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.registrationFields.disable({emitEvent: false});
    } else {
      this.registrationFields.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    if (this.registrationFields.status !== 'DISABLED' && !this.registrationFields.valid) {
      return {
        invalidRegistrationFields: true
      };
    }
    return null;
  }

  writeValue(registrationFields: Array<SignUpField>) {
    if (this.registrationFields.length === registrationFields?.length) {
      this.registrationFields.patchValue(registrationFields, {emitEvent: false});
    } else {
      this.registrationFields.clear({emitEvent: false});
      registrationFields.forEach(item => {
        this.registrationFields.push(this.fb.control(item), {emitEvent: false})
      })
    }
    this.calculateAllowFields();
  }

  get dragEnabled(): boolean {
    return this.registrationFields.controls.length > 1;
  }

  addFields() {
    this.registrationFields.push(this.fb.control({} as any));
  }

  fieldDrop(event: CdkDragDrop<string[]>) {
    const axis = this.registrationFields.at(event.previousIndex);
    this.registrationFields.removeAt(event.previousIndex);
    this.registrationFields.insert(event.currentIndex, axis);
  }

  removeField(index: number) {
    this.registrationFields.removeAt(index);
  }

  private calculateAllowFields() {
    const allFields = Array.from(SignUpFieldMap.keys());
    const selectedFields: SignUpFieldId[] = [];
    this.registrationFields.value.forEach(item => {
      selectedFields.push(item.id);
    });
    this.allowRegistrationFields = allFields.filter(item => !selectedFields.includes(item))
  }
}
