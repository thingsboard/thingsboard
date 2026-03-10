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
import { MpItemVersionView, cfTypeTranslations, cfTypeIcons, ruleChainTypeTranslations, widgetTypeTranslations, NodeInfo, nodeComponentTypeTranslations } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations, getCategoriesForType, useCaseTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
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
  readonly maxVisibleCategories = 3;
  readonly maxVisibleNodes = 2;

  @Input() item: MpItemVersionView;
  @Input() showCreator = true;
  @Input() showTypeChip = true;
  @Input() installed = false;
  @Output() cardClick = new EventEmitter<MpItemVersionView>();
  @Output() creatorClick = new EventEmitter<string>();
  @Output() installClick = new EventEmitter<MpItemVersionView>();

  typeTranslations = itemTypeTranslations;

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

  getPreviewClass(): string {
    switch (this.item.type) {
      case ItemType.WIDGET: return 'preview-widget';
      case ItemType.DASHBOARD: return 'preview-dashboard';
      case ItemType.CALCULATED_FIELD: return 'preview-calculated-field';
      case ItemType.RULE_CHAIN: return 'preview-rule-chain';
      case ItemType.DEVICE: return 'preview-device';
      default: return '';
    }
  }

  getPlaceholderIcon(): string {
    switch (this.item.type) {
      case ItemType.WIDGET: return 'widgets';
      case ItemType.DASHBOARD: return 'dashboard';
      case ItemType.CALCULATED_FIELD:
        return this.item.icon || cfTypeIcons.get(this.item.dataDescriptor?.cfType) || 'functions';
      case ItemType.RULE_CHAIN:
        return this.item.icon || (this.item.dataDescriptor?.ruleChainType === 'EDGE' ? 'router' : 'device_hub');
      case ItemType.DEVICE: return 'memory';
      default: return 'extension';
    }
  }

  getCustomIconColor(): string | null {
    if ((this.item.type === ItemType.CALCULATED_FIELD || this.item.type === ItemType.RULE_CHAIN) && this.item.color) {
      return this.item.color;
    }
    return null;
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

  getSubtypeColorClass(): string {
    switch (this.item.type) {
      case ItemType.WIDGET:
        return 'wt-' + (this.item.dataDescriptor?.widgetType || 'static');
      case ItemType.CALCULATED_FIELD:
        switch (this.item.dataDescriptor?.cfType) {
          case 'SIMPLE': return 'cf-simple';
          case 'SCRIPT': return 'cf-script';
          case 'GEOFENCING': return 'cf-geofencing';
          case 'ALARM': return 'cf-alarm';
          case 'PROPAGATION': return 'cf-propagation';
          case 'RELATED_ENTITIES_AGGREGATION': return 'cf-related-agg';
          case 'ENTITY_AGGREGATION': return 'cf-entity-agg';
          default: return 'cf-simple';
        }
      case ItemType.RULE_CHAIN:
        return this.item.dataDescriptor?.ruleChainType === 'EDGE' ? 'rc-edge' : 'rc-core';
      default:
        return '';
    }
  }

  getCategoryLabels(): string[] {
    if (!this.item.categories?.length) {
      return [];
    }
    const categoryMap = getCategoriesForType(this.item.type);
    return this.item.categories.map(c => {
      const key = categoryMap.get(c);
      return key ? this.translate.instant(key) : c;
    });
  }

  getFirstUseCaseLabel(): string | null {
    if (!this.item.useCases?.length) {
      return null;
    }
    const key = useCaseTranslations.get(this.item.useCases[0] as any);
    return key ? this.translate.instant(key) : this.item.useCases[0];
  }

  getNodes(): NodeInfo[] {
    if (this.item.type !== ItemType.RULE_CHAIN) {
      return [];
    }
    return this.item.dataDescriptor?.nodes || [];
  }

  getNodeCount(): number {
    return this.item.dataDescriptor?.nodeCount || 0;
  }

  getNodeLabel(node: NodeInfo): string {
    const key = nodeComponentTypeTranslations.get(node.type);
    return key ? this.translate.instant(key) : node.type;
  }

  getNodeColorClass(node: NodeInfo): string {
    switch (node.type) {
      case 'ENRICHMENT': return 'node-enrichment';
      case 'FILTER': return 'node-filter';
      case 'TRANSFORMATION': return 'node-transformation';
      case 'ACTION': return 'node-action';
      case 'ANALYTICS': return 'node-analytics';
      case 'EXTERNAL': return 'node-external';
      case 'FLOW': return 'node-flow';
      default: return 'node-unknown';
    }
  }

  onCardClick(): void {
    this.cardClick.emit(this.item);
  }

  onInstallClick(event: MouseEvent): void {
    event.stopPropagation();
    this.installClick.emit(this.item);
  }

  onCreatorClick(event: MouseEvent): void {
    event.stopPropagation();
    this.creatorClick.emit(this.item.creatorId);
  }

  formatPublishedTime(timestamp: number): string {
    const date = new Date(timestamp);
    return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
  }

}
