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

import { booleanAttribute, Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  CustomMobilePage,
  MobilePageType,
  mobilePageTypeTranslations,
  WEB_URL_REGEX
} from '@shared/models/mobile-app.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-mobile-page-item',
    templateUrl: './custom-mobile-page.component.html',
    styleUrls: ['./custom-mobile-page.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CustomMobilePageComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CustomMobilePageComponent),
            multi: true
        }
    ],
    standalone: false
})
export class CustomMobilePageComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  mobilePagesTypes = [MobilePageType.DASHBOARD, MobilePageType.WEB_VIEW, MobilePageType.CUSTOM];
  MobilePageType = MobilePageType;
  mobilePageTypeTranslations = mobilePageTypeTranslations;

  customMobilePageForm = this.fb.group({
    visible: [true],
    icon: ['star'],
    label: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
    type: [MobilePageType.DASHBOARD],
    dashboardId: this.fb.control<string>(null, Validators.required),
    url: [{value:'', disabled: true}, [Validators.required, Validators.pattern(WEB_URL_REGEX)]],
    path: [{value:'', disabled: true}, [Validators.required, Validators.pattern(/^(\/[\w\-._~:/?#[\]@!$&'()*+,;=%]*)?$/)]]
  });

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private store: Store<AppState>) {
    this.customMobilePageForm.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(type => {
      this.customMobilePageForm.get('dashboardId').disable({emitEvent: false});
      this.customMobilePageForm.get('url').disable({emitEvent: false});
      this.customMobilePageForm.get('path').disable({emitEvent: false});
      switch (type) {
        case MobilePageType.DASHBOARD:
          this.customMobilePageForm.get('dashboardId').enable({emitEvent: false});
          break;
        case MobilePageType.WEB_VIEW:
          this.customMobilePageForm.get('url').enable({emitEvent: false});
          break;
        case MobilePageType.CUSTOM:
          this.customMobilePageForm.get('path').enable({emitEvent: false});
          break;
      }
    });

    if (getCurrentAuthUser(this.store).authority === Authority.SYS_ADMIN) {
      this.mobilePagesTypes.shift();
      this.customMobilePageForm.get('type').setValue(MobilePageType.WEB_VIEW);
    }

    this.customMobilePageForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => this.propagateChange(value))
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.customMobilePageForm.disable({emitEvent: false});
    } else {
      this.customMobilePageForm.enable({emitEvent: false});
      this.customMobilePageForm.get('type').updateValueAndValidity({onlySelf: true});
    }
  }

  validate(): ValidationErrors | null {
    if (!this.customMobilePageForm.valid) {
      return {
        invalidCustomMobilePageForm: true
      };
    }
    return null;
  }

  writeValue(value: CustomMobilePage) {
    this.customMobilePageForm.patchValue(value, {emitEvent: false});
  }
}
