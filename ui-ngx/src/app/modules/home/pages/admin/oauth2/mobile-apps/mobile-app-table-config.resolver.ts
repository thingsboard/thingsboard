///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import {
  CellActionDescriptorType,
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityChipsEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { MobileApp, MobileAppInfo } from '@shared/models/oauth2.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { isEqual } from '@core/utils';
import { Direction } from '@app/shared/models/page/sort-order';
import { MobileAppService } from '@core/http/mobile-app.service';
import { MobileAppComponent } from '@home/pages/admin/oauth2/mobile-apps/mobile-app.component';
import { MobileAppTableHeaderComponent } from '@home/pages/admin/oauth2/mobile-apps/mobile-app-table-header.component';
import { map, Observable, of, mergeMap } from 'rxjs';

@Injectable()
export class MobileAppTableConfigResolver implements Resolve<EntityTableConfig<MobileAppInfo>> {

  private readonly config: EntityTableConfig<MobileAppInfo> = new EntityTableConfig<MobileAppInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private mobileAppService: MobileAppService) {
    this.config.tableTitle = this.translate.instant('admin.oauth2.mobile-apps');
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.MOBILE_APP;
    this.config.rowPointer = true;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.MOBILE_APP);
    this.config.entityResources = entityTypeResources.get(EntityType.MOBILE_APP);
    this.config.entityComponent = MobileAppComponent;
    this.config.headerComponent = MobileAppTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<MobileAppInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<MobileAppInfo>('pkgName', 'admin.oauth2.mobile-package', '170px'),
      new EntityTableColumn<MobileAppInfo>('appSecret', 'admin.oauth2.mobile-app-secret', '350px',
        (entity) => entity.appSecret ? this.appSecretText(entity) : '', () => ({}),
        false, () => ({}), () => undefined, false,
        {
          name: this.translate.instant('admin.oauth2.copy-mobile-app-secret'),
          icon: 'content_copy',
          style: {
            padding: '4px',
            'font-size': '16px',
            color: 'rgba(0,0,0,.87)'
          },
          isEnabled: (entity) => !!entity.appSecret,
          onAction: ($event, entity) => entity.appSecret,
          type: CellActionDescriptorType.COPY_BUTTON
        }),
      new EntityChipsEntityTableColumn<MobileAppInfo>('oauth2ClientInfos', 'admin.oauth2.clients', '20%'),
      new EntityActionTableColumn('oauth2Enabled', 'admin.oauth2.enable',
        {
          name: '',
          nameFunction: (app) =>
            this.translate.instant(app.oauth2Enabled ? 'admin.oauth2.disable' : 'admin.oauth2.enable'),
          icon: 'mdi:toggle-switch',
          iconFunction: (entity) => entity.oauth2Enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
          isEnabled: () => true,
          onAction: ($event, entity) => this.toggleEnableOAuth($event, entity)
        })
    );

    this.config.deleteEntityTitle = (app) => this.translate.instant('admin.oauth2.delete-mobile-app-title', {applicationName: app.pkgName});
    this.config.deleteEntityContent = () => this.translate.instant('admin.oauth2.delete-mobile-app-text');
    this.config.entitiesFetchFunction = pageLink => this.mobileAppService.getTenantMobileAppInfos(pageLink);
    this.config.loadEntity = id => this.mobileAppService.getMobileAppInfoById(id.id);
    this.config.saveEntity = (mobileApp, originalMobileApp) => {
      const clientsIds = mobileApp.oauth2ClientInfos as Array<string> || [];
      let clientsTask: Observable<void>;
      if (mobileApp.id && !isEqual(mobileApp.oauth2ClientInfos?.sort(),
        originalMobileApp.oauth2ClientInfos?.map(info => info.id ? info.id.id : info).sort())) {
        clientsTask = this.mobileAppService.updateOauth2Clients(mobileApp.id.id, clientsIds);
      } else {
        clientsTask = of(null);
      }
      delete mobileApp.oauth2ClientInfos;
      return clientsTask.pipe(
        mergeMap(() => this.mobileAppService.saveMobileApp(mobileApp as MobileApp, mobileApp.id ? [] : clientsIds)),
        map(savedMobileApp => {
          (savedMobileApp as MobileAppInfo).oauth2ClientInfos = clientsIds;
          return savedMobileApp;
        })
      );
    };
    this.config.deleteEntity = id => this.mobileAppService.deleteMobileApp(id.id);
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<MobileAppInfo> {
    return this.config;
  }

  private toggleEnableOAuth($event: Event, mobileApp: MobileAppInfo): void {
    if ($event) {
      $event.stopPropagation();
    }

    const modifiedMobileApp: MobileAppInfo = {
      ...mobileApp,
      oauth2Enabled: !mobileApp.oauth2Enabled
    };

    this.mobileAppService.saveMobileApp(modifiedMobileApp, mobileApp.oauth2ClientInfos.map(clientInfo => clientInfo.id.id),
      {ignoreLoading: true})
      .subscribe((result) => {
        mobileApp.oauth2Enabled = result.oauth2Enabled;
        this.config.getTable().detectChanges();
      });
  }

  private appSecretText(entity): string {
    let text = entity.appSecret;
    if (text.length > 35) {
      text = `${text.slice(0, 35)}…`;
    }
    return text;
  }

}
