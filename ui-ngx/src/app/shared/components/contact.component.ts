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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { Country, CountryData } from '../models/country.models';
import { Observable, map, startWith } from 'rxjs';
import { PhoneInputComponent } from './phone-input.component';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
@Component({
  selector: 'tb-contact',
  templateUrl: './contact.component.html',
  providers: [
    CountryData
  ]
})
export class ContactComponent implements OnInit {

  @Input()
  parentForm: UntypedFormGroup;

  @Input() isEdit: boolean;
  @Input() phoneInputDefaultCountry = 'US';

  @ViewChild('phoneInput') phoneInput: PhoneInputComponent | undefined;

  countries = this.countryData.allCountries;
  countriesFiltered: Observable<Country[]>;

  constructor(private countryData: CountryData) {
    this.displayCountryWithFlag = this.displayCountryWithFlag.bind(this);
  }

  ngOnInit() {
    this.countriesFiltered = this.parentForm.get('country').valueChanges
      .pipe(
        startWith(''),
        map((countryName: string) => this._filterCountries(countryName))
      );
  }

  private _filterCountries(countryName: string): Country[] {
    const filterValue = countryName.toLowerCase();
    return this.countries.filter(country => this.displayCountryWithFlag(country.name).toLowerCase().includes(filterValue));
  }

  displayCountryWithFlag(countryName: string): string {
    const country = this.countries.find(c => c.name === countryName);
    if (!country) {
      return '';
    }
    return country ? `${country.flag} ${country.name}` : '';
  }

  onCountryChange(select: MatAutocompleteSelectedEvent) {
    // Get the selected country and check if the phone number is empty
    // If it is, set the country code to the selected country
    const countryName = select.option.value;
    const country = this.countries.find(c => c.name === countryName);
    if (country && this.phoneInput) {
      if (!this.phoneInput.phoneFormGroup.get('phoneNumber').value) {
        this.phoneInput.phoneFormGroup.get('country').setValue(country.iso2);
      }
    }
  }
}
