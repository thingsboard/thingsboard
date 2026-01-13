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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/public-api';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  ArgumentName,
  ArgumentType,
  ArgumentTypeMap,
  AttributeScope,
  AttributeScopeMap,
  MathFunction,
  MathFunctionMap
} from './../rule-node-config.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-arguments-map-config',
  templateUrl: './arguments-map-config.component.html',
  styleUrls: ['./arguments-map-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ArgumentsMapConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ArgumentsMapConfigComponent),
      multi: true,
    }
  ]
})
export class ArgumentsMapConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, Validator {

  @Input() disabled: boolean;

  private functionValue: MathFunction;

  get function(): MathFunction {
    return this.functionValue;
  }

  @Input()
  set function(funcName: MathFunction) {
    if (funcName && this.functionValue !== funcName) {
      this.functionValue = funcName;
      this.setupArgumentsFormGroup(true);
    }
  }

  maxArgs = 16;
  minArgs = 1;
  displayArgumentName = false;

  mathFunctionMap = MathFunctionMap;
  ArgumentType = ArgumentType;

  argumentsFormGroup: FormGroup;

  attributeScopeMap = AttributeScopeMap;
  argumentTypeMap = ArgumentTypeMap;
  arguments = Object.values(ArgumentType);
  attributeScope = Object.values(AttributeScope);

  private propagateChange = null;

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.argumentsFormGroup = this.fb.group({
      arguments: this.fb.array([])
    });

    this.argumentsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });

    this.setupArgumentsFormGroup();
  }

  public onDrop(event: CdkDragDrop<string[]>) {
    const columnsFormArray = this.argumentsFormArray;
    const columnForm = columnsFormArray.at(event.previousIndex);
    columnsFormArray.removeAt(event.previousIndex);
    columnsFormArray.insert(event.currentIndex, columnForm);
    this.updateArgumentNames();
  }

  get argumentsFormArray(): FormArray {
    return this.argumentsFormGroup.get('arguments') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.argumentsFormGroup.disable({emitEvent: false});
    } else {
      this.argumentsFormGroup.enable({emitEvent: false});
      this.argumentsFormArray.controls
        .forEach((control: FormGroup) => this.updateArgumentControlValidators(control));
    }
  }

  writeValue(argumentsList: Array<any>): void {
    const argumentsControls: Array<FormGroup> = [];
    if (argumentsList) {
      argumentsList.forEach((property, index) => {
        argumentsControls.push(this.createArgumentControl(property, index));
      });
    }
    this.argumentsFormGroup.setControl('arguments', this.fb.array(argumentsControls), {emitEvent: false});
    this.setupArgumentsFormGroup();
  }


  public removeArgument(index: number) {
    this.argumentsFormArray.removeAt(index);
    this.updateArgumentNames();
  }

  public addArgument(emitEvent = true) {
    const argumentsFormArray = this.argumentsFormArray;
    const argumentControl = this.createArgumentControl(null, argumentsFormArray.length);
    argumentsFormArray.push(argumentControl, {emitEvent});
  }

  public validate() {
    if (!this.argumentsFormGroup.valid) {
      return {
        argumentsRequired: true
      };
    }
    return null;
  }

  private setupArgumentsFormGroup(emitEvent = false) {
    if (this.function) {
      this.maxArgs = this.mathFunctionMap.get(this.function).maxArgs;
      this.minArgs = this.mathFunctionMap.get(this.function).minArgs;
      this.displayArgumentName = this.function === MathFunction.CUSTOM;
    }
    if (this.argumentsFormGroup) {
      this.argumentsFormGroup.get('arguments').setValidators([Validators.minLength(this.minArgs), Validators.maxLength(this.maxArgs)]);
      while (this.argumentsFormArray.length > this.maxArgs) {
        this.removeArgument(this.maxArgs - 1);
      }
      while (this.argumentsFormArray.length < this.minArgs) {
        this.addArgument(emitEvent);
      }
      this.argumentsFormGroup.get('arguments').updateValueAndValidity({emitEvent: false});
    }
  }

  private createArgumentControl(property: any, index: number): FormGroup {
    const argumentControl = this.fb.group({
      type: [property?.type, [Validators.required]],
      key: [property?.key, [Validators.required]],
      name: [ArgumentName[index], [Validators.required]],
      attributeScope: [property?.attributeScope ?? AttributeScope.SERVER_SCOPE, [Validators.required]],
      defaultValue: [property?.defaultValue ?? null]
    });
    this.updateArgumentControlValidators(argumentControl);
    argumentControl.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateArgumentControlValidators(argumentControl);
      argumentControl.get('attributeScope').updateValueAndValidity({emitEvent: false});
      argumentControl.get('defaultValue').updateValueAndValidity({emitEvent: false});
    });
    return argumentControl;
  }

  private updateArgumentControlValidators(control: FormGroup) {
    const argumentType: ArgumentType = control.get('type').value;
    if (argumentType === ArgumentType.ATTRIBUTE) {
      control.get('attributeScope').enable({emitEvent: false});
    } else {
      control.get('attributeScope').disable({emitEvent: false});
    }
    if (argumentType && argumentType !== ArgumentType.CONSTANT) {
      control.get('defaultValue').enable({emitEvent: false});
    } else {
      control.get('defaultValue').disable({emitEvent: false});
    }
  }

  private updateArgumentNames() {
    this.argumentsFormArray.controls.forEach((argumentControl, argumentIndex) => {
      argumentControl.get('name').setValue(ArgumentName[argumentIndex]);
    });
  }

  private updateModel() {
    const argumentsForm = this.argumentsFormArray.value;
    if (!argumentsForm.length || !this.argumentsFormGroup.valid) {
      this.propagateChange(null);
    } else {
      this.propagateChange(argumentsForm);
    }
  }
}
