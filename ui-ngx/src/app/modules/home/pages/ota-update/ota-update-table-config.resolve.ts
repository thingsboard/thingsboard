///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  ChecksumAlgorithmTranslationMap,
  OtaPackage,
  OtaPackageInfo,
  OtaUpdateTypeTranslationMap
} from '@shared/models/ota-package.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { OtaPackageService } from '@core/http/ota-package.service';
import { PageLink } from '@shared/models/page/page-link';
import { OtaUpdateComponent } from '@home/pages/ota-update/ota-update.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';
import { ClipboardService } from 'ngx-clipboard';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Injectable()
export class OtaUpdateTableConfigResolve implements Resolve<EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo>> {

  private readonly config: EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo> =
    new EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private store: Store<AppState>,
              private otaPackageService: OtaPackageService,
              private fileSize: FileSizePipe,
              private clipboardService: ClipboardService) {
    this.config.entityType = EntityType.OTA_PACKAGE;
    this.config.entityComponent = OtaUpdateComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.OTA_PACKAGE);
    this.config.entityResources = entityTypeResources.get(EntityType.OTA_PACKAGE);

    this.config.entityTitle = (otaPackage) => otaPackage ? otaPackage.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<OtaPackageInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<OtaPackageInfo>('title', 'ota-update.title', '25%'),
      new EntityTableColumn<OtaPackageInfo>('version', 'ota-update.version', '25%'),
      new EntityTableColumn<OtaPackageInfo>('type', 'ota-update.package-type', '25%', entity => {
        return this.translate.instant(OtaUpdateTypeTranslationMap.get(entity.type));
      }),
      new EntityTableColumn<OtaPackageInfo>('fileName', 'ota-update.file-name', '25%'),
      new EntityTableColumn<OtaPackageInfo>('dataSize', 'ota-update.file-size', '70px', entity => {
        return entity.dataSize ? this.fileSize.transform(entity.dataSize) : '';
      }),
      new EntityTableColumn<OtaPackageInfo>('checksum', 'ota-update.checksum', '540px', entity => {
        return entity.checksum ? `${ChecksumAlgorithmTranslationMap.get(entity.checksumAlgorithm)}: ${entity.checksum}` : '';
      }, () => ({}), false)
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('ota-update.copy-checksum'),
        icon: 'content_copy',
        isEnabled: (otaPackage) => !!otaPackage.checksum,
        onAction: ($event, entity) => this.copyPackageChecksum($event, entity)
      },
      {
        name: this.translate.instant('ota-update.download'),
        icon: 'file_download',
        isEnabled: (otaPackage) => otaPackage.hasData,
        onAction: ($event, entity) => this.exportPackage($event, entity)
      }
    );

    this.config.deleteEntityTitle = otaPackage => this.translate.instant('ota-update.delete-ota-update-title',
      { title: otaPackage.title });
    this.config.deleteEntityContent = () => this.translate.instant('ota-update.delete-ota-update-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('ota-update.delete-ota-updates-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('ota-update.delete-ota-updates-text');

    this.config.entitiesFetchFunction = pageLink => this.otaPackageService.getOtaPackages(pageLink);
    this.config.loadEntity = id => this.otaPackageService.getOtaPackageInfo(id.id);
    this.config.saveEntity = otaPackage => this.otaPackageService.saveOtaPackage(otaPackage);
    this.config.deleteEntity = id => this.otaPackageService.deleteOtaPackage(id.id);

    this.config.onEntityAction = action => this.onPackageAction(action);
  }

  resolve(): EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo> {
    this.config.tableTitle = this.translate.instant('ota-update.packages-repository');
    return this.config;
  }

  exportPackage($event: Event, otaPackageInfo: OtaPackageInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (otaPackageInfo.url) {
      window.open(otaPackageInfo.url, '_blank');
    } else {
      this.otaPackageService.downloadOtaPackage(otaPackageInfo.id.id).subscribe();
    }
  }

  copyPackageChecksum($event: Event, otaPackageInfo: OtaPackageInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.clipboardService.copy(otaPackageInfo?.checksum);
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('ota-update.checksum-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onPackageAction(action: EntityAction<OtaPackageInfo>): boolean {
    switch (action.action) {
      case 'uploadPackage':
        this.exportPackage(action.event, action.entity);
        return true;
      case 'copyChecksum':
        this.copyPackageChecksum(action.event, action.entity);
        return true;
    }
    return false;
  }

}
