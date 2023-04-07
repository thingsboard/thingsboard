///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, EventEmitter, Injector, Input, Output, StaticProvider, ViewContainerRef } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { AlarmAssignee, AlarmInfo } from '@shared/models/alarm.models';
import {
  ALARM_ASSIGNEE_PANEL_DATA,
  AlarmAssigneePanelComponent,
  AlarmAssigneePanelData
} from '@home/components/alarm/alarm-assignee-panel.component';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-alarm-assignee',
  templateUrl: './alarm-assignee.component.html',
  styleUrls: ['./alarm-assignee.component.scss']
})
export class AlarmAssigneeComponent {
  @Input()
  alarm: AlarmInfo;

  @Input()
  allowAssign: boolean;

  @Output()
  alarmReassigned = new EventEmitter<boolean>();

  constructor(private utilsService: UtilsService,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private translateService: TranslateService) {
  }

  getAssignee() {
    if (this.alarm) {
      if (this.alarm.assignee) {
        return this.getUserDisplayName(this.alarm.assignee);
      } else {
        return this.translateService.instant('alarm.unassigned');
      }
    }
  }

  getUserDisplayName(entity: AlarmAssignee) {
    let displayName = '';
    if ((entity.firstName && entity.firstName.length > 0) ||
      (entity.lastName && entity.lastName.length > 0)) {
      if (entity.firstName) {
        displayName += entity.firstName;
      }
      if (entity.lastName) {
        if (displayName.length > 0) {
          displayName += ' ';
        }
        displayName += entity.lastName;
      }
    } else {
      displayName = entity.email;
    }
    return displayName;
  }

  getUserInitials(entity: AlarmAssignee): string {
    let initials = '';
    if (entity.firstName && entity.firstName.length ||
      entity.lastName && entity.lastName.length) {
      if (entity.firstName) {
        initials += entity.firstName.charAt(0);
      }
      if (entity.lastName) {
        initials += entity.lastName.charAt(0);
      }
    } else {
      initials += entity.email.charAt(0);
    }
    return initials.toUpperCase();
  }

  getFullName(entity: AlarmAssignee): string {
    let fullName = '';
    if ((entity.firstName && entity.firstName.length > 0) ||
      (entity.lastName && entity.lastName.length > 0)) {
      if (entity.firstName) {
        fullName += entity.firstName;
      }
      if (entity.lastName) {
        if (fullName.length > 0) {
          fullName += ' ';
        }
        fullName += entity.lastName;
      }
    }
    return fullName;
  }

  getAvatarBgColor(entity: AlarmAssignee) {
    return this.utilsService.stringToHslColor(this.getUserDisplayName(entity), 40, 60);
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
