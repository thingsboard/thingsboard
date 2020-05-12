///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  FormControl,
  FormGroupDirective,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  NgForm,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';

@Directive({
  selector: '[tb-json-to-string]',
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
  private parseError: boolean;
  private data: any;

  @HostListener('input', ['$event.target.value']) input(newValue: any): void {
    try {
      this.data = JSON.parse(newValue);
      this.parseError = false;
    } catch (e) {
      this.parseError = true;
    }

    this.propagateChange(this.data);
  }

  constructor(private render: Renderer2,
              private element: ElementRef,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {

  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.parseError);
    return originalErrorState || customErrorState;
  }

  validate(c: FormControl): ValidationErrors {
    return (!this.parseError) ? null : {
      invalidJSON: {
        valid: false
      }
    };
  }

  writeValue(obj: any): void {
    if (obj) {
      this.data = obj;
      this.parseError = false;
      this.render.setProperty(this.element.nativeElement, 'value', JSON.stringify(obj));
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }
}
