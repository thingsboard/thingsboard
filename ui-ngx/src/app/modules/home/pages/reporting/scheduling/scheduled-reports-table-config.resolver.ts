// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { ReportFilter, ReportQuery, ScheduledReportInfo } from '@shared/models/report.models';
import { map } from 'rxjs/operators';
import { UtilsService } from '@core/services/utils.service';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import {
  ScheduledReportTableHeaderComponent
} from '@home/pages/reporting/scheduling/scheduled-report-table-header.component';
import { Operation, Resource } from '@shared/models/security.models';
import { SchedulerEvent } from '@shared/models/scheduler-event.models';
import {
  SchedulerEventDialogComponent,
  SchedulerEventDialogData
} from '@home/components/scheduler/scheduler-event-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { defaultSchedulerEventConfigTypes } from '@home/components/scheduler/scheduler-event-config.models';
import { Observable } from 'rxjs';
import { getEntityDetailsPageURL } from '@core/utils';
import { scheduleInfo } from '@home/components/scheduler/scheduler-events.models';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { TbPopoverService } from '@shared/components/popover.service';

@Injectable()
export class ScheduledReportsTableConfigResolver  {

  constructor(private store: Store<AppState>,
              private schedulerEventService: SchedulerEventService,
              private userPermissionsService: UserPermissionsService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private popoverService: TbPopoverService) {}

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<ScheduledReportInfo> {
    const config = new EntityTableConfig<ScheduledReportInfo>();
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      reportFilter: {
        reportTemplateId: null,
        userId: null
      },
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      },
      reportFilterChanged: (filter: ReportFilter) => {
        config.componentsData.reportFilter = filter;
        config.getTable().resetSortAndFilter(true);
      }
    };

    config.columns = this.configureColumns(authUser, config);
    this.configureEntityFunctions(config);
    config.cellActionDescriptors = this.configureCellActions(config);
    config.groupActionDescriptors = this.configureGroupActions(config);

    defaultEntityTablePermissions(this.userPermissionsService, config);
    return config;
  }

  private configDefaults(config: EntityTableConfig<ScheduledReportInfo>) {
    config.entityType = EntityType.SCHEDULER_EVENT;
    config.entityTranslations = {
      add: 'scheduled-report.schedule-report',
      search: 'scheduled-report.search',
      noEntities: 'scheduled-report.no-scheduled-reports-text',
      selectedEntities: 'scheduled-report.selected-scheduled-reports'
    };
    config.entityResources = {
      helpLinkId: 'scheduledReports'
    };

    config.entityTitle = (scheduledReport) => scheduledReport ?
      this.utils.customTranslation(scheduledReport.name) : '';

    config.rowPointer = false;

    config.deleteEntityTitle = scheduledReport =>
      this.translate.instant('scheduled-report.delete-scheduled-report-title', {scheduledReportTitle: scheduledReport.name});
    config.deleteEntityContent = () => this.translate.instant('scheduled-report.delete-scheduled-report-text');
    config.deleteEntitiesTitle = count => this.translate.instant('scheduled-report.delete-scheduled-reports-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('scheduled-report.delete-scheduled-reports-text');

    config.onEntityAction = action => this.onScheduledReportAction(action, config);
    config.addEntity = () => this.addScheduledReport().pipe(
      map((res) => {
        return res ? {} as ScheduledReportInfo : null
      })
    );
    config.detailsPanelEnabled = false;
    config.headerComponent = ScheduledReportTableHeaderComponent;
  }

  private configureColumns(authUser: AuthUser, config: EntityTableConfig<ScheduledReportInfo>): Array<EntityColumn<ScheduledReportInfo>> {
    const columns: Array<EntityColumn<ScheduledReportInfo>> = [
      new DateEntityTableColumn<ScheduledReportInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<ScheduledReportInfo>('name', 'scheduled-report.name',
        config.componentsData.includeCustomers ? '20%' : '25%', config.entityTitle),
      new EntityLinkTableColumn<ScheduledReportInfo>('reportTemplateName', 'report-template.report-template',
        config.componentsData.includeCustomers ? '20%' : '25%',
        (scheduledReport) => scheduledReport.templateInfo?.name ?? '',
        (scheduledReport) => scheduledReport.templateInfo ? getEntityDetailsPageURL(scheduledReport.templateInfo.id.id, EntityType.REPORT_TEMPLATE) : ''),
      new EntityTableColumn<ScheduledReportInfo>('userName', 'user.user',
        config.componentsData.includeCustomers ? '20%' : '25%', (scheduledReport) => scheduledReport.userName),
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<ScheduledReportInfo>('customerTitle', title, '20%'));
    }
    columns.push(new EntityTableColumn<ScheduledReportInfo>('schedule', 'scheduled-report.schedule',
      config.componentsData.includeCustomers ? '20%' : '25%', (scheduledReport) => scheduleInfo(scheduledReport.schedule, this.translate),
      () => ({}), false));
    return columns;
  }

  private configureEntityFunctions(config: EntityTableConfig<ScheduledReportInfo>): void {
    config.entitiesFetchFunction = pageLink => {
      const reportQuery = new ReportQuery(pageLink, {
        reportTemplateId: config.componentsData.reportFilter.reportTemplateId,
        userId: config.componentsData.reportFilter.userId,
        includeCustomers: config.componentsData.includeCustomers
      });
      return this.schedulerEventService.getScheduledReports(reportQuery);
    };

    config.deleteEntity = id => this.schedulerEventService.deleteSchedulerEvent(id.id);
  }

  private configureCellActions(config: EntityTableConfig<ScheduledReportInfo>): Array<CellActionDescriptor<ScheduledReportInfo>> {
    const actions: Array<CellActionDescriptor<ScheduledReportInfo>> = [];
    actions.push(
      {
        name: '',
        nameFunction: (scheduledReport) =>
          scheduledReport.enabled ? this.translate.instant('scheduler.disable') : this.translate.instant('scheduler.enable'),
        iconFunction: (scheduledReport) =>
          scheduledReport.enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
        isEnabled: () => this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.WRITE),
        onAction: ($event, entity) => this.enableScheduledReport($event, config, entity)
      },
    );
    actions.push(
      {
        name: this.translate.instant('scheduled-report.edit'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.editScheduledReport($event, config, entity)
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
      actions.push({
        name: this.translate.instant('version-control.version-control'),
        icon: 'history',
        isEnabled: () => true,
        onAction: ($event, entity) => this.toggleVersionControl($event, config, entity)
      })
    }
    return actions;
  }

  private configureGroupActions(_config: EntityTableConfig<ScheduledReportInfo>): Array<GroupActionDescriptor<ScheduledReportInfo>> {
    return [];
  }

  private enableScheduledReport($event: Event, config: EntityTableConfig<ScheduledReportInfo>, scheduledReport: ScheduledReportInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    scheduledReport.enabled = !scheduledReport.enabled;
    this.schedulerEventService.updateSchedulerStatus(scheduledReport.id.id, scheduledReport.enabled, {ignoreLoading: true})
    .subscribe(() => {
      config.updateData();
    });
  }

  private addScheduledReport(): Observable<boolean> {
    return this.openScheduledReportDialog(null);
  }

  private editScheduledReport($event: Event, config: EntityTableConfig<ScheduledReportInfo>, scheduledReport: ScheduledReportInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.schedulerEventService.getSchedulerEvent(scheduledReport.id.id).subscribe({
      next: (schedulerEvent) => {
        this.openScheduledReportDialog($event, schedulerEvent, !this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.WRITE)).subscribe(
          {
            next: (res) => {
              if (res) {
                config.updateData();
              }
            }
          }
        );
      }
    });
  }

  private openScheduledReportDialog($event: Event, scheduledReport?: SchedulerEvent, readonly = false): Observable<boolean> {
    if ($event) {
      $event.stopPropagation();
    }
    let isAdd = false;
    if (!scheduledReport || !scheduledReport.id) {
      isAdd = true;
      if (!scheduledReport) {
        scheduledReport = {
          name: null,
          type: 'generateReport',
          schedule: null,
          configuration: null
        };
      }
    }
    return this.dialog.open<SchedulerEventDialogComponent, SchedulerEventDialogData, boolean>(SchedulerEventDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        schedulerEventConfigTypes: {generateReport: defaultSchedulerEventConfigTypes['generateReport']},
        isAdd,
        readonly,
        schedulerEvent: scheduledReport,
        defaultEventType: 'generateReport'
      }
    }).afterClosed();
  }

  private toggleVersionControl($event: Event, config: EntityTableConfig<ScheduledReportInfo>, scheduledReport: ScheduledReportInfo): void {
    $event?.stopPropagation();
    const trigger = $event.target as HTMLElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const versionControlPopover = this.popoverService.displayPopover({
        trigger,
        renderer: config.getTable().renderer,
        hostView: config.getTable().viewContainerRef,
        componentType: VersionControlComponent,
        preferredPlacement: ['left', 'leftTop', 'leftBottom'],
        context: {
          detailsMode: true,
          active: true,
          singleEntityMode: true,
          externalEntityId: scheduledReport.externalId || scheduledReport.id,
          entityId: scheduledReport.id,
          entityName: scheduledReport.name
        }
      });
      versionControlPopover.tbComponentRef.instance.popoverComponent = versionControlPopover;
      versionControlPopover.tbComponentRef.instance.versionRestored.subscribe(() => {
        versionControlPopover.hide();
        config.updateData();
      });
    }
  }

  onScheduledReportAction(_action: EntityAction<ScheduledReportInfo>, _config: EntityTableConfig<ScheduledReportInfo>): boolean {
    return false;
  }


}
