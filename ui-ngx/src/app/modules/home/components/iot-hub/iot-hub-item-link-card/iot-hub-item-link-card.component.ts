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

import { Component, Input, OnInit } from '@angular/core';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';

type CardState = 'loading' | 'loaded' | 'unavailable';

const COMPACT_TYPES: ReadonlySet<ItemType> = new Set([
  ItemType.CALCULATED_FIELD,
  ItemType.ALARM_RULE,
  ItemType.RULE_CHAIN
]);

@Component({
  selector: 'tb-iot-hub-item-link-card',
  standalone: false,
  templateUrl: './iot-hub-item-link-card.component.html',
  styleUrls: ['./iot-hub-item-link-card.component.scss']
})
export class TbIotHubItemLinkCardComponent implements OnInit {

  @Input() itemId!: string;

  state: CardState = 'loading';
  item: MpItemVersionView | null = null;

  constructor(private iotHubApiService: IotHubApiService) {}

  ngOnInit(): void {
    if (!this.itemId) {
      this.state = 'unavailable';
      return;
    }
    this.iotHubApiService
      .getPublishedVersion(this.itemId, { ignoreErrors: true, ignoreLoading: true })
      .subscribe({
        next: item => {
          this.item = item;
          this.state = item ? 'loaded' : 'unavailable';
        },
        error: () => {
          this.state = 'unavailable';
        }
      });
  }

  isCompact(): boolean {
    return !!this.item && COMPACT_TYPES.has(this.item.type);
  }

  getImageUrl(): string | null {
    return this.item?.image
      ? this.iotHubApiService.resolveResourceUrl(this.item.image)
      : null;
  }

  getCompactIcon(): string {
    if (!this.item) {
      return 'category';
    }
    if (this.item.icon) {
      return this.item.icon;
    }
    switch (this.item.type) {
      case ItemType.CALCULATED_FIELD: return 'functions';
      case ItemType.ALARM_RULE: return 'notification_important';
      case ItemType.RULE_CHAIN: return 'account_tree';
      default: return 'category';
    }
  }

  getTypeIcon(): string {
    if (!this.item) {
      return 'category';
    }
    switch (this.item.type) {
      case ItemType.WIDGET: return 'widgets';
      case ItemType.DASHBOARD: return 'dashboard';
      case ItemType.SOLUTION_TEMPLATE: return 'integration_instructions';
      case ItemType.CALCULATED_FIELD: return 'functions';
      case ItemType.ALARM_RULE: return 'notification_important';
      case ItemType.RULE_CHAIN: return 'account_tree';
      case ItemType.DEVICE: return 'memory';
      default: return 'category';
    }
  }

  getCompactColor(): string {
    return this.item?.color || '#048ad3';
  }

  getHref(): string {
    return `/iot-hub/${this.itemId}`;
  }
}
