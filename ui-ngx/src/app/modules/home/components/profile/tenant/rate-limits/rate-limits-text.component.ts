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
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { RateLimits, rateLimitsArrayToHtml } from './rate-limits.models';

@Component({
  selector: 'tb-rate-limits-text',
  templateUrl: './rate-limits-text.component.html',
  styleUrls: ['./rate-limits-text.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RateLimitsTextComponent),
      multi: true
    }
  ]
})
export class RateLimitsTextComponent implements ControlValueAccessor, OnInit {

  @Input()
  disabled: boolean;

  noRateLimitsText = this.translate.instant('tenant-profile.rate-limits.not-set');

  public rateLimitsText: string;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private fb: FormBuilder,
              private translate: TranslateService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Array<RateLimits>): void {
    this.updateText(value);
  }

  private updateText(value: Array<RateLimits>) {
    if (value && value.length) {
      this.rateLimitsText = rateLimitsArrayToHtml(this.translate, value);
    } else {
      this.rateLimitsText = this.noRateLimitsText;
    }
  }

}
