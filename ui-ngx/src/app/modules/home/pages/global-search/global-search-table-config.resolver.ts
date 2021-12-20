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
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GlobalSearchInfo } from '@shared/models/global-search.models';
import { GlobalSearchTableHeaderComponent } from '@home/pages/global-search/global-search-table-header.component';
import { EntityService } from '@core/http/entity.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ClipboardService } from 'ngx-clipboard';

@Injectable()
export class GlobalSearchTableConfigResolver implements Resolve<EntityTableConfig<GlobalSearchInfo>> {

  private readonly config: EntityTableConfig<GlobalSearchInfo> = new EntityTableConfig<GlobalSearchInfo>();

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private clipboard: ClipboardService) {

    this.config.entityType = EntityType.GLOBAL_SEARCH;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.GLOBAL_SEARCH);
    this.config.entityResources = entityTypeResources.get(EntityType.GLOBAL_SEARCH);
    this.config.hideTitleOnMobile = true;
    this.config.searchEnabled = false;
    this.config.addEnabled = false;
    this.config.selectionEnabled = false;
    this.config.entitiesDeleteEnabled = false;
    this.config.detailsPanelEnabled = false;
    this.config.headerComponent = GlobalSearchTableHeaderComponent;

    this.config.columns.push(
      new DateEntityTableColumn<GlobalSearchInfo>('createdTime', 'global-search.created-time', this.datePipe, '150px'),
      new DateEntityTableColumn<GlobalSearchInfo>('lastActivityTime', 'global-search.last-activity-time', this.datePipe, '150px'),
      new EntityTableColumn<GlobalSearchInfo>('id', 'global-search.id', '250px',
        (entity) => entity.id.id),
      new EntityTableColumn<GlobalSearchInfo>('name', 'global-search.entity-name', '20%'),
      new EntityTableColumn<GlobalSearchInfo>('type', 'global-search.entity-type', '20%'),
      new EntityTableColumn<GlobalSearchInfo>('tenantInfo', 'global-search.tenant-name', '20%',
        (entity) => entity.tenantInfo ? entity.tenantInfo.name : '', () => ({}), false),
      new EntityTableColumn<GlobalSearchInfo>('ownerInfo', 'global-search.owner-name', '20%',
        (entity) => entity.ownerInfo ? entity.ownerInfo.name : '', () => ({}), false)
    );

    this.config.cellActionDescriptors = [{
      name: this.translate.instant('global-search.copy-id'),
      mdiIcon: 'mdi:clipboard-arrow-left',
      isEnabled: () => true,
      onAction: ($event, entity) => {
        this.clipboard.copy(entity.id.id);
        this.onDeviceIdCopied($event);
      }
    }];

    this.config.entitiesFetchFunction = pageLink =>
      this.entityService.searchEntitiesByQuery(this.config.componentsData, pageLink);
  }

  onDeviceIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('global-search.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  resolve(): EntityTableConfig<GlobalSearchInfo> {
    this.config.tableTitle = this.translate.instant('global-search.global-search');

    this.config.componentsData = {
      entityType: EntityType.DEVICE,
      searchQuery: ''
    };

    return this.config;
  }
}
