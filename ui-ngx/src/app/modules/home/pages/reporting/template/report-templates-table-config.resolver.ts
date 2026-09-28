// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import {
  ReportConfig,
  ReportTemplate,
  ReportTemplateFilter,
  ReportTemplateInfo,
  ReportTemplateQuery,
  ReportTemplateType,
  reportTemplateTypeTranslationMap
} from '@shared/models/report.models';
import { ReportTemplateService } from '@core/http/report-template.service';
import { mergeMap } from 'rxjs/operators';
import { UtilsService } from '@core/services/utils.service';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ReportTemplateTableHeaderComponent } from '@home/pages/reporting/template/report-template-table-header.component';
import { ReportTemplateTabsComponent } from '@home/pages/reporting/template/report-template-tabs.component';
import { ReportTemplateFormComponent } from '@home/pages/reporting/template/report-template-form.component';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { SchedulerEvent, SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import {
  SchedulerEventDialogComponent,
  SchedulerEventDialogData
} from '@home/components/scheduler/scheduler-event-dialog.component';
import { defaultSchedulerEventConfigTypes } from '@home/components/scheduler/scheduler-event-config.models';
import { MatDialog } from '@angular/material/dialog';
import { Operation, Resource } from '@shared/models/security.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { UserId } from '@shared/models/id/user-id';
import { ReportService } from '@core/http/report.service';
import {
  GenerateReportDialogComponent,
  GenerateReportDialogData
} from '@home/pages/reporting/template/generate-report-dialog.component';

@Injectable()
export class ReportTemplatesTableConfigResolver  {

  constructor(private store: Store<AppState>,
              private reportTemplateService: ReportTemplateService,
              private reportService: ReportService,
              private importExport: ImportExportService,
              private userPermissionsService: UserPermissionsService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private utils: UtilsService,
              private router: Router,
              private datePipe: DatePipe) {}

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<ReportTemplateInfo> {
    const config = new EntityTableConfig<ReportTemplateInfo>();
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      reportTemplateFilter: {
        typeList: null,
        formatList: null
      },
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      },
      reportTemplateFilterChanged: (filter: ReportTemplateFilter) => {
        config.componentsData.reportTemplateFilter = filter;
        config.getTable().resetSortAndFilter(true);
      }
    };
    config.handleRowClick = ($event, reportTemplate) => {
      if (config.isDetailsOpen()) {
        config.toggleEntityDetails($event, reportTemplate);
      } else {
        this.openReportTemplate($event, reportTemplate, config);
      }
      return true;
    };

    config.columns = this.configureColumns(authUser, config);
    this.configureEntityFunctions(config);
    config.cellActionDescriptors = this.configureCellActions(config);
    config.groupActionDescriptors = this.configureGroupActions(config);
    config.addActionDescriptors = this.configureAddActions(config);

    defaultEntityTablePermissions(this.userPermissionsService, config);
    return config;
  }

  configDefaults(config: EntityTableConfig<ReportTemplateInfo>) {
    config.entityType = EntityType.REPORT_TEMPLATE;
    config.entityComponent = ReportTemplateFormComponent;
    config.entityTabsComponent = ReportTemplateTabsComponent;
    config.entityTranslations = entityTypeTranslations.get(EntityType.REPORT_TEMPLATE);
    config.entityResources = entityTypeResources.get(EntityType.REPORT_TEMPLATE);

    config.entityTitle = (reportTemplate) => reportTemplate ?
      this.utils.customTranslation(reportTemplate.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = reportTemplate =>
      this.translate.instant('report-template.delete-report-template-title', {reportTemplateTitle: reportTemplate.name});
    config.deleteEntityContent = () => this.translate.instant('report-template.delete-report-template-text');
    config.deleteEntitiesTitle = count => this.translate.instant('report-template.delete-report-templates-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('report-template.delete-report-templates-text');

    config.loadEntity = id => this.reportTemplateService.getReportTemplate(id.id);
    config.saveEntity = reportTemplate => this.reportTemplateService.saveReportTemplate(reportTemplate as ReportTemplate).pipe(
      mergeMap((savedReportTemplate) => this.reportTemplateService.getReportTemplate(savedReportTemplate.id.id))
    );
    config.onEntityAction = action => this.onReportTemplateAction(action, config);
    config.headerComponent = ReportTemplateTableHeaderComponent;
    config.entityAdded = reportTemplate => {
      this.openReportTemplate(null, reportTemplate, config);
    };
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<ReportTemplateInfo>): Array<EntityColumn<ReportTemplateInfo>> {
    const columns: Array<EntityColumn<ReportTemplateInfo>> = [
      new DateEntityTableColumn<ReportTemplateInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<ReportTemplateInfo>('name', 'report-template.name',
        config.componentsData.includeCustomers ? '30%' : '60%', config.entityTitle),
      new EntityTableColumn<ReportTemplateInfo>( 'type', 'report-template.type', '20%', entity => {
        return this.translate.instant(reportTemplateTypeTranslationMap.get(entity.type))
      }),
      new EntityTableColumn<ReportTemplateInfo>( 'format', 'report-template.format', '20%')
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<ReportTemplateInfo>('ownerName', title, '30%'));
    }
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<ReportTemplateInfo>): void {
    config.entitiesFetchFunction = pageLink => {
      const reportTemplateQuery = new ReportTemplateQuery(pageLink, {
        typeList: config.componentsData.reportTemplateFilter.typeList,
        formatList: config.componentsData.reportTemplateFilter.formatList,
        includeCustomers: config.componentsData.includeCustomers
      });
      return this.reportTemplateService.getAllReportTemplateInfos(reportTemplateQuery);
    };

    config.deleteEntity = id => this.reportTemplateService.deleteReportTemplate(id.id);
  }

  configureCellActions(config: EntityTableConfig<ReportTemplateInfo>): Array<CellActionDescriptor<ReportTemplateInfo>> {
    const actions: Array<CellActionDescriptor<ReportTemplateInfo>> = [];
    if (this.userPermissionsService.hasGenericPermission(Resource.JOB, Operation.CREATE)) {
      actions.push(
        {
          name: this.translate.instant('report.generate-report'),
          icon: 'mdi:file-plus-outline',
          isEnabled: (reportTemplate) => reportTemplate.type === ReportTemplateType.REPORT,
          onAction: ($event, entity) => this.generateReport($event, entity)
        },
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.CREATE)) {
      actions.push(
        {
          name: this.translate.instant('scheduled-report.schedule-report'),
          icon: 'mdi:file-clock-outline',
          isEnabled: (reportTemplate) => reportTemplate.type === ReportTemplateType.REPORT,
          onAction: ($event, entity) => this.scheduleReport($event, entity)
        },
      );
    }
    actions.push(
      {
        name: this.translate.instant('report-template.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportReportTemplate($event, entity)
      },
    );
    actions.push(
      {
        name: this.translate.instant('report-template.report-template-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => config.toggleEntityDetails($event, entity)
      }
    );
    return actions;
  }

  configureGroupActions(_config: EntityTableConfig<ReportTemplateInfo>): Array<GroupActionDescriptor<ReportTemplateInfo>> {
    return [];
  }

  configureAddActions(config: EntityTableConfig<ReportTemplateInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('report-template.create-new-report-template'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('report-template.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importReportTemplate($event, config)
      }
    );
    return actions;
  }

  openReportTemplate($event: Event, reportTemplate: ReportTemplateInfo, config: EntityTableConfig<ReportTemplateInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([reportTemplate.id.id], {relativeTo: config.getTable().route});
    this.router.navigateByUrl(url).then(() => {});
  }

  importReportTemplate(_$event: Event, config: EntityTableConfig<ReportTemplateInfo>) {
    this.importExport.importReportTemplate().subscribe(
      {
        next: reportTemplate => {
          config.entityAdded(reportTemplate);
        }
      }
    );
  }

  exportReportTemplate($event: Event, reportTemplate: ReportTemplateInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportReportTemplate(reportTemplate.id.id);
  }

  private generateReport($event: Event, reportTemplate: ReportTemplateInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<GenerateReportDialogComponent, GenerateReportDialogData, boolean>(GenerateReportDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        reportTemplateId: reportTemplate.id
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('report.generate-report-started'),
          type: 'success',
          duration: 5000
        }));
        this.router.navigate(['/reporting/reports']);
      }
    });
  }

  private scheduleReport($event: Event, reportTemplate: ReportTemplateInfo) {
    if ($event) {
      $event.stopPropagation();
    }

    const authUser = getCurrentAuthUser(this.store);

    const reportConfig:  Partial<ReportConfig> & SchedulerEventConfiguration = {
      reportTemplateId: reportTemplate.id,
      userId: new UserId(authUser.userId),
      timezone: getDefaultTimezone()
    };

    const scheduledReport: SchedulerEvent = {
      name: null,
      type: 'generateReport',
      schedule: null,
      configuration: reportConfig
    };

    return this.dialog.open<SchedulerEventDialogComponent, SchedulerEventDialogData, boolean>(SchedulerEventDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        schedulerEventConfigTypes: {generateReport: defaultSchedulerEventConfigTypes['generateReport']},
        isAdd: true,
        readonly: false,
        schedulerEvent: scheduledReport,
        defaultEventType: 'generateReport'
      }
    });
  }

  onReportTemplateAction(action: EntityAction<ReportTemplateInfo>, config: EntityTableConfig<ReportTemplateInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openReportTemplate(action.event, action.entity, config);
        return true;
      case 'export':
        this.exportReportTemplate(action.event, action.entity);
        return true;
      case 'generateReport':
        this.generateReport(action.event, action.entity);
        return true;
      case 'scheduleReport':
        this.scheduleReport(action.event, action.entity);
        return true;
    }
    return false;
  }


}
