// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

@Component({
    selector: 'tb-contact',
    templateUrl: './contact.component.html',
    standalone: false
})
export class ContactComponent {

  @Input()
  parentForm: UntypedFormGroup;

  @Input() isEdit: boolean;

  phoneInputDefaultCountry = 'US';

  constructor() {
  }

  changeCountry(countryCode: string) {
    this.phoneInputDefaultCountry = countryCode ?? 'US';
    setTimeout(() => {
      this.parentForm.get('phone').setValue(this.parentForm.get('phone').value);
    });
  }
}
