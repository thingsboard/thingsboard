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
  ResourceInfo, ResourceInfoWithReferences,
  ResourceType,
  ResourceTypeTranslationMap,
  toResourceDeleteResult
} from '@shared/models/resource.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ResourceService } from '@core/http/resource.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { ResourcesLibraryComponent } from '@home/components/resources/resources-library.component';
import { PageLink } from '@shared/models/page/page-link';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { catchError, map } from 'rxjs/operators';
import { ResourcesTableHeaderComponent } from '@home/pages/admin/resource/resources-table-header.component';
import { ResourceLibraryTabsComponent } from '@home/pages/admin/resource/resource-library-tabs.component';
import { forkJoin, of } from "rxjs";
import {
  ResourcesInUseDialogComponent,
  ResourcesInUseDialogData
} from "@shared/components/resource/resources-in-use-dialog.component";
import { parseHttpErrorMessage } from "@core/utils";
import { ActionNotificationShow } from "@core/notification/notification.actions";
import { ResourcesDatasource } from "@home/pages/admin/resource/resources-datasource";
import { MatDialog } from "@angular/material/dialog";
import { DialogService } from "@core/services/dialog.service";

@Injectable()
export class ResourcesLibraryTableConfigResolver  {

  private readonly config: EntityTableConfig<Resource, PageLink, ResourceInfo> = new EntityTableConfig<Resource, PageLink, ResourceInfo>();
  private readonly resourceTypesTranslationMap = ResourceTypeTranslationMap;

  constructor(private store: Store<AppState>,
              private resourceService: ResourceService,
              private translate: TranslateService,
              private router: Router,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.TB_RESOURCE;
    this.config.entityComponent = ResourcesLibraryComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.TB_RESOURCE);
    this.config.entityResources = entityTypeResources.get(EntityType.TB_RESOURCE);
    this.config.headerComponent = ResourcesTableHeaderComponent;
    this.config.entityTabsComponent = ResourceLibraryTabsComponent;

    this.config.entityTitle = (resource) => resource ?
      resource.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<ResourceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<ResourceInfo>('title', 'resource.title', '60%'),
      new EntityTableColumn<ResourceInfo>('resourceType', 'resource.resource-type', '40%',
        entity => this.translate.instant(this.resourceTypesTranslationMap.get(entity.resourceType))),
      new EntityTableColumn<ResourceInfo>('tenantId', 'resource.system', '60px',
        entity => checkBoxCell(entity.tenantId.id === NULL_UUID)),
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('resource.download'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.downloadResource($event, entity)
      },
      {
        name: this.translate.instant('resource.delete'),
        icon: 'delete',
        isEnabled: (resource) => this.config.deleteEnabled(resource),
        onAction: ($event, entity) => this.deleteResource($event, entity)
      },
    );

    this.config.groupActionDescriptors = [{
      name: this.translate.instant('action.delete'),
      icon: 'delete',
      isEnabled: true,
      onAction: ($event, entities) => this.deleteResources($event, entities)
    }];

    this.config.entitiesDeleteEnabled = false;

    this.config.entitiesFetchFunction = pageLink => this.resourceService.getResources(pageLink, this.config.componentsData.resourceType);
    this.config.loadEntity = id => this.resourceService.getResourceInfoById(id.id);
    this.config.saveEntity = resource => this.saveResource(resource);

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
    this.config.componentsData = {
      resourceType: ''
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
    const url = this.router.createUrlTree(['resources', 'resources-library', resourceInfo.id.id]);
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
      case 'deleteResource':
        this.deleteResource(action.event, action.entity);
    }
    return false;
  }

  private detailsReadonly(resource: ResourceInfo, authority: Authority): boolean {
    if (resource?.resourceType === ResourceType.LWM2M_MODEL) {
      return true;
    }
    return !this.isResourceEditable(resource, authority);
  }

  private isResourceEditable(resource: ResourceInfo, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return resource && resource.tenantId && resource.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }

  private deleteResource($event: Event, resource: ResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('resource.delete-resource-title', { resourceTitle: resource.title }),
      this.translate.instant('resource.delete-resource-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.resourceService.deleteResource(resource.id.id, false, {ignoreErrors: true}).pipe(
          map(() => toResourceDeleteResult(resource)),
          catchError((err) => of(toResourceDeleteResult(resource, err)))
        ).subscribe(
          (deleteResult) => {
            if (deleteResult.success) {
              if (this.config.getEntityDetailsPage()) {
                this.config.getEntityDetailsPage().goBack();
              } else {
                this.config.updateData(true);
              }
            } else if (deleteResult.resourceIsReferencedError) {
              const resources: ResourceInfoWithReferences[] = [{...resource, ...{references: deleteResult.references}}];
              const data = {
                multiple: false,
                resources,
                configuration: {
                  title: 'resource.resource-is-in-use',
                  message: this.translate.instant('resource.resource-is-in-use-text', {title: resources[0].title}),
                  deleteText: 'resource.delete-resource-in-use-text',
                  selectedText: 'resource.selected-resources',
                  columns: ['select', 'title', 'references']
                }
              };
              this.dialog.open<ResourcesInUseDialogComponent, ResourcesInUseDialogData,
                ResourceInfo[]>(ResourcesInUseDialogComponent, {
                disableClose: true,
                panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                data
              }).afterClosed().subscribe((resources) => {
                if (resources) {
                  this.resourceService.deleteResource(resource.id.id, true).subscribe(() => {
                    if (this.config.getEntityDetailsPage()) {
                      this.config.getEntityDetailsPage().goBack();
                    } else {
                      this.config.updateData(true);
                    }
                  });
                }
              });
            } else {
              const errorMessageWithTimeout = parseHttpErrorMessage(deleteResult.error, this.translate);
              setTimeout(() => {
                this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
              }, errorMessageWithTimeout.timeout);
            }
          }
        );
      }
    });
  }

  private deleteResources($event: Event, resources: ResourceInfo[]) {
    if ($event) {
      $event.stopPropagation();
    }
    if (resources && resources.length) {
      const title = this.translate.instant('resource.delete-resources-title', {count: resources.length});
      const content = this.translate.instant('resource.delete-resources-text');
      this.dialogService.confirm(title, content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')).subscribe((result) => {
        if (result) {
          const tasks = resources.map((resource) =>
            this.resourceService.deleteResource(resource.id.id, false, {ignoreErrors: true}).pipe(
              map(() => toResourceDeleteResult(resource)),
              catchError((err) => of(toResourceDeleteResult(resource, err)))
            )
          );
          forkJoin(tasks).subscribe(
            (deleteResults) => {
              const anySuccess = deleteResults.some(res => res.success);
              const referenceErrors = deleteResults.filter(res => res.resourceIsReferencedError);
              const otherError = deleteResults.find(res => !res.success);
              if (anySuccess) {
                this.config.updateData();
              }
              if (referenceErrors?.length) {
                const resourcesWithReferences: ResourceInfoWithReferences[] =
                  referenceErrors.map(ref => ({...ref.resource, ...{references: ref.references}}));
                const data = {
                  multiple: true,
                  resources: resourcesWithReferences,
                  configuration: {
                    title: 'resource.resources-are-in-use',
                    message: this.translate.instant('resource.resources-are-in-use-text'),
                    deleteText: 'resource.delete-resource-in-use-text',
                    selectedText: 'resource.selected-resources',
                    datasource: new ResourcesDatasource(this.resourceService, resourcesWithReferences, () => true),
                    columns: ['select', 'title', 'references']
                  }
                };
                this.dialog.open<ResourcesInUseDialogComponent, ResourcesInUseDialogData,
                  ResourceInfo[]>(ResourcesInUseDialogComponent, {
                  disableClose: true,
                  panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                  data
                }).afterClosed().subscribe((forceDeleteResources) => {
                  if (forceDeleteResources && forceDeleteResources.length) {
                    const forceDeleteTasks = forceDeleteResources.map((resource) =>
                      this.resourceService.deleteResource(resource.id.id, true)
                    );
                    forkJoin(forceDeleteTasks).subscribe(
                      () => {
                        this.config.updateData();
                      }
                    );
                  }
                });
              } else if (otherError) {
                const errorMessageWithTimeout = parseHttpErrorMessage(otherError.error, this.translate);
                setTimeout(() => {
                  this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
                }, errorMessageWithTimeout.timeout);
              }
            }
          );
        }
      });
    }
  }
}
