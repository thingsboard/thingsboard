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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { MpItemVersionView, cfTypeTranslations, cfTypeIcons, ruleChainTypeTranslations, widgetTypeTranslations, nodeComponentTypeTranslations, NodeInfo } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations, getCategoriesForType, useCaseTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItemDescriptor, IotHubInstalledItemInfo } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { TbIotHubInstallDialogComponent, IotHubInstallDialogData } from './iot-hub-install-dialog.component';
import { TbIotHubUpdateDialogComponent, IotHubUpdateDialogData } from './iot-hub-update-dialog.component';

export interface IotHubItemDetailDialogData {
  item: MpItemVersionView;
  iotHubApiService: IotHubApiService;
  installedDescriptor?: IotHubInstalledItemDescriptor;
  installedItemInfo?: IotHubInstalledItemInfo;
}

@Component({
  selector: 'tb-iot-hub-item-detail-dialog',
  standalone: false,
  templateUrl: './iot-hub-item-detail-dialog.component.html',
  styleUrls: ['./iot-hub-item-detail-dialog.component.scss']
})
export class TbIotHubItemDetailDialogComponent {

  readonly ItemType = ItemType;
  item: MpItemVersionView;
  typeTranslations = itemTypeTranslations;
  readmeContent: string = '';
  installedDescriptor?: IotHubInstalledItemDescriptor;
  installedItemInfo?: IotHubInstalledItemInfo;

  private static readonly ITEM_TYPE_TO_ENTITY_TYPE: Record<string, EntityType> = {
    'WIDGET': EntityType.WIDGET_TYPE,
    'DASHBOARD': EntityType.DASHBOARD,
    'CALCULATED_FIELD': EntityType.CALCULATED_FIELD,
    'RULE_CHAIN': EntityType.RULE_CHAIN,
    'DEVICE': EntityType.DEVICE_PROFILE
  };

  private categoryMap: Map<string, string>;
  private useCaseMap = useCaseTranslations;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: IotHubItemDetailDialogData,
    private dialogRef: MatDialogRef<TbIotHubItemDetailDialogComponent>,
    private dialog: MatDialog,
    private router: Router,
    private translate: TranslateService
  ) {
    this.item = data.item;
    this.installedDescriptor = data.installedDescriptor;
    this.installedItemInfo = data.installedItemInfo;
    if (!this.installedDescriptor && data.installedItemInfo) {
      this.data.iotHubApiService.getInstalledItemByItemId(data.installedItemInfo.itemId, {ignoreLoading: true}).subscribe(
        installedItem => this.installedDescriptor = installedItem?.descriptor
      );
    }
    this.categoryMap = getCategoriesForType(this.item.type);
    this.loadReadme();
  }

  getCategoryLabel(key: string): string {
    const translationKey = this.categoryMap.get(key);
    return translationKey ? this.translate.instant(translationKey) : key;
  }

  getUseCaseLabel(key: string): string {
    const translationKey = this.useCaseMap.get(key as any);
    return translationKey ? this.translate.instant(translationKey) : key;
  }

  getPreviewUrl(): string | null {
    return this.item.image ? this.data.iotHubApiService.resolveResourceUrl(this.item.image) : null;
  }

  getTypeChipClass(): string {
    switch (this.item.type) {
      case ItemType.WIDGET: return 'type-widget';
      case ItemType.DASHBOARD: return 'type-dashboard';
      case ItemType.CALCULATED_FIELD: return 'type-calc-field';
      case ItemType.RULE_CHAIN: return 'type-rule-chain';
      case ItemType.DEVICE: return 'type-device';
      default: return '';
    }
  }

  getTypeLabel(): string {
    const key = this.typeTranslations.get(this.item.type);
    return key ? this.translate.instant(key) : '';
  }

  getTypeIcon(): string {
    switch (this.item.type) {
      case ItemType.WIDGET: return 'widgets';
      case ItemType.DASHBOARD: return 'dashboard';
      case ItemType.CALCULATED_FIELD: return 'functions';
      case ItemType.RULE_CHAIN: return 'account_tree';
      case ItemType.DEVICE: return 'memory';
      default: return 'category';
    }
  }

  getCompactIcon(): string {
    if (this.item.icon) {
      return this.item.icon;
    }
    if (this.item.type === ItemType.CALCULATED_FIELD) {
      const cfType = this.item.dataDescriptor?.cfType;
      return cfTypeIcons.get(cfType) || 'functions';
    }
    switch (this.item.dataDescriptor?.ruleChainType) {
      case 'CORE': return 'device_hub';
      case 'EDGE': return 'router';
      default: return 'account_tree';
    }
  }

  getCustomColor(): string | null {
    return this.item.color || null;
  }

  getCompactIconColorClass(): string {
    if (this.item.color) {
      return '';
    }
    if (this.item.type === ItemType.CALCULATED_FIELD) {
      switch (this.item.dataDescriptor?.cfType) {
        case 'SIMPLE': return 'cf-simple';
        case 'SCRIPT': return 'cf-script';
        case 'GEOFENCING': return 'cf-geofencing';
        case 'ALARM': return 'cf-alarm';
        case 'PROPAGATION': return 'cf-propagation';
        case 'RELATED_ENTITIES_AGGREGATION': return 'cf-related-agg';
        case 'ENTITY_AGGREGATION': return 'cf-entity-agg';
        default: return 'cf-entity-agg';
      }
    }
    switch (this.item.dataDescriptor?.ruleChainType) {
      case 'CORE': return 'rc-core';
      case 'EDGE': return 'rc-edge';
      default: return 'rc-core';
    }
  }

  getCompactSubtypeLabel(): string {
    if (this.item.type === ItemType.CALCULATED_FIELD) {
      const cfType = this.item.dataDescriptor?.cfType;
      const key = cfType ? cfTypeTranslations.get(cfType) : null;
      return key ? this.translate.instant(key) : cfType || '';
    }
    const rcType = this.item.dataDescriptor?.ruleChainType;
    const key = rcType ? ruleChainTypeTranslations.get(rcType) : null;
    return key ? this.translate.instant(key) : rcType || '';
  }

  getSubtypeLabel(): string {
    switch (this.item.type) {
      case ItemType.CALCULATED_FIELD:
        return this.getCompactSubtypeLabel();
      case ItemType.RULE_CHAIN:
        return this.getCompactSubtypeLabel();
      case ItemType.WIDGET: {
        const wt = this.item.dataDescriptor?.widgetType;
        const key = wt ? widgetTypeTranslations.get(wt) : null;
        return key ? this.translate.instant(key) : '';
      }
      default:
        return '';
    }
  }

  getSubtypeColorClass(): string {
    switch (this.item.type) {
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
      case ItemType.WIDGET:
        switch (this.item.dataDescriptor?.widgetType) {
          case 'timeseries': return 'wt-timeseries';
          case 'latest': return 'wt-latest';
          case 'rpc': return 'wt-rpc';
          case 'alarm': return 'wt-alarm';
          case 'static': return 'wt-static';
          default: return 'wt-static';
        }
      default:
        return '';
    }
  }

  getNodeLabel(node: NodeInfo): string {
    const key = nodeComponentTypeTranslations.get(node.type);
    const typeLabel = key ? this.translate.instant(key) : node.type;
    return `${node.name} (${typeLabel})`;
  }

  getNodes(): NodeInfo[] {
    return this.item.dataDescriptor?.nodes || [];
  }

  getNodeCount(): number {
    return this.item.dataDescriptor?.nodeCount || 0;
  }

  hasUpdate(): boolean {
    return this.installedItemInfo != null
      && this.installedItemInfo.itemVersionId !== this.item.id;
  }

  install(): void {
    const dialogRef = this.dialog.open(TbIotHubInstallDialogComponent, {
      panelClass: ['tb-dialog'],
      data: {
        item: this.item,
        iotHubApiService: this.data.iotHubApiService
      } as IotHubInstallDialogData
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === 'installed') {
        this.dialogRef.close('installed');
      }
    });
  }

  updateItem(): void {
    const dialogRef = this.dialog.open(TbIotHubUpdateDialogComponent, {
      panelClass: ['tb-dialog'],
      data: {
        itemId: this.item.itemId,
        itemName: this.item.name,
        itemType: this.item.type,
        version: this.item.version,
        versionId: this.item.id,
        iotHubApiService: this.data.iotHubApiService
      } as IotHubUpdateDialogData
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === 'updated') {
        this.dialogRef.close('updated');
      }
    });
  }

  openSignup(): void {
    window.open('https://iothub.thingsboard.io/signup', '_blank');
  }

  navigateToCreator(): void {
    this.dialogRef.close();
    this.router.navigate(['/iot-hub/creator', this.item.creatorId]);
  }

  openEntityDetails(): void {
    const descriptor = this.installedDescriptor;
    if (!descriptor) {
      return;
    }
    let entityId: string | null = null;
    switch (descriptor.type) {
      case 'WIDGET': entityId = descriptor.widgetTypeId?.id; break;
      case 'DASHBOARD': entityId = descriptor.dashboardId?.id; break;
      case 'CALCULATED_FIELD': entityId = descriptor.calculatedFieldId?.id; break;
      case 'RULE_CHAIN': entityId = descriptor.ruleChainId?.id; break;
    }
    const entityType = TbIotHubItemDetailDialogComponent.ITEM_TYPE_TO_ENTITY_TYPE[this.item.type];
    if (entityType && entityId) {
      const url = getEntityDetailsPageURL(entityId, entityType);
      if (url) {
        this.dialogRef.close();
        this.router.navigateByUrl(url);
      }
    }
  }

  close(): void {
    this.dialogRef.close();
  }

  private loadReadme(): void {
    const versionId = this.item.id as string;
    this.data.iotHubApiService.getVersionReadme(versionId, { ignoreLoading: true }).subscribe(
      content => this.readmeContent = this.prefixResourceUrls(content || '')
    );
  }

  private prefixResourceUrls(markdown: string): string {
    const baseUrl = this.data.iotHubApiService.baseUrl;
    return markdown.replace(/(\(|")(\/api\/resources\/[^)"]*)/g, `$1${baseUrl}$2`);
  }
}
