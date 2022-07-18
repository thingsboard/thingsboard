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

  @Input()
  disabled: boolean;

  @Input()
  defaultCountry = 'US';

  @Input()
  enableFlagsSelect = true;

  @Input()
  required = true;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'legacy';

  @Input()
  placeholder;

  @Input()
  label = 'phone-input.phone-input-label';

  allCountries: Array<Country> = this.countryCodeData.allCountries;
  phonePlaceholder = '+12015550123';
  flagIcon: string;
  phoneFormGroup: FormGroup;

  private isLoading = true;
  get isLoad(): boolean {
    return this.isLoading;
  }

  set isLoad(value) {
    if (this.isLoading) {
      this.isLoading = value;
      if (this.phoneFormGroup) {
        this.defineCountryFromNumber(this.phoneFormGroup.get('phoneNumber').value);
      }
    }
  }

  private getExampleNumber;
  private parsePhoneNumberFromString;
  private baseCode = 127397;
  private countryCallingCode = '+';
  private modelValue: string;
  private changeSubscriptions: Subscription[] = [];
  private validators: ValidatorFn[] = [(c: FormControl) => Validators.pattern(this.getPhoneNumberPattern())(c), this.validatePhoneNumber()];

  private propagateChange = (v: any) => { };

  constructor(private translate: TranslateService,
              private fb: FormBuilder,
              private countryCodeData: CountryData) {
    import('libphonenumber-js/max').then((libphonenubmer) => {
      this.parsePhoneNumberFromString = libphonenubmer.parsePhoneNumberFromString;
      this.getExampleNumber = libphonenubmer.getExampleNumber;
    }).then(() => this.isLoad = false);
  }

  ngOnInit(): void {
    if (this.required) {
      this.validators.push(Validators.required);
    }
    this.phoneFormGroup = this.fb.group({
      country: [this.defaultCountry, []],
      phoneNumber: [null, this.validators]
    });

    this.flagIcon = this.getFlagIcon(this.phoneFormGroup.get('country').value);

    this.changeSubscriptions.push(this.phoneFormGroup.get('phoneNumber').valueChanges.subscribe(value => {
      this.updateModel();
      this.defineCountryFromNumber(value);
    }));

    this.changeSubscriptions.push(this.phoneFormGroup.get('country').valueChanges.subscribe(value => {
      if (value) {
        const code = this.countryCallingCode;
        this.getFlagAndPhoneNumberData(value);
        let phoneNumber = this.phoneFormGroup.get('phoneNumber').value;
        if (phoneNumber) {
          if (code !== this.countryCallingCode && phoneNumber.includes(code)) {
            phoneNumber = phoneNumber.replace(code, this.countryCallingCode);
            this.phoneFormGroup.get('phoneNumber').patchValue(phoneNumber);
          }
        }
      }
    }));
  }

  ngOnDestroy() {
    for (const subscription of this.changeSubscriptions) {
      subscription.unsubscribe();
    }
  }

  focus() {
    const phoneNumber = this.phoneFormGroup.get('phoneNumber');
    if (!phoneNumber.value) {
      phoneNumber.patchValue(this.countryCallingCode, {emitEvent: true});
    }
  }

  private getFlagAndPhoneNumberData(country) {
    if (this.enableFlagsSelect) {
      this.flagIcon = this.getFlagIcon(country);
    }
    this.getPhoneNumberData(country);
  }

  private getPhoneNumberData(country): void {
    if (this.getExampleNumber) {
      const phoneData = this.getExampleNumber(country, examples);
      this.phonePlaceholder = phoneData.number;
      this.countryCallingCode = `+${this.enableFlagsSelect ? phoneData.countryCallingCode : ''}`;
    }
  }

  private getFlagIcon(countryCode) {
    return String.fromCodePoint(...countryCode.split('').map(country => this.baseCode + country.charCodeAt(0)));
  }

  validatePhoneNumber(): ValidatorFn {
    return (c: FormControl) => {
      const phoneNumber = c.value;
      if (phoneNumber && this.parsePhoneNumberFromString) {
        const parsedPhoneNumber = this.parsePhoneNumberFromString(phoneNumber);
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

  private defineCountryFromNumber(phoneNumber) {
    if (phoneNumber && this.parsePhoneNumberFromString) {
      const parsedPhoneNumber = this.parsePhoneNumberFromString(phoneNumber);
      const country = this.phoneFormGroup.get('country').value;
      if (parsedPhoneNumber?.country && parsedPhoneNumber?.country !== country) {
        this.phoneFormGroup.get('country').patchValue(parsedPhoneNumber.country, {emitEvent: true});
      }
    }
  }

  private getPhoneNumberPattern(): RegExp {
    return new RegExp(`^${this.countryCallingCode.replace('+', '\\+')}$|^\\+[1-9]\\d{1,14}$`);
  }

  validate(): ValidationErrors | null {
    const phoneNumber = this.phoneFormGroup.get('phoneNumber');
    return phoneNumber.valid || this.countryCallingCode === phoneNumber.value ? null : {
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

  writeValue(phoneNumber): void {
    this.modelValue = phoneNumber;
    let country = this.defaultCountry;
    if (this.parsePhoneNumberFromString) {
      this.phoneFormGroup.get('phoneNumber').clearValidators();
      this.phoneFormGroup.get('phoneNumber').setValidators(this.validators);
      if (phoneNumber) {
        const parsedPhoneNumber = this.parsePhoneNumberFromString(phoneNumber);
        if (parsedPhoneNumber?.isValid() && parsedPhoneNumber?.isPossible()) {
          this.enableFlagsSelect = true;
        } else {
          const validators = [Validators.maxLength(255)];
          if (this.required) {
            validators.push(Validators.required);
          }
          this.phoneFormGroup.get('phoneNumber').setValidators(validators);
          this.enableFlagsSelect = false;
        }
      } else {
        this.enableFlagsSelect = true;
      }
      this.phoneFormGroup.updateValueAndValidity({emitEvent: false});
      country = phoneNumber ? this.parsePhoneNumberFromString(phoneNumber)?.country || this.defaultCountry : this.defaultCountry;
      this.getFlagAndPhoneNumberData(country);
    }
    this.phoneFormGroup.reset({phoneNumber, country}, {emitEvent: false});
  }

  private updateModel() {
    const phoneNumber = this.phoneFormGroup.get('phoneNumber');
    if (phoneNumber.value === '+' || phoneNumber.value === this.countryCallingCode) {
      this.propagateChange(null);
    } else if (phoneNumber.valid) {
      this.modelValue = phoneNumber.value;
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }
}
