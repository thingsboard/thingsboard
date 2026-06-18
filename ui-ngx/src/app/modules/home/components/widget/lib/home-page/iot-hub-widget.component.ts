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
import { forkJoin } from 'rxjs';
import { PageComponent } from '@shared/components/page.component';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { resolveIotHubItemImageUrl } from '@home/components/iot-hub/iot-hub-utils';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';

const WIDGET_CARD_COUNT = 3;

@Component({
  selector: 'tb-iot-hub-widget',
  templateUrl: './iot-hub-widget.component.html',
  styleUrls: ['./home-page-widget.scss', './iot-hub-widget.component.scss'],
  standalone: false
})
export class IotHubWidgetComponent extends PageComponent implements OnInit {

  @Input()
  ctx: WidgetContext;

  authority = Authority;

  authUser = getCurrentAuthUser(this.store);

  hasIotHubAccess = true;

  solutionTemplates: MpItemVersionView[] = [];
  devices: MpItemVersionView[] = [];
  installedSolutionTemplates: IotHubInstalledItem[] = [];
  installedDeviceCounts: Record<string, number> = {};

  constructor(private iotHubApiService: IotHubApiService,
              private iotHubActions: IotHubActionsService) {
    super();
  }

  ngOnInit() {
    this.ctx.overflowVisible = true;
    this.hasIotHubAccess = [Authority.TENANT_ADMIN].includes(this.authUser.authority);
    if (this.hasIotHubAccess) {
      this.load();
    }
  }

  openItemDetail(item: MpItemVersionView): void {
    this.iotHubActions.openItemDetail(item, this.findInstalledSolutionTemplate(item), this.findInstalledDeviceCount(item))
      .subscribe(result => {
        if (result === 'installed' || result === 'deleted' || result === 'updated') {
          this.reloadInstalled(item.type);
        }
      });
  }

  getItemImage(item: MpItemVersionView): string | null {
    return resolveIotHubItemImageUrl(item, this.iotHubApiService);
  }

  isInstalled(item: MpItemVersionView): boolean {
    return item.type === ItemType.SOLUTION_TEMPLATE && !!this.findInstalledSolutionTemplate(item);
  }

  private findInstalledSolutionTemplate(item: MpItemVersionView): IotHubInstalledItem | undefined {
    return this.installedSolutionTemplates.find(i => i.itemId === item.itemId);
  }

  private findInstalledDeviceCount(item: MpItemVersionView): number {
    return this.installedDeviceCounts[item.itemId] || 0;
  }

  private load(): void {
    const config = { ignoreLoading: true };
    const sortOrder: SortOrder = { property: 'totalInstallCount', direction: Direction.DESC };
    const buildQuery = (type: ItemType): MpItemVersionQuery =>
      new MpItemVersionQuery(new PageLink(WIDGET_CARD_COUNT, 0, null, sortOrder), { type });
    const installedPageLink = new PageLink(10000, 0);

    forkJoin({
      solutionTemplates: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.SOLUTION_TEMPLATE), config),
      devices: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.DEVICE), config),
      installedSolutionTemplates: this.iotHubApiService.getInstalledItems(installedPageLink, ItemType.SOLUTION_TEMPLATE, undefined, config),
      installedDeviceCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.DEVICE, config)
    }).subscribe(result => {
      this.solutionTemplates = result.solutionTemplates.data;
      this.devices = result.devices.data;
      this.installedSolutionTemplates = result.installedSolutionTemplates.data;
      this.installedDeviceCounts = result.installedDeviceCounts;
      this.ctx.detectChanges();
    });
  }

  private reloadInstalled(type: ItemType): void {
    const config = { ignoreLoading: true };
    if (type === ItemType.SOLUTION_TEMPLATE) {
      const installedPageLink = new PageLink(10000, 0);
      this.iotHubApiService.getInstalledItems(installedPageLink, ItemType.SOLUTION_TEMPLATE, undefined, config).subscribe(data => {
        this.installedSolutionTemplates = data.data;
        this.ctx.detectChanges();
      });
    } else if (type === ItemType.DEVICE) {
      this.iotHubApiService.getInstalledItemCounts(ItemType.DEVICE, config).subscribe(counts => {
        this.installedDeviceCounts = counts;
        this.ctx.detectChanges();
      });
    }
  }

}
