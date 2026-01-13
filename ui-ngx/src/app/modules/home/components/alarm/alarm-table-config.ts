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

import {
  CellActionDescriptorType,
  DateEntityTableColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { TimePageLink } from '@shared/models/page/page-link';
import { forkJoin, Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AlarmAssignee,
  AlarmInfo,
  AlarmQueryV2,
  AlarmSearchStatus,
  alarmSeverityColors,
  alarmSeverityTranslations,
  AlarmsMode,
  alarmStatusTranslations,
  getUserDisplayName,
  getUserInitials
} from '@app/shared/models/alarm.models';
import { AlarmService } from '@app/core/http/alarm.service';
import { DialogService } from '@core/services/dialog.service';
import { AlarmTableHeaderComponent } from '@home/components/alarm/alarm-table-header.component';
import {
  AlarmDetailsDialogComponent,
  AlarmDetailsDialogData
} from '@home/components/alarm/alarm-details-dialog.component';
import { forAllTimeInterval } from '@shared/models/time/time.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { ChangeDetectorRef, Injector, StaticProvider, ViewContainerRef } from '@angular/core';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import {
  ALARM_ASSIGNEE_PANEL_DATA,
  AlarmAssigneePanelComponent,
  AlarmAssigneePanelData
} from '@home/components/alarm/alarm-assignee-panel.component';
import { ComponentPortal } from '@angular/cdk/portal';
import { getEntityDetailsPageURL, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { UtilsService } from '@core/services/utils.service';
import { AlarmFilterConfig } from '@shared/models/query/query.models';
import { EntityService } from '@core/http/entity.service';

export class AlarmTableConfig extends EntityTableConfig<AlarmInfo, TimePageLink> {

  private authUser = getCurrentAuthUser(this.store);

  alarmFilterConfig: AlarmFilterConfig;

  constructor(private alarmService: AlarmService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private alarmsMode: AlarmsMode = AlarmsMode.ALL,
              public entityId: EntityId = null,
              private defaultAlarmFilterConfig: AlarmFilterConfig = {statusList: [AlarmSearchStatus.ACTIVE]},
              private store: Store<AppState>,
              private viewContainerRef: ViewContainerRef,
              private overlay: Overlay,
              private cd: ChangeDetectorRef,
              private utilsService: UtilsService,
              pageMode = false) {
    super();
    this.loadDataOnInit = pageMode;
    this.tableTitle = '';
    this.useTimePageLink = true;
    this.forAllTimeEnabled = true;
    this.pageMode = pageMode;
    this.defaultTimewindowInterval = forAllTimeInterval();
    this.detailsPanelEnabled = false;
    this.selectionEnabled = true;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.actionsColumnTitle = 'alarm.details';
    this.entityType = EntityType.ALARM;
    this.entityTranslations = entityTypeTranslations.get(EntityType.ALARM);
    this.entityResources = {
    } as EntityTypeResource<AlarmInfo>;
    this.alarmFilterConfig = defaultAlarmFilterConfig;

    this.headerComponent = AlarmTableHeaderComponent;

    this.entitiesFetchFunction = pageLink => this.fetchAlarms(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<AlarmInfo>('createdTime', 'alarm.created-time', this.datePipe, '150px'));
    this.columns.push(
      new EntityLinkTableColumn<AlarmInfo>('originatorName', 'alarm.originator', '25%',
        (entity) => this.utilsService.customTranslation(entity.originatorName, entity.originatorName),
        (entity) => getEntityDetailsPageURL(entity.originator.id, entity.originator.entityType as EntityType)));
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('type', 'alarm.type', '25%',
          entity => this.utilsService.customTranslation(entity.type, entity.type)));
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('severity', 'alarm.severity', '25%',
        (entity) => this.translate.instant(alarmSeverityTranslations.get(entity.severity)),
        entity => ({
          fontWeight: 'bold',
          color: alarmSeverityColors.get(entity.severity)
        })));
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('assignee', 'alarm.assignee', '240px',
        (entity) => this.getAssigneeTemplate(entity), () => ({}), false, () => ({}), () => undefined, false,
        {
          icon: 'keyboard_arrow_down',
          type: CellActionDescriptorType.DEFAULT,
          isEnabled: () => true,
          name: this.translate.instant('alarm.assign'),
          onAction: ($event, entity) => this.openAlarmAssigneePanel($event, entity)
        })
    );
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('status', 'alarm.status', '25%',
        (entity) => this.translate.instant(alarmStatusTranslations.get(entity.status))));

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('alarm.details'),
        icon: 'more_horiz',
        isEnabled: () => true,
        onAction: ($event, entity) => this.showAlarmDetails(entity)
      }
    );

    this.groupActionDescriptors.push(
      {
        name: this.translate.instant('alarm.acknowledge'),
        icon: 'done',
        isEnabled: true,
        onAction: ($event, entities) => this.ackAlarms($event, entities)
      },
      {
        name: this.translate.instant('alarm.clear'),
        icon: 'clear',
        isEnabled: true,
        onAction: ($event, entities) => this.clearAlarms($event, entities)
      },
      {
        name: this.translate.instant('alarm.delete'),
        icon: 'delete',
        isEnabled: true,
        onAction: ($event, entities) => this.deleteAlarms($event, entities)
      }
    );
  }

  fetchAlarms(pageLink: TimePageLink): Observable<PageData<AlarmInfo>> {
    const alarmFilter = this.entityService.resolveAlarmFilter(this.alarmFilterConfig, false);
    const query = new AlarmQueryV2(this.entityId, pageLink, alarmFilter);
    switch (this.alarmsMode) {
      case AlarmsMode.ALL:
        return this.alarmService.getAllAlarmsV2(query);
      case AlarmsMode.ENTITY:
        return this.alarmService.getAlarmsV2(query);
    }
  }

  showAlarmDetails(entity: AlarmInfo) {
    const isPermissionWrite = this.authUser.authority !== Authority.CUSTOMER_USER || entity.customerId?.id === this.authUser.customerId;
    this.dialog.open<AlarmDetailsDialogComponent, AlarmDetailsDialogData, boolean>
    (AlarmDetailsDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          alarmId: entity.id.id,
          alarm: entity,
          allowAcknowledgment: isPermissionWrite,
          allowClear: isPermissionWrite,
          displayDetails: true,
          allowAssign: true
        }
      }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.updateData();
        }
      }
    );
  }

  getAssigneeTemplate(entity: AlarmInfo): string {
    const hasAssigneeId = isDefinedAndNotNull(entity.assigneeId);
    let templateContent: string;

    if (hasAssigneeId && ((isNotEmptyStr(entity.assignee?.firstName) || isNotEmptyStr(entity.assignee?.lastName)) ||
      isNotEmptyStr(entity.assignee?.email))) {
      templateContent = `
        <span class="assigned-container">
         <span class="user-avatar" style="background-color: ${this.getAvatarBgColor(entity.assignee)}">
           ${getUserInitials(entity.assignee)}
         </span>
         <span class="user-display-name">${getUserDisplayName(entity.assignee)}</span>
        </span>`;
    } else {
      templateContent = `
        <span class="unassigned-container">
         <mat-icon class="material-icons unassigned-icon">
           ${hasAssigneeId ? 'no_accounts' : 'account_circle'}
         </mat-icon>
         <span>${this.translate.instant(hasAssigneeId ? 'alarm.user-deleted' : 'alarm.unassigned')}</span>
        </span>`;
    }
    return `<span class="assignee-cell">${templateContent}</span>`;
  }

  getAvatarBgColor(alarmAssignee: AlarmAssignee) {
    return this.utilsService.stringToHslColor(getUserDisplayName(alarmAssignee), 40, 60);
  }

  openAlarmAssigneePanel($event: Event, entity: AlarmInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig();
    config.backdropClass = 'cdk-overlay-transparent-backdrop';
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'end',
      originY: 'bottom',
      overlayX: 'end',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);
    config.minWidth = '260px';
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const providers: StaticProvider[] = [
      {
        provide: ALARM_ASSIGNEE_PANEL_DATA,
        useValue: {
          alarmId: entity.id.id,
          assigneeId: entity.assigneeId?.id
        } as AlarmAssigneePanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(AlarmAssigneePanelComponent,
      this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.reassigned) {
        this.updateData();
      }
    });
  }

  ackAlarms($event: Event, alarms: Array<AlarmInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const unacknowledgedAlarms = alarms.filter(alarm => !alarm.acknowledged);
    let title = '';
    let content = '';
    if (!unacknowledgedAlarms.length) {
      title = this.translate.instant('alarm.selected-alarms', {count: alarms.length});
      content = this.translate.instant('alarm.selected-alarms-are-acknowledged');
      this.dialogService.alert(
        title,
        content).subscribe();
    } else {
      title = this.translate.instant('alarm.aknowledge-alarms-title', {count: unacknowledgedAlarms.length});
      content = this.translate.instant('alarm.aknowledge-alarms-text', {count: unacknowledgedAlarms.length});
      this.dialogService.confirm(
        title,
        content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')
      ).subscribe((res) => {
        if (res) {
          const tasks: Observable<AlarmInfo>[] = [];
          for (const alarm of unacknowledgedAlarms) {
            tasks.push(this.alarmService.ackAlarm(alarm.id.id));
          }
          forkJoin(tasks).subscribe(() => {
            this.updateData();
          });
        }
      });
    }
  }

  clearAlarms($event: Event, alarms: Array<AlarmInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const activeAlarms = alarms.filter(alarm => !alarm.cleared);
    let title = '';
    let content = '';
    if (!activeAlarms.length) {
      title = this.translate.instant('alarm.selected-alarms', {count: alarms.length});
      content = this.translate.instant('alarm.selected-alarms-are-cleared');
      this.dialogService.alert(
        title,
        content
        ).subscribe();
    } else {
      title = this.translate.instant('alarm.clear-alarms-title', {count: activeAlarms.length});
      content = this.translate.instant('alarm.clear-alarms-text', {count: activeAlarms.length});
      this.dialogService.confirm(
        title,
        content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')
      ).subscribe((res) => {
        if (res) {
          const tasks: Observable<AlarmInfo>[] = [];
          for (const alarm of activeAlarms) {
            tasks.push(this.alarmService.clearAlarm(alarm.id.id));
          }
          forkJoin(tasks).subscribe(() => {
            this.updateData();
          });
        }
      });
    }
  }

  deleteAlarms($event: Event, alarms: Array<AlarmInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('alarm.delete-alarms-title', {count: alarms.length});
    const content = this.translate.instant('alarm.delete-alarms-text', {count: alarms.length});
    this.dialogService.confirm(
      title,
      content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((res) => {
      if (res) {
        const tasks: Observable<boolean>[] = [];
        for (const alarm of alarms) {
          tasks.push(this.alarmService.deleteAlarm(alarm.id.id));
        }
        forkJoin(tasks).subscribe(() => {
          this.updateData();
        });
      }
    });
  }

}
