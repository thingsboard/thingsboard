// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnInit } from '@angular/core';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { getItemTypeIcon, ItemType } from '@shared/models/iot-hub/iot-hub-item.models';

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
    const item = this.item!;
    return item.image ? this.iotHubApiService.resolveResourceUrl(item.image) : null;
  }

  getCompactIcon(): string {
    const item = this.item!;
    return item.icon || getItemTypeIcon(item.type);
  }

  getTypeIcon(): string {
    return getItemTypeIcon(this.item!.type);
  }

  getCompactColor(): string {
    return this.item!.color || '#048ad3';
  }

  getHref(): string {
    return `/iot-hub/${this.itemId}`;
  }
}
