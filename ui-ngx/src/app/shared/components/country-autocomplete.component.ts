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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import { Country, CountryData } from '@shared/models/country.models';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { isNotEmptyStr, requireMatch } from '@core/utils';
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TranslateService } from '@ngx-translate/core';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';

interface CountrySearchData extends Country {
  searchText?: string;
}

@Component({
  selector: 'tb-country-autocomplete',
  templateUrl: 'country-autocomplete.component.html',
  styleUrls: ['./country-autocomplete.component.scss'],
  providers: [
    CountryData,
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CountryAutocompleteComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CountryAutocompleteComponent),
      multi: true
    }
  ]
})
export class CountryAutocompleteComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  labelText = this.translate.instant('contact.country');

  @Input()
  requiredText = this.translate.instant('contact.country-required');

  @Input()
  requiredMatchText = this.translate.instant('contact.country-required-match');

  @Input()
  autocompleteHint: string;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @ViewChild('countryInput', {static: true}) countryInput: ElementRef;

  @ViewChild('autocompleteTrigger') autocompleteTrigger: MatAutocompleteTrigger;

  @Output()
  selectCountryCode = new EventEmitter<string>();

  countryFormGroup: FormGroup;

  searchText = '';

  filteredCountries: Observable<Array<Country>>;

  onTouched = () => {
  };
  private propagateChange: (value: any) => void = () => {
  };

  private modelValue: Country;

  private allCountries: CountrySearchData[] = this.countryData.allCountries;
  private initSearchData = false;
  private dirty = false;

  constructor(private fb: FormBuilder,
              private countryData: CountryData,
              private translate: TranslateService) {
    this.countryFormGroup = this.fb.group({
      country: ['', requireMatch()]
    });
  }

  ngOnInit(): void {
    if (this.required) {
      this.countryFormGroup.get('country').addValidators(Validators.required);
      this.countryFormGroup.get('country').updateValueAndValidity();
    }
    this.filteredCountries = this.countryFormGroup.get('country').valueChanges.pipe(
      debounceTime(150),
      tap(value => {
        let modelValue: Country;
        if (typeof value === 'string' || !value) {
          modelValue = null;
        } else {
          modelValue = value;
        }
        this.updateView(modelValue);
        if (value === null) {
          this.clear();
        }
      }),
      map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
      distinctUntilChanged(),
      switchMap(name => of(this.fetchCountries(name))),
      share()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.countryFormGroup.disable({emitEvent: false});
    } else {
      this.countryFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.countryFormGroup.valid ? null : {
      countryFormGroup: false
    };
  }

  writeValue(country: string) {
    this.dirty = true;

    const findCountry = isNotEmptyStr(country) ? this.allCountries.find(item => item.name === country) : null;

    this.modelValue = findCountry || null;
    this.countryFormGroup.get('country').patchValue(this.modelValue || '', { emitEvent: false });
    this.selectCountryCode.emit(this.modelValue ? this.modelValue.iso2 : null);
  }

  displayCountryFn(country?: Country): string | undefined {
    return country ? country.name : undefined;
  }

  onFocus() {
    if (this.dirty) {
      this.countryFormGroup.get('country').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  checkInputAndAutoSelect() {
    const control = this.countryFormGroup.get('country');
    const value = control.value;

    if (value && typeof value === 'string') {
      const filterValue = value.trim().toLowerCase();
      const foundCountry = this.allCountries.find(c => c.name.toLowerCase() === filterValue);

      if (foundCountry) {
        control.setValue(foundCountry);
        this.autocompleteTrigger.closePanel();
      }
    }

    this.onTouched();
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  clear() {
    this.countryFormGroup.get('country').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.countryInput.nativeElement.blur();
      this.countryInput.nativeElement.focus();
    }, 0);
  }

  private fetchCountries(searchText: string): Country[] {
    this.searchText = searchText;
    if (!this.initSearchData) {
      this.allCountries.forEach(country => {
        country.searchText = `${country.name} ${country.iso2}`.toLowerCase();
      });
      this.initSearchData = true;
    }
    if (isNotEmptyStr(searchText)) {
      const filterValue = searchText.toLowerCase();
      return this.allCountries.filter(country => country.searchText.includes(filterValue));
    }
    return this.allCountries;
  }

  private updateView(value: Country | null) {
    if (this.modelValue?.name !== value?.name) {
      this.modelValue = value;
      this.propagateChange(this.modelValue?.name);
      if (value) {
        this.selectCountryCode.emit(value.iso2);
      }
    }
  }
}
