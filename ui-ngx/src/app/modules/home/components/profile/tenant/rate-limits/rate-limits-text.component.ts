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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RateLimits, rateLimitsArrayToHtml } from './rate-limits.models';

@Component({
  selector: 'tb-rate-limits-text',
  templateUrl: './rate-limits-text.component.html',
  styleUrls: ['./rate-limits-text.component.scss']
})
export class RateLimitsTextComponent implements OnChanges {

  @Input()
  rateLimitsArray: Array<RateLimits>;

  @Input()
  disabled: boolean;

  rateLimitsText: string;

  constructor(private translate: TranslateService) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      if (propName === 'rateLimitsArray') {
        const change = changes[propName];
        this.updateView(change.currentValue);
      }
    }
  }

  private updateView(value: Array<RateLimits>): void {
    if (value?.length) {
      this.rateLimitsText = rateLimitsArrayToHtml(this.translate, value);
    } else {
      this.rateLimitsText = this.translate.instant('tenant-profile.rate-limits.not-set');
    }
  }
}
