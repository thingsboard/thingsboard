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
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewChild,
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
import { JsFuncModulesComponent } from '@shared/components/js-func-modules.component';
import { ResourceSubType } from '@shared/models/resource.models';
import { Observable } from 'rxjs';
import { ResourceAutocompleteComponent } from '@shared/components/resource/resource-autocomplete.component';
import { HttpClient } from '@angular/common/http';
import { loadModuleMarkdownDescription, loadModuleMarkdownSourceCode } from '@shared/models/js-function.models';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface JsFuncModuleRow {
  alias: string;
  moduleLink: string;
}

export const moduleValid = (module: JsFuncModuleRow): boolean => !(!module.alias || !module.moduleLink);

@Component({
    selector: 'tb-js-func-module-row',
    templateUrl: './js-func-module-row.component.html',
    styleUrls: ['./js-func-module-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => JsFuncModuleRowComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => JsFuncModuleRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class JsFuncModuleRowComponent implements ControlValueAccessor, OnInit, Validator {

  ResourceSubType = ResourceSubType;

  @ViewChild('resourceAutocomplete')
  resourceAutocomplete: ResourceAutocompleteComponent;

  @Input()
  index: number;

  @Output()
  moduleRemoved = new EventEmitter();

  moduleRowFormGroup: UntypedFormGroup;

  modelValue: JsFuncModuleRow;

  moduleDescription = this.loadModuleDescription.bind(this);

  moduleSourceCode = this.loadModuleSourceCode.bind(this);

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private modulesComponent: JsFuncModulesComponent,
              private http: HttpClient,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {}

  ngOnInit() {
    this.moduleRowFormGroup = this.fb.group({
      alias: [null, [this.moduleAliasValidator(), Validators.pattern(/^[$_\p{ID_Start}][$\p{ID_Continue}]*$/u)]],
      moduleLink: [null, [Validators.required]]
    });
    this.moduleRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  writeValue(value: JsFuncModuleRow): void {
    this.modelValue = value;
    this.moduleRowFormGroup.patchValue(
      {
        alias: value?.alias,
        moduleLink: value?.moduleLink
      }, {emitEvent: false}
    );
    this.cd.markForCheck();
  }

  public validate(_c: UntypedFormControl) {
    const aliasControl = this.moduleRowFormGroup.get('alias');
    if (aliasControl.hasError('moduleAliasNotUnique') || aliasControl.hasError('pattern')) {
      aliasControl.updateValueAndValidity({onlySelf: false, emitEvent: false});
    }
    if (aliasControl.hasError('moduleAliasNotUnique')) {
      this.moduleRowFormGroup.get('alias').markAsTouched();
      return {
        moduleAliasNotUnique: true
      };
    }
    if (aliasControl.hasError('pattern')) {
      this.moduleRowFormGroup.get('alias').markAsTouched();
      return {
        invalidVariableName: true
      };
    }
    const module: JsFuncModuleRow = {...this.modelValue, ...this.moduleRowFormGroup.value};
    if (!moduleValid(module)) {
      return {
        module: true
      };
    }
    return null;
  }

  private loadModuleDescription(): Observable<string> | null {
    const moduleLink = this.moduleRowFormGroup.get('moduleLink').value;
    if (moduleLink) {
      const resource = this.resourceAutocomplete.resource;
      return loadModuleMarkdownDescription(this.http, this.translate, resource);
    } else {
      return null;
    }
  }

  private loadModuleSourceCode(): Observable<string> | null {
    const moduleLink = this.moduleRowFormGroup.get('moduleLink').value;
    if (moduleLink) {
      const resource = this.resourceAutocomplete.resource;
      return loadModuleMarkdownSourceCode(this.http, this.translate, resource);
    } else {
      return null;
    }
  }

  private moduleAliasValidator(): ValidatorFn {
    return control => {
      if (!control.value) {
        return {
          required: true
        };
      }
      if (!this.modulesComponent.moduleAliasUnique(control.value, this.index)) {
        return {
          moduleAliasNotUnique: true
        };
      }
      return null;
    };
  }

  private updateModel() {
    const value: JsFuncModuleRow = this.moduleRowFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }

}
