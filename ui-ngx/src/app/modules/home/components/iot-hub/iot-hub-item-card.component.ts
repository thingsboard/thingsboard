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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MpItemVersionView, cfTypeTranslations, cfTypeIcons, ruleChainTypeTranslations, widgetTypeTranslations } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { TranslateService } from '@ngx-translate/core';
import { IotHubApiService } from '@core/http/iot-hub-api.service';

@Component({
  selector: 'tb-iot-hub-item-card',
  standalone: false,
  templateUrl: './iot-hub-item-card.component.html',
  styleUrls: ['./iot-hub-item-card.component.scss']
})
export class TbIotHubItemCardComponent {

  readonly ItemType = ItemType;

  @Input() item: MpItemVersionView;
  @Input() installedItem: IotHubInstalledItem;
  @Input() installedItemsCount = 0;
  @Input() showCreator = true;
  @Input() showTypeChip = true;
  @Input() showSubtype = false;
  @Input() mode: 'default' | 'add' = 'default';
  @Output() cardClick = new EventEmitter<MpItemVersionView>();
  @Output() creatorClick = new EventEmitter<string>();
  @Output() installClick = new EventEmitter<MpItemVersionView>();
  @Output() updateClick = new EventEmitter<MpItemVersionView>();
  @Output() deleteClick = new EventEmitter<MpItemVersionView>();
  @Output() addClick = new EventEmitter<MpItemVersionView>();
  @Output() guideClick = new EventEmitter<MpItemVersionView>();

  constructor(
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService
  ) {}

  isCompactLayout(): boolean {
    return this.item.type === ItemType.CALCULATED_FIELD || this.item.type === ItemType.RULE_CHAIN;
  }

  getPreviewUrl(): string | null {
    if (!this.item.image) {
      return null;
    }
    const url = this.item.image.split('?')[0];
    const resolved = url.endsWith('/preview') ? this.item.image : `${url}/preview`;
    return this.iotHubApiService.resolveResourceUrl(resolved);
  }

  getPlaceholderIcon(): string {
    switch (this.item.type) {
      case ItemType.WIDGET: return 'widgets';
      case ItemType.DASHBOARD: return 'dashboard';
      case ItemType.SOLUTION_TEMPLATE: return 'integration_instructions';
      case ItemType.CALCULATED_FIELD:
        return this.item.icon || cfTypeIcons.get(this.item.dataDescriptor?.cfType) || 'functions';
      case ItemType.RULE_CHAIN:
        return this.item.icon || (this.item.dataDescriptor?.ruleChainType === 'EDGE' ? 'router' : 'device_hub');
      case ItemType.DEVICE: return 'memory';
      default: return 'extension';
    }
  }

  getSubtypeLabel(): string {
    switch (this.item.type) {
      case ItemType.WIDGET: {
        const wt = this.item.dataDescriptor?.widgetType;
        const key = wt ? widgetTypeTranslations.get(wt) : null;
        return key ? this.translate.instant(key) : wt || '';
      }
      case ItemType.CALCULATED_FIELD: {
        const cfType = this.item.dataDescriptor?.cfType;
        const key = cfType ? cfTypeTranslations.get(cfType) : null;
        return key ? this.translate.instant(key) : cfType || '';
      }
      case ItemType.RULE_CHAIN: {
        const rcType = this.item.dataDescriptor?.ruleChainType;
        const key = rcType ? ruleChainTypeTranslations.get(rcType) : null;
        return key ? this.translate.instant(key) : rcType || '';
      }
      default:
        return '';
    }
  }

  isInstalled(): boolean {
    return !!this.installedItem;
  }

  isSameVersion(): boolean {
    return this.installedItem?.itemVersionId === this.item.id;
  }

  onCardClick(): void {
    this.cardClick.emit(this.item);
  }

  onInstallClick(event: MouseEvent): void {
    event.stopPropagation();
    this.installClick.emit(this.item);
  }

  onAddClick(event: MouseEvent): void {
    event.stopPropagation();
    this.addClick.emit(this.item);
  }

  onUpdateClick(event: MouseEvent): void {
    event.stopPropagation();
    this.updateClick.emit(this.item);
  }

  onDeleteClick(event: MouseEvent): void {
    event.stopPropagation();
    this.deleteClick.emit(this.item);
  }

  hasInstallGuide(): boolean {
    return !!this.installedItem && this.installedItem.itemType === 'DEVICE';
  }

  onGuideClick(event: MouseEvent): void {
    event.stopPropagation();
    this.guideClick.emit(this.item);
  }

  onCreatorClick(event: Event): void {
    event.stopPropagation();
    this.creatorClick.emit(this.item.creatorId);
  }

}
