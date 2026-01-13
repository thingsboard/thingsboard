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
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges
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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefinedAndNotNull, mergeDeep, trimDefaultValues } from '@core/utils';
import {
  defaultFormProperties,
  FormProperty,
  FormPropertyContainerType,
  FormPropertyGroup,
  FormPropertyType,
  isInputFieldPropertyType,
  PropertyConditionFunction,
  toPropertyGroups
} from '@shared/models/dynamic-form.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { ContentType } from '@shared/models/constants';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'tb-dynamic-form',
  templateUrl: './dynamic-form.component.html',
  styleUrls: ['./dynamic-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DynamicFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DynamicFormComponent),
      multi: true
    }
  ]
})
export class DynamicFormComponent implements OnInit, OnChanges, ControlValueAccessor, Validator {

  isInputFieldPropertyType = isInputFieldPropertyType;

  FormPropertyContainerType = FormPropertyContainerType;

  FormPropertyType = FormPropertyType;

  ContentType = ContentType;

  @Input()
  disabled: boolean;

  @Input()
  properties: FormProperty[];

  @Input()
  title: string;

  @Input()
  @coerceBoolean()
  isArrayItem = false;

  @Input()
  @coerceBoolean()
  stroked = false;

  @Input()
  @coerceBoolean()
  noPadding = false;

  @Input()
  @coerceBoolean()
  noBorder = false;

  @Input()
  @coerceBoolean()
  trimDefaults = false;

  private modelValue: {[id: string]: any};

  private propagateChange = null;

  private defaults: {[id: string]: any};

  private validatorTriggers: string[];

  public propertiesFormGroup: UntypedFormGroup;

  propertyGroups: FormPropertyGroup[];

  constructor(protected store: Store<AppState>,
              private customTranslate: CustomTranslatePipe,
              private sanitizer: DomSanitizer,
              private destroyRef: DestroyRef,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.propertiesFormGroup = this.fb.group({
    });
    this.propertiesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.loadMetadata();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['properties'].includes(propName)) {
          this.loadMetadata();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.propertiesFormGroup.disable({emitEvent: false});
    } else {
      this.propertiesFormGroup.enable({emitEvent: false});
      this.updateControlsState();
    }
  }

  writeValue(value: {[id: string]: any}): void {
    this.modelValue = value || {};
    this.setupValue();
  }

  validate(_c: UntypedFormControl) {
    const valid = this.propertiesFormGroup.valid;
    return valid ? null : {
      properties: {
        valid: false,
      },
    };
  }

  private loadMetadata() {
    this.validatorTriggers = [];
    this.propertyGroups = [];
    this.defaults = {};

    for (const control of Object.keys(this.propertiesFormGroup.controls)) {
      this.propertiesFormGroup.removeControl(control, {emitEvent: false});
    }
    if (this.properties) {
      this.defaults = defaultFormProperties(this.properties);
      for (let property of this.properties) {
        property.disabled = false;
        property.visible = true;
        if (property.condition) {
          try {
            property.conditionFunction = new Function('property', 'model', property.condition) as PropertyConditionFunction;
          } catch (_e) {
          }
        }
      }
      this.propertyGroups = toPropertyGroups(this.properties, this.isArrayItem, this.customTranslate, this.sanitizer);
      for (const property of this.properties) {
        if (property.type !== FormPropertyType.htmlSection) {
          if (property.disableOnProperty) {
            if (!this.validatorTriggers.includes(property.disableOnProperty)) {
              this.validatorTriggers.push(property.disableOnProperty);
            }
          }
          const validators: ValidatorFn[] = [];
          if (property.required) {
            validators.push(Validators.required);
          }
          if (property.type === FormPropertyType.number) {
            if (isDefinedAndNotNull(property.min)) {
              validators.push(Validators.min(property.min));
            }
            if (isDefinedAndNotNull(property.max)) {
              validators.push(Validators.max(property.max));
            }
          }
          if (property.type === FormPropertyType.select) {
            if (property.multiple) {
              if (isDefinedAndNotNull(property.minItems)) {
                validators.push(Validators.minLength(property.minItems));
              }
              if (isDefinedAndNotNull(property.maxItems)) {
                validators.push(Validators.maxLength(property.maxItems));
              }
            }
          }
          this.propertiesFormGroup.addControl(property.id, this.fb.control(null, validators), {emitEvent: false});
        }
      }
    }
    this.setupValue();
    this.cd.markForCheck();
  }

  private calculateControlsState(updateControls = false) {
    for (const trigger of this.validatorTriggers) {
      const value: boolean = this.propertiesFormGroup.get(trigger).value;
      this.properties.filter(p => p.disableOnProperty === trigger).forEach(
        (p) => {
          p.disabled = !value;
        }
      );
    }
    if (this.properties) {
      for (let property of this.properties) {
        if (property.conditionFunction) {
          property.visible = property.conditionFunction(property, this.modelValue);
        }
      }
      this.propertyGroups.forEach(g => {
        g.containers.forEach(container => {
          if ([FormPropertyContainerType.fieldset, FormPropertyContainerType.field, FormPropertyContainerType.htmlSection, FormPropertyContainerType.array].includes(container.type)) {
            container.visible = container.property.visible;
          } else {
            container.visible = container.switch?.visible || container.properties.some(p => p.visible);
          }
        });
        g.visible = g.containers.some(c => c.visible);
      });
    }
    if (updateControls) {
      this.updateControlsState();
    }
  }

  private updateControlsState() {
    if (this.properties) {
      for (let property of this.properties) {
        if (property.type !== FormPropertyType.htmlSection) {
          const control = this.propertiesFormGroup.get(property.id);
          if (property.visible && !property.disabled) {
            control.enable({emitEvent: false});
            control.updateValueAndValidity({emitEvent: false});
          } else {
            control.disable({emitEvent: false});
          }
        }
      }
    }
  }

  private setupValue() {
    if (this.properties) {
      this.modelValue = mergeDeep<{[id: string]: any}>({}, this.defaults, this.modelValue);
      this.propertiesFormGroup.patchValue(
        this.modelValue, {emitEvent: false}
      );
      this.calculateControlsState();
      this.setDisabledState(this.disabled);
    }
  }

  private updateModel() {
    this.modelValue = this.propertiesFormGroup.getRawValue();
    this.calculateControlsState(true);
    let result = this.modelValue;
    if (this.trimDefaults) {
      result = trimDefaultValues(this.modelValue, this.defaults);
    }
    this.propagateChange(result);
  }

}
