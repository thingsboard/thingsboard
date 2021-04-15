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
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Resolve } from '@angular/router';
import { Resource, ResourceInfo, ResourceTypeTranslationMap } from '@shared/models/resource.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ResourceService } from '@core/http/resource.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { ResourcesLibraryComponent } from '@home/pages/resource/resources-library.component';
import { PageLink } from '@shared/models/page/page-link';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { map } from 'rxjs/operators';

@Injectable()
export class ResourcesLibraryTableConfigResolver implements Resolve<EntityTableConfig<Resource, PageLink, ResourceInfo>> {

  private readonly config: EntityTableConfig<Resource, PageLink, ResourceInfo> = new EntityTableConfig<Resource, PageLink, ResourceInfo>();
  private readonly resourceTypesTranslationMap = ResourceTypeTranslationMap;

  constructor(private store: Store<AppState>,
              private resourceService: ResourceService,
              private translate: TranslateService,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.TB_RESOURCE;
    this.config.entityComponent = ResourcesLibraryComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.TB_RESOURCE);
    this.config.entityResources = entityTypeResources.get(EntityType.TB_RESOURCE);

    this.config.entityTitle = (resource) => resource ?
      resource.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<ResourceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<ResourceInfo>('title', 'resource.title', '60%'),
      new EntityTableColumn<ResourceInfo>('resourceType', 'resource.resource-type', '40%',
        entity => this.resourceTypesTranslationMap.get(entity.resourceType)),
      new EntityTableColumn<ResourceInfo>('tenantId', 'resource.system', '60px',
        entity => {
          return checkBoxCell(entity.tenantId.id === NULL_UUID);
        }),
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('resource.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportResource($event, entity)
      }
    );

    this.config.deleteEntityTitle = resource => this.translate.instant('resource.delete-resource-title',
      {resourceTitle: resource.title});
    this.config.deleteEntityContent = () => this.translate.instant('resource.delete-resource-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('resource.delete-resources-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('resource.delete-resources-text');

    this.config.entitiesFetchFunction = pageLink => this.resourceService.getResources(pageLink);
    this.config.loadEntity = id => this.resourceService.getResource(id.id);
    this.config.saveEntity = resource => this.saveResource(resource);
    this.config.deleteEntity = id => this.resourceService.deleteResource(id.id);

    this.config.onEntityAction = action => this.onResourceAction(action);
  }

  saveResource(resource) {
    if (Array.isArray(resource.data)) {
      const resources = [];
      resource.data.forEach((data, index) => {
        resources.push({
          resourceType: resource.resourceType,
          data,
          fileName: resource.fileName[index],
          title: resource.title
        });
      });
      return this.resourceService.saveResources(resources, {resendRequest: true}).pipe(
        map((response) => response[0])
      );
    } else {
      return this.resourceService.saveResource(resource);
    }
  }

  resolve(): EntityTableConfig<Resource, PageLink, ResourceInfo> {
    this.config.tableTitle = this.translate.instant('resource.resources-library');
    const authUser = getCurrentAuthUser(this.store);
    this.config.deleteEnabled = (resource) => this.isResourceEditable(resource, authUser.authority);
    this.config.entitySelectionEnabled = (resource) => this.isResourceEditable(resource, authUser.authority);
    this.config.detailsReadonly = (resource) => !this.isResourceEditable(resource, authUser.authority);
    return this.config;
  }

  exportResource($event: Event, resource: ResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.resourceService.downloadResource(resource.id.id).subscribe();
  }

  onResourceAction(action: EntityAction<ResourceInfo>): boolean {
    switch (action.action) {
      case 'uploadResource':
        this.exportResource(action.event, action.entity);
        return true;
    }
    return false;
  }

  private isResourceEditable(resource: ResourceInfo, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return resource && resource.tenantId && resource.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }
}
