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

import { Component, Inject, Type } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { MpItemVersionView, cfTypeTranslations, cfTypeIcons, ruleChainTypeTranslations, widgetTypeTranslations, NodeInfo } from '@shared/models/iot-hub/iot-hub-version.models';
import { getItemTypeIcon, ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { getInstalledItemUrl, IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { SolutionInstallDialogComponent } from '@home/components/iot-hub/solution-install-dialog.component';
import { SolutionTemplateInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';

export type IotHubItemDetailDialogMode = 'default' | 'add';

export interface IotHubItemDetailDialogData {
  item: MpItemVersionView;
  installedItem?: IotHubInstalledItem;
  installedItemsCount?: number;
  mode?: IotHubItemDetailDialogMode;
  showCreator?: boolean;
  preview?: boolean;
}

@Component({
  selector: 'tb-iot-hub-item-detail-dialog',
  standalone: false,
  templateUrl: './iot-hub-item-detail-dialog.component.html',
  styleUrls: ['./iot-hub-item-detail-dialog.component.scss']
})
export class TbIotHubItemDetailDialogComponent extends DialogComponent<TbIotHubItemDetailDialogComponent> {

  readonly ItemType = ItemType;
  item: MpItemVersionView;
  mode: IotHubItemDetailDialogMode;
  showCreator: boolean;
  preview: boolean;
  typeTranslations = itemTypeTranslations;
  readmeContent: string = '';
  installedItem?: IotHubInstalledItem;
  installedItemsCount = 0;
  carouselImages: string[] = [];
  carouselIndex = 0;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubItemDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubItemDetailDialogData,
    private dialog: MatDialog,
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService,
    private iotHubActions: IotHubActionsService
  ) {
    super(store, router, dialogRef);
    this.item = data.item;
    this.mode = data.mode || 'default';
    this.showCreator = data.showCreator !== false;
    this.preview = data.preview === true;
    this.installedItem = data.installedItem;
    this.installedItemsCount = data.installedItemsCount || 0;
    this.buildCarouselImages();
    this.loadReadme();
  }

  isCompactLayout(): boolean {
    return this.item.type === ItemType.CALCULATED_FIELD
        || this.item.type === ItemType.ALARM_RULE
        || this.item.type === ItemType.RULE_CHAIN;
  }

  getPreviewUrl(): string | null {
    return this.item.image ? this.iotHubApiService.resolveResourceUrl(this.item.image) : null;
  }

  getCreatorAvatarUrl(): string | null {
    return this.item.creatorAvatarUrl ? this.iotHubApiService.resolveResourceUrl(this.item.creatorAvatarUrl) : null;
  }

  getTypeLabel(): string {
    const key = this.typeTranslations.get(this.item.type);
    return key ? this.translate.instant(key) : '';
  }

  getTypeIcon(): string {
    return getItemTypeIcon(this.item.type);
  }

  getCompactIcon(): string {
    if (this.item.icon) {
      return this.item.icon;
    }
    if (this.item.type === ItemType.CALCULATED_FIELD) {
      const cfType = this.item.dataDescriptor?.cfType;
      return cfTypeIcons.get(cfType) || getItemTypeIcon(ItemType.CALCULATED_FIELD);
    }
    if (this.item.type === ItemType.ALARM_RULE) {
      return getItemTypeIcon(ItemType.ALARM_RULE);
    }
    switch (this.item.dataDescriptor?.ruleChainType) {
      case 'CORE': return 'device_hub';
      case 'EDGE': return 'router';
      default: return getItemTypeIcon(ItemType.RULE_CHAIN);
    }
  }

  getCompactSubtypeLabel(): string {
    if (this.item.type === ItemType.CALCULATED_FIELD || this.item.type === ItemType.ALARM_RULE) {
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
      case ItemType.ALARM_RULE:
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

  getNodes(): NodeInfo[] {
    return this.item.dataDescriptor?.nodes || [];
  }

  isInstalled(): boolean {
    return this.installedItem != null;
  }

  shouldShowChangelog(): boolean {
    const changelog = this.item.changelog?.trim();
    return !!changelog && changelog !== 'Initial release';
  }

  hasUpdate(): boolean {
    return this.installedItem != null
      && this.installedItem.itemVersionId !== this.item.id;
  }

  install(): void {
    this.iotHubActions.installItem(this.item).subscribe(result => {
      if (result === 'installed') {
        this.dialogRef.close('installed');
      }
    });
  }

  installDevice(): void {
    this.iotHubActions.installDevice(this.item).subscribe(result => {
      if (result === 'installed') {
        this.dialogRef.close('installed');
      }
    });
  }

  updateItem(): void {
    this.iotHubActions.updateItem(this.installedItem, this.item.version, this.item.id as string).subscribe(result => {
      if (result === 'updated') {
        this.dialogRef.close('updated');
      }
    });
  }

  navigateToCreator(): void {
    this.dialogRef.close();
    void this.router.navigate(['/iot-hub/creator', this.item.creatorId]);
  }

  openEntityDetails(): void {
    const url = getInstalledItemUrl(this.installedItem?.descriptor);
    if (url) {
      this.dialogRef.close();
      void this.router.navigateByUrl(url);
    }
  }

  openSolutionInstructions(): void {
    const descriptor = this.installedItem?.descriptor;
    if (descriptor?.type === 'SOLUTION_TEMPLATE') {
      this.dialog.open(SolutionInstallDialogComponent, {
        disableClose: true,
        autoFocus: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          descriptor: descriptor as SolutionTemplateInstalledItemDescriptor,
          instructions: true
        }
      });
    }
  }

  deleteItem(): void {
    this.iotHubActions.deleteItem(this.installedItem).subscribe(confirmed => {
      if (confirmed) {
        this.dialogRef.close('deleted');
      }
    });
  }

  addItem(): void {
    this.dialogRef.close({ action: 'add', item: this.item });
  }

  openInstalledItemsDialog(): void {
    this.iotHubActions.openInstalledItems(this.item).subscribe();
  }

  hasDetails(): boolean {
    if (this.getSubtypeLabel()) {
      return true;
    }
    if (this.item.useCases?.length) {
      return true;
    }
    if (this.item.type === ItemType.DEVICE) {
      return !!this.item.dataDescriptor?.hardwareType || !!this.item.dataDescriptor?.connectivity?.length;
    }
    return !!this.item.categories?.length;
  }

  goToPrevSlide(): void {
    this.carouselIndex = (this.carouselIndex - 1 + this.carouselImages.length) % this.carouselImages.length;
  }

  goToNextSlide(): void {
    this.carouselIndex = (this.carouselIndex + 1) % this.carouselImages.length;
  }

  close(): void {
    this.dialogRef.close();
  }

  private buildCarouselImages(): void {
    if (this.item.type !== ItemType.SOLUTION_TEMPLATE || !this.item.resources?.length) {
      return;
    }
    const screenshotResources = this.item.resources.filter(r => r.type === 'SCREENSHOT');
    const allResources = screenshotResources.length > 0
      ? screenshotResources
      : this.item.resources.filter(r => r.type === 'ICON');
    this.carouselImages = allResources.map(r =>
      this.iotHubApiService.resolveResourceUrl(`/api/resources/${r.id}`)
    );
  }

  private loadReadme(): void {
    const versionId = this.item.id as string;
    this.iotHubApiService.getVersionReadme(versionId, { ignoreLoading: true }).subscribe(
      content => this.readmeContent = content
    );
  }

}
