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
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { TranslateService } from '@ngx-translate/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-select-attributes',
    templateUrl: './select-attributes.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SelectAttributesComponent),
            multi: true
        }, {
            provide: NG_VALIDATORS,
            useExisting: SelectAttributesComponent,
            multi: true
        }],
    standalone: false
})

export class SelectAttributesComponent implements OnInit, ControlValueAccessor {

  private propagateChange = (v: any) => { };

  public attributeControlGroup: FormGroup;
  public separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
  public onTouched = () => {};

  @Input() popupHelpLink: string;

  constructor(public translate: TranslateService,
              private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.attributeControlGroup = this.fb.group({
      clientAttributeNames: [[], []],
      sharedAttributeNames: [[], []],
      serverAttributeNames: [[], []],
      latestTsKeyNames: [[], []],
      getLatestValueWithTs: [false, []]
    }, {
      validators: this.atLeastOne(Validators.required, ['clientAttributeNames', 'sharedAttributeNames',
        'serverAttributeNames', 'latestTsKeyNames'])
    });

    this.attributeControlGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      this.propagateChange(this.preparePropagateValue(value));
    });
  }

  private preparePropagateValue(propagateValue: {[key: string]: string[] | boolean | null}): {[key: string]: string[] | boolean } {
    const formatValue = {};
    for (const key in propagateValue) {
      if (key === 'getLatestValueWithTs') {
        formatValue[key] = propagateValue[key];
      } else {
        formatValue[key] = isDefinedAndNotNull(propagateValue[key]) ? propagateValue[key] : [];
      }
    }

    return formatValue;
  };

  validate() {
    if (this.attributeControlGroup.valid) {
      return null;
    } else {
      return {atLeastOneRequired: true};
    }
  }

  private atLeastOne(validator: ValidatorFn, controls: string[] = null) {
    return (group: FormGroup): ValidationErrors | null => {
      if (!controls) {
        controls = Object.keys(group.controls);
      }
      const hasAtLeastOne = group?.controls && controls.some(k => !validator(group.controls[k]));

      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }

  writeValue(value): void {
    this.attributeControlGroup.setValue(value, {emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.attributeControlGroup.disable({emitEvent: false});
    } else {
      this.attributeControlGroup.enable({emitEvent: false});
    }
  }
}
