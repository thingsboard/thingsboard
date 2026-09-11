// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RateLimits, rateLimitsArrayToHtml } from './rate-limits.models';

@Component({
    selector: 'tb-rate-limits-text',
    templateUrl: './rate-limits-text.component.html',
    styleUrls: ['./rate-limits-text.component.scss'],
    standalone: false
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
