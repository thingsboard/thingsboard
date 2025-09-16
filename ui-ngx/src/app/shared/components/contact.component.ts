///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, Input, OnInit } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { UtilsService } from "@core/services/utils.service";

@Component({
  selector: 'tb-contact',
  templateUrl: './contact.component.html'
})
export class ContactComponent implements OnInit {

  @Input()
  parentForm: UntypedFormGroup;

  @Input() isEdit: boolean;

  phoneInputDefaultCountry = 'US';

  constructor(private utils: UtilsService) {
  }
  
  ngOnInit() {
    this.parentForm.controls['email'].addValidators(this.utils.validateEmail)
  }

  changeCountry(countryCode: string) {
    this.phoneInputDefaultCountry = countryCode ?? 'US';
    setTimeout(() => {
      this.parentForm.get('phone').setValue(this.parentForm.get('phone').value);
    });
  }
}
