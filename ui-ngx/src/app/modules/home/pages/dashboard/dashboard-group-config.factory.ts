// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Inject, Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupDetailsMode, EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Dashboard, DashboardInfo } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { DashboardFormComponent } from '@home/pages/dashboard/dashboard-form.component';
import { Operation, Resource } from '@shared/models/security.models';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { Router, UrlTree } from '@angular/router';
import {
  PublicDashboardLinkDialogComponent,
  PublicDashboardLinkDialogData
} from '@home/pages/dashboard/public-dashboard-link.dialog.component';
import { WINDOW } from '@core/services/window.service';
import { map, mergeMap } from 'rxjs/operators';
import { Widget } from '@shared/models/widget.models';
import { EntityAliases } from '@shared/models/alias.models';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';

// @dynamic
@Injectable()
export class DashboardGroupConfigFactory implements EntityGroupStateConfigFactory<DashboardInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<DashboardInfo>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog,
              private importExport: ImportExportService,
              private homeDialogs: HomeDialogsService,
              private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              private store: Store<AppState>,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<DashboardInfo>):
    Observable<GroupEntityTableConfig<DashboardInfo>> {
    const config = new GroupEntityTableConfig<DashboardInfo>(entityGroup, params);

    config.entityComponent = DashboardFormComponent;
    config.addDialogStyle = {height: '800px'};

    config.entityTitle = (dashboard) => dashboard ?
      this.utils.customTranslation(dashboard.title, dashboard.title) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = dashboard => this.translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: dashboard.title});
    config.deleteEntityContent = () => this.translate.instant('dashboard.delete-dashboard-text');
    config.deleteEntitiesTitle = count => this.translate.instant('dashboard.delete-dashboards-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('dashboard.delete-dashboards-text');

    config.loadEntity = id => this.dashboardService.getDashboardInfo(id.id);
    config.saveEntity = dashboard => this.dashboardService.saveDashboard(dashboard).pipe(
      mergeMap((savedDashboard) => this.dashboardService.getDashboardInfo(savedDashboard.id.id))
    );
    config.deleteEntity = id => this.dashboardService.deleteDashboard(id.id);

    config.onEntityAction = action => this.onDashboardAction(action, config, params);

    config.entityAdded = dashboard => {
      this.openDashboard(null, dashboard, config, params);
    };

    if (config.entityGroup.additionalInfo && config.entityGroup.additionalInfo.isPublic) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('dashboard.public-dashboard-link'),
          icon: 'link',
          isEnabled: () => true,
          onAction: ($event, entity) => {
            this.openPublicDashboardLinkDialog($event, entity, config);
          }
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.READ) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.READ)) {
      config.onGroupEntityRowClick = ($event, dashboard) => {
        if (config.isDetailsOpen()) {
          config.onToggleEntityDetails($event, dashboard);
        } else {
          this.openDashboard($event, dashboard, config, params);
        }
      };
    }

    config.cellActionDescriptors.push(
      {
        name: this.translate.instant('dashboard.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => {
          this.exportDashboard($event, entity);
        }
      }
    );

    if (config.settings.detailsMode === EntityGroupDetailsMode.onRowClick &&
      this.userPermissionsService.hasGroupEntityPermission(Operation.READ, config.entityGroup)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('dashboard.dashboard-details'),
          icon: 'edit',
          isEnabled: () => true,
          onAction: ($event, entity) => config.onToggleEntityDetails($event, entity)
        }
      );
    }

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('dashboard.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importDashboard($event, config)
        }
      );
      if (getCurrentAuthState(this.store).aiEnabled &&
        this.userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL)) {
        config.headerButtonDescriptors.push(
          {
            name: this.translate.instant('ai-assistant.configure-with-ai'),
            icon: 'mdi:creation',
            isEnabled: () => true,
            onAction: ($event) => this.generateWithAi($event, config, params)
          }
        );
      }
    }
    return this.groupConfigTableConfigService.prepareConfiguration(params, config);
  }

  openDashboard($event: Event, dashboard: ShortEntityView | Dashboard, config: GroupEntityTableConfig<DashboardInfo>,
                params: EntityGroupParams, queryParams: {[key: string]: string} = null) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.groupType === EntityType.EDGE) {
        url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId, params.customerId,
          'edgeManagement', 'edges', 'groups',
          params.childEntityGroupId, params.edgeId, 'dashboardGroups', params.edgeEntitiesGroupId, dashboard.id.id],
          {queryParams});
      } else {
        url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId,
          params.customerId, 'dashboards', 'groups', params.childEntityGroupId, dashboard.id.id], {queryParams});
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([dashboard.id.id], {relativeTo: config.getActivatedRoute(), queryParams});
      this.router.navigateByUrl(url);
    }
  }

  generateWithAi($event: Event, config: GroupEntityTableConfig<DashboardInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    const dashboard = this.dashboardUtils.validateAndUpdateDashboard({title: 'New Dashboard'} as Dashboard);
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    this.dashboardService.saveDashboard(dashboard, entityGroupId).subscribe((savedDashboard) => {
      this.openDashboard(null, savedDashboard, config, params, {action: 'aiAssistant'});
    });
  }

  exportDashboard($event: Event, dashboard: ShortEntityView | DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportDashboard(dashboard.id.id);
  }

  importDashboard($event: Event, config: GroupEntityTableConfig<DashboardInfo>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.importExport.importDashboard(customerId, this.editMissingAliases.bind(this), entityGroupId).subscribe((res) => {
      if (res) {
        config.updateData();
      }
    });
  }

  private editMissingAliases(widgets: Array<Widget>, isSingleWidget: boolean,
                             customTitle: string, missingEntityAliases: EntityAliases): Observable<EntityAliases> {
    return this.dialog.open<EntityAliasesDialogComponent, EntityAliasesDialogData,
      EntityAliases>(EntityAliasesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityAliases: missingEntityAliases,
        widgets,
        customTitle,
        isSingleWidget,
        disableAdd: true
      }
    }).afterClosed().pipe(
      map((updatedEntityAliases) => {
          if (updatedEntityAliases) {
            return updatedEntityAliases;
          } else {
            throw new Error('Unable to resolve missing entity aliases!');
          }
        }
      ));
  }

  openPublicDashboardLinkDialog($event: Event, dashboard: ShortEntityView | DashboardInfo, config: GroupEntityTableConfig<DashboardInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<PublicDashboardLinkDialogComponent, PublicDashboardLinkDialogData>(
      PublicDashboardLinkDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          dashboard,
          entityGroup: config.entityGroup
        }
      });
  }

  manageOwnerAndGroups($event: Event, dashboard: DashboardInfo, config: GroupEntityTableConfig<DashboardInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, dashboard).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onDashboardAction(action: EntityAction<DashboardInfo>,
                    config: GroupEntityTableConfig<DashboardInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openDashboard(action.event, action.entity, config, params);
        return true;
      case 'export':
        this.exportDashboard(action.event, action.entity);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }

}
