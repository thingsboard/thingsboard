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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { PageLink } from '@shared/models/page/page-link';
import { DeepLinkOpenItem } from './iot-hub-deep-link.utils';

interface ItemTypePageConfig {
  type: ItemType;
  titleKey: string;
  descriptionKey: string;
  image: string;
  routeSegment: string;
}

const PAGE_CONFIGS: Record<string, ItemTypePageConfig> = {
  WIDGET: {
    type: ItemType.WIDGET,
    titleKey: 'item.type-widget-plural',
    descriptionKey: 'iot-hub.items-page-desc-widgets',
    image: 'assets/iot-hub/items-page-widgets-hero.svg',
    routeSegment: 'widgets'
  },
  DASHBOARD: {
    type: ItemType.DASHBOARD,
    titleKey: 'item.type-dashboard-plural',
    descriptionKey: 'iot-hub.items-page-desc-dashboards',
    image: 'assets/iot-hub/items-page-dashboards-hero.svg',
    routeSegment: 'dashboards'
  },
  SOLUTION_TEMPLATE: {
    type: ItemType.SOLUTION_TEMPLATE,
    titleKey: 'item.type-solution-template-plural',
    descriptionKey: 'iot-hub.items-page-desc-solution-templates',
    image: 'assets/iot-hub/items-page-solution-templates-hero.png',
    routeSegment: 'solution-templates'
  },
  CALCULATED_FIELD: {
    type: ItemType.CALCULATED_FIELD,
    titleKey: 'item.type-calculated-field-plural',
    descriptionKey: 'iot-hub.items-page-desc-calculated-fields',
    image: 'assets/iot-hub/items-page-calculated-fields-hero.svg',
    routeSegment: 'calculated-fields'
  },
  RULE_CHAIN: {
    type: ItemType.RULE_CHAIN,
    titleKey: 'item.type-rule-chain-plural',
    descriptionKey: 'iot-hub.items-page-desc-rule-chains',
    image: 'assets/iot-hub/items-page-rule-chains-hero.svg',
    routeSegment: 'rule-chains'
  },
  DEVICE: {
    type: ItemType.DEVICE,
    titleKey: 'iot-hub.device-library',
    descriptionKey: 'iot-hub.items-page-desc-devices',
    image: 'assets/iot-hub/items-page-devices-hero.svg',
    routeSegment: 'devices'
  }
};

@Component({
  selector: 'tb-iot-hub-items-page',
  standalone: false,
  templateUrl: './iot-hub-items-page.component.html',
  styleUrls: ['./iot-hub-items-page.component.scss']
})
export class TbIotHubItemsPageComponent implements OnInit {

  config: ItemTypePageConfig;
  installedItemsCount = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private iotHubApiService: IotHubApiService,
    private iotHubActions: IotHubActionsService
  ) {}

  ngOnInit(): void {
    const itemType = this.route.snapshot.data['itemType'] as string;
    this.config = PAGE_CONFIGS[itemType];
    this.loadInstalledCount();
    this.maybeOpenDeepLinkedItem();
  }

  goBack(): void {
    void this.router.navigate(['/iot-hub']);
  }

  navigateToInstalledItems(): void {
    void this.router.navigate(['/iot-hub/installed'], { queryParams: { itemType: this.config.type } });
  }

  openSignup(): void {
    window.open('https://iothub.thingsboard.io/signup', '_blank');
  }

  private loadInstalledCount(): void {
    this.iotHubApiService.getInstalledItemsCount(this.config.type, { ignoreLoading: true }).subscribe(count => {
      this.installedItemsCount = count;
    });
  }

  private maybeOpenDeepLinkedItem(): void {
    const openItem = history.state?.openItem as DeepLinkOpenItem | undefined;
    if (!openItem || openItem.version.type !== this.config.type) {
      return;
    }
    history.replaceState({ ...history.state, openItem: undefined }, '');
    this.resolveInstalledItem(openItem.version).subscribe(installed => {
      this.iotHubActions.openItemDetail(
        openItem.version,
        installed ?? undefined,
        installed ? 1 : 0,
        'default',
        true,
        openItem.preview
      ).subscribe();
    });
  }

  private resolveInstalledItem(version: MpItemVersionView): Observable<IotHubInstalledItem | null> {
    return this.iotHubApiService
      .getInstalledItems(new PageLink(1), undefined, version.itemId, { ignoreLoading: true })
      .pipe(map(page => page.data[0] ?? null));
  }
}
