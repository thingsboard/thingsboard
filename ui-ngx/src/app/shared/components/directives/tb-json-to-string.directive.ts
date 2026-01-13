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

import { Directive, ElementRef, forwardRef, HostListener, Renderer2, SkipSelf } from '@angular/core';
import {
  ControlValueAccessor,
  FormGroupDirective,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NgForm,
  UntypedFormControl,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { isObject } from '@core/utils';

@Directive({
  selector: '[tb-json-to-string]',
  // eslint-disable-next-line @angular-eslint/no-host-metadata-property
  host: {
    '(blur)': 'onTouched()'
  },
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TbJsonToStringDirective),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => TbJsonToStringDirective),
    multi: true,
  },
  {
    provide: ErrorStateMatcher,
    useExisting: TbJsonToStringDirective
  }]
})

export class TbJsonToStringDirective implements ControlValueAccessor, Validator, ErrorStateMatcher {
  private propagateChange = null;
  public onTouched = () => {};
  private parseError: boolean;
  private data: any;

  @HostListener('input', ['$event.target.value']) input(newValue: any): void {
    try {
      if (newValue) {
        this.data = JSON.parse(newValue);
        if (isObject(this.data)) {
          this.parseError = false;
        } else {
          this.data = null;
          this.parseError = true;
        }
      } else {
        this.data = null;
        this.parseError = false;
      }
    } catch (e) {
      this.data = null;
      this.parseError = true;
    }

    this.propagateChange(this.data);
  }

  constructor(private render: Renderer2,
              private element: ElementRef,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {

  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!(control && control.invalid && !Array.isArray(control.value) && control.touched);
  }

  validate(c: UntypedFormControl): ValidationErrors {
    return (!this.parseError) ? null : {
      invalidJSON: {
        valid: false
      }
    };
  }

  writeValue(obj: any): void {
    this.data = obj;
    this.parseError = false;
    this.render.setProperty(this.element.nativeElement, 'value', obj ? JSON.stringify(obj) : '');
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
}
