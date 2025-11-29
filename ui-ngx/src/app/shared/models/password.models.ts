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

import { UserPasswordPolicy } from '@shared/models/settings.models';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { isEqual } from '@core/utils';

export enum TooltipPasswordErrorMessageKey {
  minLength = 'security.password-requirement.password-tooltip-min-length',
  maxLength = 'security.password-requirement.password-tooltip-max-length',
  notUpperCase = 'security.password-requirement.password-tooltip-uppercase',
  notLowerCase = 'security.password-requirement.password-tooltip-lowercase',
  notNumeric = 'security.password-requirement.password-tooltip-digit',
  notSpecial = 'security.password-requirement.password-tooltip-special-characters',
  hasWhitespaces = 'security.password-requirement.password-should-not-contain-spaces'
}

export const passwordErrorRules = [
  { key: 'minLength', policyProp: 'minimumLength', translation: TooltipPasswordErrorMessageKey.minLength },
  { key: 'notUpperCase', policyProp: 'minimumUppercaseLetters', translation: TooltipPasswordErrorMessageKey.notUpperCase },
  { key: 'notLowerCase', policyProp: 'minimumLowercaseLetters', translation: TooltipPasswordErrorMessageKey.notLowerCase },
  { key: 'notNumeric', policyProp: 'minimumDigits', translation: TooltipPasswordErrorMessageKey.notNumeric },
  { key: 'notSpecial', policyProp: 'minimumSpecialCharacters', translation: TooltipPasswordErrorMessageKey.notSpecial },
  { key: 'maxLength', policyProp: 'maximumLength', translation: TooltipPasswordErrorMessageKey.maxLength },
  { key: 'hasWhitespaces', policyProp: 'hasWhitespaces', translation: TooltipPasswordErrorMessageKey.hasWhitespaces },
];

export const passwordsMatchValidator = (firstControlName: string, secondControlName: string): ValidatorFn =>{
  return (group: AbstractControl): ValidationErrors | null => {
    const newPassControl = group.get(firstControlName);
    const confirmControl = group.get(secondControlName);

    if (!newPassControl || !confirmControl) {
      return null;
    }

    const newPass = newPassControl.value ?? '';
    const confirm = confirmControl.value ?? '';

    if ((newPass || confirm) && confirm !== newPass) {
      confirmControl.setErrors({ passwordsNotMatch: true });
      return { passwordsNotMatch: true };
    } else {
      const currentErrors = confirmControl?.errors;
      if (currentErrors?.['passwordsNotMatch']) {
        const { passwordsNotMatch, ...rest } = currentErrors;
        confirmControl?.setErrors(Object.keys(rest).length ? rest : null);
      }
      return null;
    }
  };
}

export const passwordStrengthValidator = (passwordPolicy: UserPasswordPolicy): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = control.value;
    const errors: any = {};

    if (passwordPolicy.minimumUppercaseLetters > 0 &&
      !new RegExp(`(?:.*?[A-Z]){${passwordPolicy.minimumUppercaseLetters}}`).test(value)) {
      errors.notUpperCase = true;
    }

    if (passwordPolicy.minimumLowercaseLetters > 0 &&
      !new RegExp(`(?:.*?[a-z]){${passwordPolicy.minimumLowercaseLetters}}`).test(value)) {
      errors.notLowerCase = true;
    }

    if (passwordPolicy.minimumDigits > 0
      && !new RegExp(`(?:.*?\\d){${passwordPolicy.minimumDigits}}`).test(value)) {
      errors.notNumeric = true;
    }

    if (passwordPolicy.minimumSpecialCharacters > 0 &&
      !new RegExp(`(?:.*?[\\W_]){${passwordPolicy.minimumSpecialCharacters}}`).test(value)) {
      errors.notSpecial = true;
    }

    if (!passwordPolicy.allowWhitespaces && /\s/.test(value)) {
      errors.hasWhitespaces = true;
    }

    if (passwordPolicy.minimumLength > 0 && value.length < passwordPolicy.minimumLength) {
      errors.minLength = true;
    }

    if (!value.length || passwordPolicy.maximumLength > 0 && value.length > passwordPolicy.maximumLength) {
      errors.maxLength = true;
    }

    return isEqual(errors, {}) ? null : errors;
  };
}
