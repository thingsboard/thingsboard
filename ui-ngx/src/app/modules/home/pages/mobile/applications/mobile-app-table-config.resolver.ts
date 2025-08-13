///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { ActivatedRouteSnapshot } from '@angular/router';
import {
  CellActionDescriptor,
  CellActionDescriptorType,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@app/shared/models/page/sort-order';
import { MobileAppService } from '@core/http/mobile-app.service';
import { MobileAppComponent } from '@home/pages/mobile/applications/mobile-app.component';
import { MobileAppTableHeaderComponent } from '@home/pages/mobile/applications/mobile-app-table-header.component';
import {
  MobileApp,
  MobileAppBundleInfo,
  MobileAppStatus,
  mobileAppStatusTranslations
} from '@shared/models/mobile-app.models';
import { platformTypeTranslations } from '@shared/models/oauth2.models';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatDialog } from '@angular/material/dialog';
import {
  MobileAppDeleteDialogData,
  RemoveAppDialogComponent
} from '@home/pages/mobile/applications/remove-app-dialog.component';

@Injectable()
export class MobileAppTableConfigResolver  {

  private readonly config: EntityTableConfig<MobileApp> = new EntityTableConfig<MobileApp>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private mobileAppService: MobileAppService,
              private truncatePipe: TruncatePipe,
              private dialog: MatDialog
              ) {
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.MOBILE_APP;
    this.config.addAsTextButton = true;
    this.config.entitiesDeleteEnabled = false;
    this.config.rowPointer = true;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.MOBILE_APP);
    this.config.entityResources = entityTypeResources.get(EntityType.MOBILE_APP);
    this.config.entityComponent = MobileAppComponent;
    this.config.headerComponent = MobileAppTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<MobileApp>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<MobileApp>('pkgName', 'mobile.application-package', '20%', (entity) => entity.pkgName ?? '', () => ({}),
        true, () => ({}), () => undefined, false,
        {
          name: this.translate.instant('mobile.copy-application-package'),
          icon: 'content_copy',
          style: {
            padding: '4px',
            'font-size': '16px',
            color: 'rgba(0,0,0,.54)'
          },
          isEnabled: (entity) => !!entity.pkgName,
          onAction: (_$event, entity) => entity.pkgName,
          type: CellActionDescriptorType.COPY_BUTTON
        }),
      new EntityTableColumn<MobileApp>('title', 'mobile.mobile-package-title', '20%'),
      new EntityTableColumn<MobileApp>('appSecret', 'mobile.application-secret', '15%',
        (entity) => this.truncatePipe.transform(entity.appSecret, true, 10, '…'), () => ({}),
        true, () => ({}), () => undefined, false,
        {
          name: this.translate.instant('mobile.copy-application-secret'),
          icon: 'content_copy',
          style: {
            padding: '4px',
            'font-size': '16px',
            color: 'rgba(0,0,0,.54)'
          },
          isEnabled: (entity) => !!entity.appSecret,
          onAction: (_$event, entity) => entity.appSecret,
          type: CellActionDescriptorType.COPY_BUTTON
        }),
      new EntityTableColumn<MobileApp>('platformType', 'mobile.platform-type', '15%',
        (entity) => this.translate.instant(platformTypeTranslations.get(entity.platformType))
      ),
      new EntityTableColumn<MobileApp>('status', 'mobile.status', '15%',
        (entity) => `<span style="display: flex;">${this.mobileStatus(entity.status)}</span>`,
        (entity)=> this.mobileStatusStyle(entity.status)
      ),
      new EntityTableColumn<MobileApp>('minVersion', 'mobile.min-version', '15%',
        (entity) => entity.versionInfo?.minVersion ?? '', () => ({}), false),
      new EntityTableColumn<MobileApp>('latestVersion', 'mobile.latest-version', '15%',
        (entity) => entity.versionInfo?.latestVersion ?? '', () => ({}), false),
    );

    this.config.entitiesFetchFunction = pageLink => this.mobileAppService.getTenantMobileAppInfos(pageLink);
    this.config.loadEntity = id => this.mobileAppService.getMobileAppInfoById(id.id);
    this.config.saveEntity = (mobileApp) => this.mobileAppService.saveMobileApp(mobileApp);
    this.config.deleteEntityTitle = (mobileApp) => this.translate.instant('mobile.delete-application-title-short', {name: mobileApp.name});
    this.config.deleteEntityContent = () => this.translate.instant('mobile.delete-application-text-short');
    this.config.deleteEntity = id => this.mobileAppService.deleteMobileApp(id.id);

    this.config.cellActionDescriptors = this.configureCellActions();
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<MobileApp> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<MobileApp>> {
    return [
      {
        name: this.translate.instant('action.delete'),
        icon: 'delete',
        isEnabled: () => true,
        onAction: ($event, entity) => this.deleteEntity($event, entity)
      }
    ];
  }

  private deleteEntity($event: Event, entity: MobileApp) {
    if ($event) {
      $event.stopPropagation();
    }
    if(entity.status === MobileAppStatus.PUBLISHED) {
      this.dialog.open<RemoveAppDialogComponent, MobileAppDeleteDialogData,
        MobileAppBundleInfo>(RemoveAppDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          id: entity.id.id
        }
      }).afterClosed()
        .subscribe((res) => {
          if (res) {
            this.config.updateData();
          }
        });
    } else {
      this.config.getTable().deleteEntity($event, entity);
    }
  }

  private mobileStatus(status: MobileAppStatus): string {
    const translateKey = mobileAppStatusTranslations.get(status);
    let backgroundColor = 'rgba(25, 128, 56, 0.06)';
    switch (status) {
      case MobileAppStatus.DEPRECATED:
        backgroundColor = 'rgba(250, 164, 5, 0.06)';
        break;
      case MobileAppStatus.SUSPENDED:
        backgroundColor = 'rgba(209, 39, 48, 0.06)';
        break;
      case MobileAppStatus.DRAFT:
        backgroundColor = 'rgba(0, 148, 255, 0.06)';
        break;
    }
    return `<div style="border-radius: 14px; height: 28px; line-height: 20px; padding: 4px 10px;
                        width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(translateKey)}
            </div>`;
  }

  private mobileStatusStyle(status: MobileAppStatus): object {
    const styleObj = {
      fontSize: '14px',
      color: '#198038'
    };
    switch (status) {
      case MobileAppStatus.DEPRECATED:
        styleObj.color = '#FAA405';
        break;
      case MobileAppStatus.SUSPENDED:
        styleObj.color = '#D12730';
        break;
      case MobileAppStatus.DRAFT:
        styleObj.color = '#0094FF';
        break;
    }
    return styleObj;
  }

}
