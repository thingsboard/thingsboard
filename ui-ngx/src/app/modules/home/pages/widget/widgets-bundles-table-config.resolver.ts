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

import { Router } from '@angular/router';
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@app/core/http/widget.service';
import { WidgetsBundleComponent } from '@modules/home/pages/widget/widgets-bundle.component';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { Direction } from '@shared/models/page/sort-order';
import { WidgetsBundleTabsComponent } from '@home/pages/widget/widgets-bundle-tabs.component';

@Injectable()
export class WidgetsBundlesTableConfigResolver  {

  private readonly config: EntityTableConfig<WidgetsBundle> = new EntityTableConfig<WidgetsBundle>();

  constructor(private store: Store<AppState>,
              private dialogService: DialogService,
              private widgetsService: WidgetService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.WIDGETS_BUNDLE;
    this.config.entityComponent = WidgetsBundleComponent;
    this.config.entityTabsComponent = WidgetsBundleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.WIDGETS_BUNDLE);
    this.config.entityResources = entityTypeResources.get(EntityType.WIDGETS_BUNDLE);
    this.config.defaultSortOrder = {property: 'title', direction: Direction.ASC};

    this.config.rowPointer = true;

    this.config.entityTitle = (widgetsBundle) => widgetsBundle ?
      widgetsBundle.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<WidgetsBundle>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<WidgetsBundle>('title', 'widgets-bundle.title', '100%'),
      new EntityTableColumn<WidgetsBundle>('tenantId', 'widgets-bundle.system', '60px',
        entity => {
          return checkBoxCell(entity.tenantId.id === NULL_UUID);
        }),
    );

    this.config.addActionDescriptors.push(
      {
        name: this.translate.instant('widgets-bundle.create-new-widgets-bundle'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('widgets-bundle.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importWidgetsBundle($event)
      }
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('widgets-bundle.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportWidgetsBundle($event, entity)
      },
      {
        name: this.translate.instant('widgets-bundle.widgets-bundle-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.config.toggleEntityDetails($event, entity)
      }
    );

    this.config.deleteEntityTitle = widgetsBundle => this.translate.instant('widgets-bundle.delete-widgets-bundle-title',
      { widgetsBundleTitle: widgetsBundle.title });
    this.config.deleteEntityContent = () => this.translate.instant('widgets-bundle.delete-widgets-bundle-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('widgets-bundle.delete-widgets-bundles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('widgets-bundle.delete-widgets-bundles-text');


    this.config.loadEntity = id => this.widgetsService.getWidgetsBundle(id.id);
    this.config.saveEntity = widgetsBundle => this.widgetsService.saveWidgetsBundle(widgetsBundle);
    this.config.deleteEntity = id => this.widgetsService.deleteWidgetsBundle(id.id);
    this.config.onEntityAction = action => this.onWidgetsBundleAction(action, this.config);

    this.config.handleRowClick = ($event, widgetsBundle) => {
      if (this.config.isDetailsOpen()) {
        this.config.toggleEntityDetails($event, widgetsBundle);
      } else {
        this.openWidgetsBundle($event, widgetsBundle);
      }
      return true;
    };

    this.config.entityAdded = widgetsBundle => {
      this.openWidgetsBundle(null, widgetsBundle);
    };
  }

  resolve(): EntityTableConfig<WidgetsBundle> {
    this.config.tableTitle = this.translate.instant('widgets-bundle.widgets-bundles');
    const authUser = getCurrentAuthUser(this.store);
    this.config.deleteEnabled = (widgetsBundle) => this.isWidgetsBundleEditable(widgetsBundle, authUser.authority);
    this.config.entitySelectionEnabled = (widgetsBundle) => this.isWidgetsBundleEditable(widgetsBundle, authUser.authority);
    this.config.detailsReadonly = (widgetsBundle) => !this.isWidgetsBundleEditable(widgetsBundle, authUser.authority);
    const authState = getCurrentAuthState(this.store);
    this.config.entitiesFetchFunction = pageLink => this.widgetsService.getWidgetBundles(pageLink);
    return this.config;
  }

  isWidgetsBundleEditable(widgetsBundle: WidgetsBundle, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return widgetsBundle && widgetsBundle.tenantId && widgetsBundle.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }

  importWidgetsBundle($event: Event) {
    this.importExport.importWidgetsBundle().subscribe(
      (widgetsBundle) => {
        if (widgetsBundle) {
          this.config.updateData();
        }
      }
    );
  }

  openWidgetsBundle($event: Event, widgetsBundle: WidgetsBundle) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`resources/widgets-bundles/${widgetsBundle.id.id}/widgetTypes`);
  }

  private openWidgetsBundleDetails($event: Event, widgetsBundle: WidgetsBundle, config: EntityTableConfig<WidgetsBundle>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['details', widgetsBundle.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  exportWidgetsBundle($event: Event, widgetsBundle: WidgetsBundle) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetsBundle(widgetsBundle.id.id);
  }

  onWidgetsBundleAction(action: EntityAction<WidgetsBundle>, config: EntityTableConfig<WidgetsBundle>): boolean {
    switch (action.action) {
      case 'open':
        this.openWidgetsBundle(action.event, action.entity);
        return true;
      case 'openDetails':
        this.openWidgetsBundleDetails(action.event, action.entity, config);
        return true;
      case 'export':
        this.exportWidgetsBundle(action.event, action.entity);
        return true;
    }
    return false;
  }

}
