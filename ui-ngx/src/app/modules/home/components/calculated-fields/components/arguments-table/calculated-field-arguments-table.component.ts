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
  DestroyRef,
  effect,
  forwardRef,
  input,
  Input,
  Renderer2,
  ViewContainerRef,
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  ArgumentEntityType,
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgument,
  CalculatedFieldArgumentValue,
  CalculatedFieldType,
} from '@shared/models/calculated-field.models';
import { CalculatedFieldArgumentPanelComponent } from '@home/components/calculated-fields/components/public-api';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull } from '@core/utils';
import { noLeadTrailSpacesRegex } from '@shared/models/regex.constants';

@Component({
  selector: 'tb-calculated-field-arguments-table',
  templateUrl: './calculated-field-arguments-table.component.html',
  styleUrls: [`calculated-field-arguments-table.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldArgumentsTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldArgumentsTableComponent),
      multi: true
    }
  ],
})
export class CalculatedFieldArgumentsTableComponent implements ControlValueAccessor, Validator {

  @Input() entityId: EntityId;
  @Input() tenantId: string;

  calculatedFieldType = input<CalculatedFieldType>()

  errorText = '';
  argumentsFormArray = this.fb.array<AbstractControl>([]);
  keysPopupClosed = true;

  readonly entityTypeTranslations = entityTypeTranslations;
  readonly ArgumentTypeTranslations = ArgumentTypeTranslations;
  readonly EntityType = EntityType;
  readonly ArgumentEntityType = ArgumentEntityType;

  private propagateChange: (argumentsObj: Record<string, CalculatedFieldArgument>) => void = () => {};

  constructor(
    private fb: FormBuilder,
    private popoverService: TbPopoverService,
    private viewContainerRef: ViewContainerRef,
    private destroyRef: DestroyRef,
    private cd: ChangeDetectorRef,
    private renderer: Renderer2
  ) {
    this.argumentsFormArray.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.propagateChange(this.getArgumentsObject());
    });
    effect(() => this.calculatedFieldType() && this.argumentsFormArray.updateValueAndValidity());
  }

  registerOnChange(fn: (argumentsObj: Record<string, CalculatedFieldArgument>) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_): void {}

  validate(): ValidationErrors | null {
    this.updateErrorText();
    return this.errorText ? { argumentsFormArray: false } : null;
  }

  onDelete(index: number): void {
    this.argumentsFormArray.removeAt(index);
    this.argumentsFormArray.markAsDirty();
  }

  manageArgument($event: Event, matButton: MatButton, index?: number): void {
    $event?.stopPropagation();
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx = {
        index,
        argument: this.argumentsFormArray.at(index)?.getRawValue() ?? {},
        entityId: this.entityId,
        calculatedFieldType: this.calculatedFieldType(),
        buttonTitle: this.argumentsFormArray.at(index)?.value ? 'action.apply' : 'action.add',
        tenantId: this.tenantId,
      };
      this.keysPopupClosed = false;
      const argumentsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, CalculatedFieldArgumentPanelComponent, 'left', false, null,
        ctx,
        {},
        {}, {}, true);
      argumentsPanelPopover.tbComponentRef.instance.popover = argumentsPanelPopover;
      argumentsPanelPopover.tbComponentRef.instance.argumentsDataApplied.subscribe(({ value, index }) => {
        argumentsPanelPopover.hide();
        const formGroup = this.getArgumentFormGroup(value);
        if (isDefinedAndNotNull(index)) {
          this.argumentsFormArray.setControl(index, formGroup);
        } else {
          this.argumentsFormArray.push(formGroup);
        }
        this.argumentsFormArray.markAsDirty();
        this.cd.markForCheck();
      });
      argumentsPanelPopover.tbHideStart.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
        this.keysPopupClosed = true;
      });
    }
  }

  private updateErrorText(): void {
    if (this.calculatedFieldType() === CalculatedFieldType.SIMPLE
      && this.argumentsFormArray.controls.some(control => control.get('refEntityKey').get('type').value === ArgumentType.Rolling)) {
      this.errorText = 'calculated-fields.hint.arguments-simple-with-rolling';
    } else if (!this.argumentsFormArray.controls.length) {
      this.errorText = 'calculated-fields.hint.arguments-empty';
    } else {
      this.errorText = '';
    }
  }

  private getArgumentsObject(): Record<string, CalculatedFieldArgument> {
    return this.argumentsFormArray.controls.reduce((acc, control) => {
      const rawValue = control.getRawValue();
      const { argumentName, ...argument } = rawValue as CalculatedFieldArgumentValue;
      acc[argumentName] = argument;
      return acc;
    }, {} as Record<string, CalculatedFieldArgument>);
  }

  writeValue(argumentsObj: Record<string, CalculatedFieldArgument>): void {
    this.argumentsFormArray.clear();
    this.populateArgumentsFormArray(argumentsObj)
  }

  private populateArgumentsFormArray(argumentsObj: Record<string, CalculatedFieldArgument>): void {
    Object.keys(argumentsObj).forEach(key => {
      this.argumentsFormArray.push(this.fb.group({
        argumentName: [key, [Validators.required, Validators.maxLength(255), Validators.pattern(noLeadTrailSpacesRegex)]],
        ...argumentsObj[key],
        ...(argumentsObj[key].refEntityId ? {
          refEntityId: this.fb.group({
            entityType: [{ value: argumentsObj[key].refEntityId.entityType, disabled: true }],
            id: [{ value: argumentsObj[key].refEntityId.id , disabled: true }],
          }),
        } : {}),
        refEntityKey: this.fb.group({
          type: [{ value: argumentsObj[key].refEntityKey.type, disabled: true }],
          key: [{ value: argumentsObj[key].refEntityKey.key, disabled: true }],
        }),
      }) as AbstractControl);
    });
  }

  private getArgumentFormGroup(value: CalculatedFieldArgumentValue): AbstractControl {
    return this.fb.group({
      ...value,
      argumentName: [value.argumentName, [Validators.required, Validators.maxLength(255), Validators.pattern(noLeadTrailSpacesRegex)]],
      ...(value.refEntityId ? {
        refEntityId: this.fb.group({
          entityType: [{ value: value.refEntityId.entityType, disabled: true }],
          id: [{ value: value.refEntityId.id , disabled: true }],
        }),
      } : {}),
      refEntityKey: this.fb.group({
        type: [{ value: value.refEntityKey.type, disabled: true }],
        key: [{ value: value.refEntityKey.key, disabled: true }],
      }),
    })
  }
}
