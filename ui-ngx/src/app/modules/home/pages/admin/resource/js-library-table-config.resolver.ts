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
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Router } from '@angular/router';
import {
  Resource,
  ResourceInfo,
  ResourceSubType,
  ResourceSubTypeTranslationMap,
  ResourceType
} from '@shared/models/resource.models';
import { EntityType, entityTypeResources } from '@shared/models/entity-type.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ResourceService } from '@core/http/resource.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { PageLink } from '@shared/models/page/page-link';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { JsLibraryTableHeaderComponent } from '@home/pages/admin/resource/js-library-table-header.component';
import { JsResourceComponent } from '@home/pages/admin/resource/js-resource.component';
import { switchMap } from 'rxjs/operators';
import { ResourceTabsComponent } from '@home/pages/admin/resource/resource-tabs.component';

@Injectable()
export class JsLibraryTableConfigResolver  {

  private readonly config: EntityTableConfig<Resource, PageLink, ResourceInfo> = new EntityTableConfig<Resource, PageLink, ResourceInfo>();

  constructor(private store: Store<AppState>,
              private resourceService: ResourceService,
              private translate: TranslateService,
              private router: Router,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.TB_RESOURCE;
    this.config.entityComponent = JsResourceComponent;
    this.config.entityTabsComponent = ResourceTabsComponent;
    this.config.entityTranslations = {
      details: 'javascript.javascript-resource-details',
      add: 'javascript.add',
      noEntities: 'javascript.no-javascript-resource-text',
      search: 'javascript.search',
      selectedEntities: 'javascript.selected-javascript-resources'
    };
    this.config.entityResources = entityTypeResources.get(EntityType.TB_RESOURCE);
    this.config.headerComponent = JsLibraryTableHeaderComponent;

    this.config.entityTitle = (resource) => resource ?
      resource.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<ResourceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<ResourceInfo>('title', 'resource.title', '60%'),
      new EntityTableColumn<ResourceInfo>('resourceSubType', 'javascript.javascript-type', '40%',
        entity => this.translate.instant(ResourceSubTypeTranslationMap.get(entity.resourceSubType))),
      new EntityTableColumn<ResourceInfo>('tenantId', 'resource.system', '60px',
        entity => checkBoxCell(entity.tenantId.id === NULL_UUID)),
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('javascript.download'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.downloadResource($event, entity)
      }
    );

    this.config.deleteEntityTitle = resource => this.translate.instant('javascript.delete-javascript-resource-title',
      { resourceTitle: resource.title });
    this.config.deleteEntityContent = () => this.translate.instant('javascript.delete-javascript-resource-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('javascript.delete-javascript-resources-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('javascript.delete-javascript-resources-text');

    this.config.entitiesFetchFunction = pageLink => this.resourceService.getResources(pageLink, ResourceType.JS_MODULE, this.config.componentsData.resourceSubType);
    this.config.loadEntity = id => {
      const current = this.config.getTable()?.dataSource?.currentEntity as ResourceInfo;
      if (!current || current?.resourceSubType === ResourceSubType.MODULE) {
        return this.resourceService.getResource(id.id);
      } else {
        return this.resourceService.getResourceInfoById(id.id)
      }
    };
    this.config.saveEntity = resource => {
      resource.resourceType = ResourceType.JS_MODULE;
      let saveObservable = this.resourceService.saveResource(resource);
      if (resource.resourceSubType === ResourceSubType.MODULE) {
        saveObservable = saveObservable.pipe(
          switchMap((saved) => this.resourceService.getResource(saved.id.id))
        );
      }
      return saveObservable;
    };
    this.config.deleteEntity = id => this.resourceService.deleteResource(id.id);

    this.config.onEntityAction = action => this.onResourceAction(action);
  }

  resolve(): EntityTableConfig<Resource, PageLink, ResourceInfo> {
    this.config.tableTitle = this.translate.instant('javascript.javascript-library');
    this.config.componentsData = {
      resourceSubType: ''
    };
    const authUser = getCurrentAuthUser(this.store);
    this.config.deleteEnabled = (resource) => this.isResourceEditable(resource, authUser.authority);
    this.config.entitySelectionEnabled = (resource) => this.isResourceEditable(resource, authUser.authority);
    this.config.detailsReadonly = (resource) => this.detailsReadonly(resource, authUser.authority);
    return this.config;
  }

  private openResource($event: Event, resourceInfo: ResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['resources', 'javascript-library', resourceInfo.id.id]);
    this.router.navigateByUrl(url).then(() => {});
  }

  downloadResource($event: Event, resource: ResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.resourceService.downloadResource(resource.id.id).subscribe();
  }

  onResourceAction(action: EntityAction<ResourceInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openResource(action.event, action.entity);
        return true;
      case 'downloadResource':
        this.downloadResource(action.event, action.entity);
        return true;
    }
    return false;
  }

  private detailsReadonly(resource: ResourceInfo, authority: Authority): boolean {
    return !this.isResourceEditable(resource, authority);
  }

  private isResourceEditable(resource: ResourceInfo, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return resource && resource.tenantId && resource.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }
}
