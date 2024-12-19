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
  Component,
  DestroyRef,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  QueryList,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { ScadaSymbolProperty, ScadaSymbolPropertyType } from '@home/components/widget/lib/scada/scada-symbol.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  propertyValid,
  ScadaSymbolPropertyRowComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-property-row.component';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-scada-symbol-metadata-properties',
  templateUrl: './scada-symbol-properties.component.html',
  styleUrls: ['./scada-symbol-properties.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolPropertiesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolPropertiesComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolPropertiesComponent implements ControlValueAccessor, OnInit, Validator {

  @HostBinding('style.display') styleDisplay = 'flex';
  @HostBinding('style.overflow') styleOverflow = 'hidden';

  @ViewChildren(ScadaSymbolPropertyRowComponent)
  propertyRows: QueryList<ScadaSymbolPropertyRowComponent>;

  @Input()
  disabled: boolean;

  booleanPropertyIds: string[] = [];

  propertiesFormGroup: UntypedFormGroup;

  errorText = '';

  get dragEnabled(): boolean {
    return !this.disabled && this.propertiesFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.propertiesFormGroup = this.fb.group({
      properties: this.fb.array([])
    });
    this.propertiesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let properties: ScadaSymbolProperty[] = this.propertiesFormGroup.get('properties').value;
        if (properties) {
          properties = properties.filter(p => propertyValid(p));
        }
        this.booleanPropertyIds = properties.filter(p => p.type === ScadaSymbolPropertyType.switch).map(p => p.id);
        properties.forEach((p, i) => {
          if (p.disableOnProperty && !this.booleanPropertyIds.includes(p.disableOnProperty)) {
            p.disableOnProperty = null;
            const controls = this.propertiesFormArray().controls;
            controls[i].patchValue(p, {emitEvent: false});
          }
        });
        this.propagateChange(properties);
      }
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
      this.propertiesFormGroup.disable({emitEvent: false});
    } else {
      this.propertiesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ScadaSymbolProperty[] | undefined): void {
    const properties= value || [];
    this.propertiesFormGroup.setControl('properties', this.preparePropertiesFormArray(properties), {emitEvent: false});
    this.booleanPropertyIds = properties.filter(p => p.type === ScadaSymbolPropertyType.switch).map(p => p.id);
  }

  public validate(c: UntypedFormControl) {
    this.errorText = '';
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const notUniqueControls =
      propertiesArray.controls.filter(control => control.hasError('propertyIdNotUnique'));
    for (const control of notUniqueControls) {
      control.updateValueAndValidity({onlySelf: false, emitEvent: false});
      if (control.hasError('propertyIdNotUnique')) {
        this.errorText = this.translate.instant('scada.property.not-unique-property-ids-error');
      }
    }
    const valid =  this.propertiesFormGroup.valid;
    return valid ? null : {
      properties: {
        valid: false,
      },
    };
  }

  public propertyIdUnique(id: string, index: number): boolean {
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    for (let i = 0; i < propertiesArray.controls.length; i++) {
      if (i !== index) {
        const otherControl = propertiesArray.controls[i];
        if (id === otherControl.value.id) {
          return false;
        }
      }
    }
    return true;
  }

  propertyDrop(event: CdkDragDrop<string[]>) {
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const property = propertiesArray.at(event.previousIndex);
    propertiesArray.removeAt(event.previousIndex);
    propertiesArray.insert(event.currentIndex, property);
  }

  propertiesFormArray(): UntypedFormArray {
    return this.propertiesFormGroup.get('properties') as UntypedFormArray;
  }

  trackByProperty(index: number, propertyControl: AbstractControl): any {
    return propertyControl;
  }

  removeProperty(index: number, emitEvent = true) {
    (this.propertiesFormGroup.get('properties') as UntypedFormArray).removeAt(index, {emitEvent});
  }

  addProperty() {
    const property: ScadaSymbolProperty = {
      id: '',
      name: '',
      type: ScadaSymbolPropertyType.text,
      default: ''
    };
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const propertyControl = this.fb.control(property, []);
    propertiesArray.push(propertyControl);
    setTimeout(() => {
      const propertyRow = this.propertyRows.get(this.propertyRows.length-1);
      propertyRow.onAdd(() => {
        this.removeProperty(propertiesArray.length-1);
      });
    });
  }

  private preparePropertiesFormArray(properties: ScadaSymbolProperty[] | undefined): UntypedFormArray {
    const propertiesControls: Array<AbstractControl> = [];
    if (properties) {
      properties.forEach((property) => {
        propertiesControls.push(this.fb.control(property, []));
      });
    }
    return this.fb.array(propertiesControls);
  }
}
