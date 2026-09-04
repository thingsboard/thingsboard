// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Injector, Input, Output, StaticProvider, ViewContainerRef } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { AlarmAssignee, AlarmInfo, getUserDisplayName, getUserInitials } from '@shared/models/alarm.models';
import {
  ALARM_ASSIGNEE_PANEL_DATA,
  AlarmAssigneePanelComponent,
  AlarmAssigneePanelData
} from '@home/components/alarm/alarm-assignee-panel.component';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { isNotEmptyStr } from '@core/utils';

@Component({
    selector: 'tb-alarm-assignee',
    templateUrl: './alarm-assignee.component.html',
    styleUrls: ['./alarm-assignee.component.scss'],
    standalone: false
})
export class AlarmAssigneeComponent {
  @Input()
  alarm: AlarmInfo;

  @Input()
  allowAssign: boolean;

  @Output()
  alarmReassigned = new EventEmitter<boolean>();

  userAssigned: boolean;

  constructor(private utilsService: UtilsService,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private translateService: TranslateService) {
  }

  getAssignee() {
    if (this.alarm) {
      this.userAssigned = this.alarm.assigneeId && ((isNotEmptyStr(this.alarm.assignee?.firstName) ||
        isNotEmptyStr(this.alarm.assignee?.lastName)) || isNotEmptyStr(this.alarm.assignee?.email));
      if (this.userAssigned) {
        return getUserDisplayName(this.alarm.assignee);
      } else {
        return this.translateService.instant(this.alarm.assigneeId ? 'alarm.user-deleted' : 'alarm.unassigned');
      }
    }
  }

  getUserInitials(alarmAssignee: AlarmAssignee): string {
    return getUserInitials(alarmAssignee);
  }

  getAvatarBgColor(entity: AlarmAssignee) {
    return this.utilsService.stringToHslColor(getUserDisplayName(entity), 40, 60);
  }

  openAlarmAssigneePanel($event: Event, alarm: AlarmInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.allowAssign) {
      const target = $event.currentTarget;
      const config = new OverlayConfig();
      config.backdropClass = 'cdk-overlay-transparent-backdrop';
      config.hasBackdrop = true;
      const connectedPosition: ConnectedPosition = {
        originX: 'center',
        originY: 'bottom',
        overlayX: 'center',
        overlayY: 'top'
      };
      config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
        .withPositions([connectedPosition]);
      config.width = (target as HTMLElement).offsetWidth;
      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });
      const providers: StaticProvider[] = [
        {
          provide: ALARM_ASSIGNEE_PANEL_DATA,
          useValue: {
            alarmId: alarm.id.id,
            assigneeId: alarm.assigneeId?.id
          } as AlarmAssigneePanelData
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      overlayRef.attach(new ComponentPortal(AlarmAssigneePanelComponent,
        this.viewContainerRef, injector)).onDestroy(() => this.alarmReassigned.emit(true));
    }
  }

}
