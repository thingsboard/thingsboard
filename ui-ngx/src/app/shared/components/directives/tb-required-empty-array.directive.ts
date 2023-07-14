///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Directive } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';

@Directive({
  selector: '[tb-required-empty-array]',
  providers: [{
    provide: NG_VALIDATORS,
    useExisting: TbRequiredEmptyArrayDirective,
    multi: true
  }]
})
export class TbRequiredEmptyArrayDirective implements Validator {
  validate(control: AbstractControl): ValidationErrors | null {
    const value: any = control.value;
    if (value == null || (typeof value === 'string' && value.length === 0)) {
      return {
        required: true
      };
    }
    return null;
  }
}
