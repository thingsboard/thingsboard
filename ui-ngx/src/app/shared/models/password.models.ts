// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
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

export interface PasswordErrorRule {
  key: string;
  translation: TooltipPasswordErrorMessageKey;
  applies: (policy: UserPasswordPolicy) => boolean;
}

export const passwordErrorRules: PasswordErrorRule[] = [
  { key: 'minLength', translation: TooltipPasswordErrorMessageKey.minLength, applies: policy => policy?.minimumLength > 0 },
  { key: 'notUpperCase', translation: TooltipPasswordErrorMessageKey.notUpperCase, applies: policy => policy?.minimumUppercaseLetters > 0 },
  { key: 'notLowerCase', translation: TooltipPasswordErrorMessageKey.notLowerCase, applies: policy => policy?.minimumLowercaseLetters > 0 },
  { key: 'notNumeric', translation: TooltipPasswordErrorMessageKey.notNumeric, applies: policy => policy?.minimumDigits > 0 },
  { key: 'notSpecial', translation: TooltipPasswordErrorMessageKey.notSpecial, applies: policy => policy?.minimumSpecialCharacters > 0 },
  { key: 'maxLength', translation: TooltipPasswordErrorMessageKey.maxLength, applies: policy => policy?.maximumLength > 0 },
  { key: 'hasWhitespaces', translation: TooltipPasswordErrorMessageKey.hasWhitespaces, applies: policy => policy?.allowWhitespaces === false },
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
