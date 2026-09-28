// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { FormControl, FormGroup, Validators } from "@angular/forms";
import { validateEmail } from "@app/core/utils";
import { passwordsMatchValidator } from '@shared/models/password.models';

export interface SignupRequestValues {
  fields: SignupFieldsValues;
  recaptchaResponse: string;
}

interface SignupFieldsValues {
  EMAIL: string;
  FIRST_NAME: string;
  LAST_NAME: string;
  PASSWORD: string;
  CHECK_PASSWORD: string;
}

export class SignupRequest {
  fields: FormGroup;
  recaptchaResponse: string;

  constructor(firstName: string, lastName: string, email: string, password: string, recaptchaResponse: string) {
    this.fields = new FormGroup({
      FIRST_NAME: new FormControl(firstName, [Validators.required, Validators.maxLength(256)]),
      LAST_NAME: new FormControl(lastName, [Validators.required, Validators.maxLength(256)]),
      EMAIL: new FormControl(email, [validateEmail]),
      PASSWORD: new FormControl(password),
      CHECK_PASSWORD: new FormControl(password)
    }, {
      validators: [passwordsMatchValidator('PASSWORD', 'CHECK_PASSWORD')]
    });
    this.recaptchaResponse = recaptchaResponse;
  }

  public static create(): SignupRequest {
    return new SignupRequest('', '', '', '', '');
  }
}

export enum SignUpResult {
  SUCCESS = 'SUCCESS',
  INACTIVE_USER_EXISTS = 'INACTIVE_USER_EXISTS'
}
