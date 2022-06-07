///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { Country, CountryData } from '@shared/models/country.models';
import examples from 'libphonenumber-js/examples.mobile.json';
import { CountryCode, getExampleNumber, parsePhoneNumberFromString } from 'libphonenumber-js';
import { phoneNumberPattern } from '@shared/models/settings.models';
import { Subscription } from 'rxjs';
import { FloatLabelType, MatFormFieldAppearance } from '@angular/material/form-field/form-field';

@Component({
  selector: 'tb-phone-input',
  templateUrl: './phone-input.component.html',
  styleUrls: ['./phone-input.component.scss'],
  providers: [
    CountryData,
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PhoneInputComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PhoneInputComponent),
      multi: true
    }
  ]
})
export class PhoneInputComponent implements OnInit, ControlValueAccessor, Validator {

  @Input() disabled: boolean;
  @Input() defaultCountry: CountryCode = 'US';
  @Input() enableFlagsSelect = true;
  @Input() required = true;
  @Input() floatLabel: FloatLabelType = 'always';
  @Input() appearance: MatFormFieldAppearance = 'legacy';

  allCountries: Array<Country> = [];
  phonePlaceholder: string;
  countryCallingCode: string;
  flagIcon: string;

  phoneNumberPattern = phoneNumberPattern;

  private modelValue: string;
  public phoneFormGroup: FormGroup;
  private valueChange$: Subscription = null;
  private propagateChange = (v: any) => { };

  constructor(private translate: TranslateService,
              private fb: FormBuilder,
              private countryCodeData: CountryData) {
  }

  ngOnInit(): void {
    if (this.enableFlagsSelect) {
      this.fetchCountryData();
    }
    this.phoneFormGroup = this.fb.group({
      country: [this.defaultCountry, []],
      phoneNumber: [null, this.required ? [Validators.required] : []]
    });
    this.phoneFormGroup.get('phoneNumber').setValidators([Validators.pattern(phoneNumberPattern), this.validatePhoneNumber()]);

    this.valueChange$ = this.phoneFormGroup.get('phoneNumber').valueChanges.subscribe(() => {
      this.updateModel();
    });

    this.phoneFormGroup.get('country').valueChanges.subscribe(value => {
      if (value) {
        const code = this.countryCallingCode;
        this.getFlagAndPhoneNumberData(value);
        let phoneNumber = this.phoneFormGroup.get('phoneNumber').value;
        if (phoneNumber) {
          if (code !== this.countryCallingCode && phoneNumber.includes(code)) {
            phoneNumber = phoneNumber.replace(code, this.countryCallingCode);
          } else {
            phoneNumber = this.countryCallingCode;
          }
          this.phoneFormGroup.get('phoneNumber').patchValue(phoneNumber);
        }
      }
    });

    this.phoneFormGroup.get('phoneNumber').valueChanges.subscribe(value => {
      if (value) {
        const parsedPhoneNumber = parsePhoneNumberFromString(value);
        const country = this.phoneFormGroup.get('country').value;
        if (parsedPhoneNumber?.country && parsedPhoneNumber?.country !== country) {
          this.phoneFormGroup.get('country').patchValue(parsedPhoneNumber.country, {emitEvent: true});
        }
      }
    });
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
  }

  focus() {
    const phoneNumber = this.phoneFormGroup.get('phoneNumber');
    this.phoneFormGroup.markAsPristine();
    this.phoneFormGroup.markAsUntouched();
    if (!phoneNumber.value) {
      phoneNumber.patchValue(this.countryCallingCode);
    }
  }

  getFlagAndPhoneNumberData(country) {
    if (this.enableFlagsSelect) {
      this.flagIcon = this.getFlagIcon(country);
    }
    this.getPhoneNumberData(country);
  }

  getPhoneNumberData(country): void {
    const phoneData = getExampleNumber(country, examples);
    this.phonePlaceholder = phoneData.number;
    this.countryCallingCode = '+' + phoneData.countryCallingCode;
  }

  getFlagIcon(countryCode) {
    const base = 127462 - 65;
    return  String.fromCodePoint(...countryCode.split('').map(country => base + country.charCodeAt(0)));
  }

  validatePhoneNumber(): ValidatorFn {
    return (c: FormControl) => {
      const phoneNumber = c.value;
      if (phoneNumber) {
        const parsedPhoneNumber = parsePhoneNumberFromString(phoneNumber);
        if (!parsedPhoneNumber?.isValid() || !parsedPhoneNumber?.isPossible()) {
          return {
            invalidPhoneNumber: {
              valid: false
            }
          };
        }
      }
      return null;
    };
  }

  validate(): ValidationErrors | null {
    return this.phoneFormGroup.get('phoneNumber').valid ? null : {
      phoneFormGroup: false
    };
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.phoneFormGroup.disable({emitEvent: false});
    } else {
      this.phoneFormGroup.enable({emitEvent: false});
    }
  }

  protected fetchCountryData(): void {
    this.allCountries = [];
    this.countryCodeData.allCountries.forEach((c) => {
      const cc = c[1];
      const country: Country = {
        name: c[0].toString(),
        iso2: c[1].toString(),
        dialCode: c[2].toString(),
        flag: this.getFlagIcon(cc),
      };

      this.allCountries.push(country);
    });
  }

  writeValue(phoneNumber): void {
    this.modelValue = phoneNumber;
    const country = phoneNumber ? parsePhoneNumberFromString(phoneNumber)?.country : this.defaultCountry;
    this.getFlagAndPhoneNumberData(country);
    this.phoneFormGroup.patchValue({phoneNumber, country}, {emitEvent: !phoneNumber});
  }

  private updateModel() {
    const phoneNumber = this.phoneFormGroup.get('phoneNumber');
    if (phoneNumber.valid && phoneNumber.value) {
      this.modelValue = phoneNumber.value;
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
