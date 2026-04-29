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
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Router } from '@angular/router';
import {
  Resource,
  ResourceInfo,
  ResourceInfoWithReferences,
  ResourceSubType,
  ResourceSubTypeTranslationMap,
  ResourceType,
  toResourceDeleteResult
} from '@shared/models/resource.models';
import { EntityType } from '@shared/models/entity-type.models';
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
import { catchError, map, switchMap } from 'rxjs/operators';
import { ResourceTabsComponent } from '@home/pages/admin/resource/resource-tabs.component';
import { forkJoin, Observable, of } from 'rxjs';
import { parseHttpErrorMessage } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import {
  ResourcesInUseDialogComponent,
  ResourcesInUseDialogData
} from "@shared/components/resource/resources-in-use-dialog.component";
import { ResourcesDatasource } from "@home/pages/admin/resource/resources-datasource";

@Injectable()
export class JsLibraryTableConfigResolver  {

  private readonly config: EntityTableConfig<Resource, PageLink, ResourceInfo> = new EntityTableConfig<Resource, PageLink, ResourceInfo>();

  constructor(private store: Store<AppState>,
              private resourceService: ResourceService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private dialogService: DialogService,
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
    this.config.entityResources = {
      helpLinkId: 'jsExtension'
    };
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

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.groupActionDescriptors = [{
      name: this.translate.instant('action.delete'),
      icon: 'delete',
      isEnabled: true,
      onAction: ($event, entities) => this.deleteResources($event, entities)
    }];

    this.config.entitiesDeleteEnabled = false;

    this.config.entitiesFetchFunction = pageLink => this.resourceService.getResources(pageLink, ResourceType.JS_MODULE, this.config.componentsData.resourceSubType);
    this.config.loadEntity = id => {
      const current = this.config.getTable()?.dataSource?.currentEntity as ResourceInfo;
      if (!current || current?.resourceSubType === ResourceSubType.MODULE) {
        return this.resourceService.getResource(id.id);
      } else {
        return this.resourceService.getResourceInfoById(id.id)
      }
    };
    this.config.saveEntity = (resource: Resource, originalResource: Resource) => {
      resource.resourceType = ResourceType.JS_MODULE;
      let saveObservable: Observable<Resource>;
      if (!originalResource) {
        saveObservable = this.resourceService.uploadResource(resource);
      } else {
        const { data, ...resourceInfo } = resource;
        saveObservable = this.resourceService.updatedResourceInfo(resource.id.id, resourceInfo);
        if (data) {
          saveObservable = saveObservable.pipe(
            switchMap(() => this.resourceService.updatedResourceData(resource.id.id, data))
          )
        }
      }
      if (resource.resourceSubType === ResourceSubType.MODULE) {
        saveObservable = saveObservable.pipe(
          switchMap((saved) => this.resourceService.getResource(saved.id.id))
        );
      }
      return saveObservable;
    };

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
      case 'deleteLibrary':
        this.deleteResource(action.event, action.entity);
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

  private deleteResource($event: Event, resource: ResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('javascript.delete-javascript-resource-title', { resourceTitle: resource.title }),
      this.translate.instant('javascript.delete-javascript-resource-text'),
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
                  title: 'javascript.javascript-resource-is-in-use',
                  message: this.translate.instant('javascript.javascript-resource-is-in-use-text', {title: resources[0].title}),
                  deleteText: 'javascript.delete-javascript-resource-in-use-text',
                  selectedText: 'javascript.selected-javascript-resources',
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
      const title = this.translate.instant('javascript.delete-javascript-resources-title', {count: resources.length});
      const content = this.translate.instant('javascript.delete-javascript-resources-text');
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
                    title: 'javascript.javascript-resources-are-in-use',
                    message: this.translate.instant('javascript.javascript-resources-are-in-use-text'),
                    deleteText: 'javascript.delete-javascript-resource-in-use-text',
                    selectedText: 'javascript.selected-javascript-resources',
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

  private configureCellActions(): Array<CellActionDescriptor<ResourceInfo>> {
    const actions: Array<CellActionDescriptor<ResourceInfo>> = [];
    actions.push(
      {
        name: this.translate.instant('javascript.download'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.downloadResource($event, entity)
      },
      {
        name: this.translate.instant('javascript.delete'),
        icon: 'delete',
        isEnabled: (resource) => this.config.deleteEnabled(resource),
        onAction: ($event, entity) => this.deleteResource($event, entity)
      },
    );
    return actions;
  }
}
