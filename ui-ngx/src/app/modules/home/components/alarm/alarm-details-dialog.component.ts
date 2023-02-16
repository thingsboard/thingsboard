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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, ReplaySubject } from 'rxjs';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  AlarmInfo,
  alarmSeverityColors,
  alarmSeverityTranslations,
  AlarmStatus,
  alarmStatusTranslations
} from '@app/shared/models/alarm.models';
import { AlarmService } from '@core/http/alarm.service';
import { tap } from 'rxjs/operators';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';

export interface AlarmDetailsDialogData {
  alarmId?: string;
  alarm?: AlarmInfo;
  allowAcknowledgment: boolean;
  allowClear: boolean;
  displayDetails: boolean;
}

@Component({
  selector: 'tb-alarm-details-dialog',
  templateUrl: './alarm-details-dialog.component.html',
  styleUrls: ['./alarm-details-dialog.component.scss']
})
export class AlarmDetailsDialogComponent extends DialogComponent<AlarmDetailsDialogComponent, boolean> implements OnInit {

  alarmId: string;
  alarmFormGroup: UntypedFormGroup;

  allowAcknowledgment: boolean;
  allowClear: boolean;
  displayDetails: boolean;

  loadAlarmSubject = new ReplaySubject<AlarmInfo>();
  alarm$: Observable<AlarmInfo> = this.loadAlarmSubject.asObservable().pipe(
    tap(alarm => this.loadAlarmFields(alarm))
  );

  alarmSeverityColorsMap = alarmSeverityColors;
  alarmStatuses = AlarmStatus;

  alarmUpdated = false;

  assigneeInitials = '';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private datePipe: DatePipe,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AlarmDetailsDialogData,
              private alarmService: AlarmService,
              public dialogRef: MatDialogRef<AlarmDetailsDialogComponent, boolean>,
              public fb: UntypedFormBuilder,
              private utilsService: UtilsService) {
    super(store, router, dialogRef);

    this.allowAcknowledgment = data.allowAcknowledgment;
    this.allowClear = data.allowClear;
    this.displayDetails = data.displayDetails;

    this.alarmFormGroup = this.fb.group(
      {
        createdTime: [''],
        originatorName: [''],
        assigneeFirstName: [''],
        assigneeLastName: [''],
        assigneeEmail: [''],
        assigneeId: [''],
        startTime: [''],
        endTime: [''],
        ackTime: [''],
        clearTime: [''],
        assignTime: [''],
        type: [''],
        alarmSeverity: [''],
        assignee: [''],
        alarmStatus: [''],
        alarmDetails: [null]
      }
    );

    if (!this.data.alarm) {
      this.alarmId = this.data.alarmId;
      this.loadAlarm();
    } else {
      this.alarmId = this.data.alarm?.id?.id;
      this.loadAlarmSubject.next(this.data.alarm);
    }
  }

  loadAlarm() {
    this.alarmService.getAlarmInfo(this.alarmId).subscribe(
      alarm => this.loadAlarmSubject.next(alarm)
    );
  }

  loadAlarmFields(alarm: AlarmInfo) {
    this.alarmFormGroup.get('createdTime')
      .patchValue(this.datePipe.transform(alarm.createdTime, 'yyyy-MM-dd HH:mm:ss'));
    this.alarmFormGroup.get('originatorName')
      .patchValue(alarm.originatorName);
    if(alarm.assigneeFirstName) {
      this.alarmFormGroup.get('assigneeFirstName')
        .patchValue(alarm.assigneeFirstName);
    }
    if(alarm.assigneeLastName) {
      this.alarmFormGroup.get('assigneeLastName')
        .patchValue(alarm.assigneeLastName);
    }
    if(alarm.assigneeEmail) {
      this.alarmFormGroup.get('assigneeEmail')
        .patchValue(alarm.assigneeEmail);
    }
    if(alarm.assigneeId) {
      this.alarmFormGroup.get('assigneeId')
        .patchValue(alarm.assigneeId.id);
    }
    if (alarm.startTs) {
      this.alarmFormGroup.get('startTime')
        .patchValue(this.datePipe.transform(alarm.startTs, 'yyyy-MM-dd HH:mm:ss'));
    }
    if (alarm.endTs) {
      this.alarmFormGroup.get('endTime')
        .patchValue(this.datePipe.transform(alarm.endTs, 'yyyy-MM-dd HH:mm:ss'));
    }
    if (alarm.ackTs) {
      this.alarmFormGroup.get('ackTime')
        .patchValue(this.datePipe.transform(alarm.ackTs, 'yyyy-MM-dd HH:mm:ss'));
    }
    if (alarm.clearTs) {
      this.alarmFormGroup.get('clearTime')
        .patchValue(this.datePipe.transform(alarm.clearTs, 'yyyy-MM-dd HH:mm:ss'));
    }
    if (alarm.assignTs) {
      this.alarmFormGroup.get('assignTime')
        .patchValue(this.datePipe.transform(alarm.assignTs, 'yyyy-MM-dd HH:mm:ss'));
    }
    this.alarmFormGroup.get('type').patchValue(alarm.type);
    this.alarmFormGroup.get('alarmSeverity')
      .patchValue(this.translate.instant(alarmSeverityTranslations.get(alarm.severity)));
    this.alarmFormGroup.get('alarmStatus')
      .patchValue(this.translate.instant(alarmStatusTranslations.get(alarm.status)));
    if (alarm.assigneeId) {
      this.alarmFormGroup.get('assignee').
      patchValue(this.getUserDisplayName(alarm));
      this.assigneeInitials = this.getUserInitials(alarm);
    }
    this.alarmFormGroup.get('alarmDetails').patchValue(alarm.details);
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close(this.alarmUpdated);
  }

  acknowledge(): void {
    if (this.alarmId) {
      this.alarmService.ackAlarm(this.alarmId).subscribe(
        () => {
          this.alarmUpdated = true;
          this.loadAlarm();
        }
      );
    }
  }

  clear(): void {
    if (this.alarmId) {
      this.alarmService.clearAlarm(this.alarmId).subscribe(
        () => {
          this.alarmUpdated = true;
          this.loadAlarm();
        }
      );
    }
  }

  getColorFromString(userDisplayName: string) {
    return this.utilsService.stringToHslColor(userDisplayName, 40, 60);
  }

  getUserDisplayName(entity: AlarmInfo) {
    let displayName = '';
    if ((entity.assigneeFirstName && entity.assigneeFirstName.length > 0) ||
      (entity.assigneeLastName && entity.assigneeLastName.length > 0)) {
      if (entity.assigneeFirstName) {
        displayName += entity.assigneeFirstName;
      }
      if (entity.assigneeLastName) {
        if (displayName.length > 0) {
          displayName += ' ';
        }
        displayName += entity.assigneeLastName;
      }
    } else {
      displayName = entity.assigneeEmail;
    }
    return displayName;
  }

  getUserInitials(entity: AlarmInfo): string {
    let initials = '';
    if (entity.assigneeFirstName && entity.assigneeFirstName.length ||
      entity.assigneeLastName && entity.assigneeLastName.length) {
      if (entity.assigneeFirstName) {
        initials += entity.assigneeFirstName.charAt(0);
      }
      if (entity.assigneeLastName) {
        initials += entity.assigneeLastName.charAt(0);
      }
    } else {
      initials += entity.assigneeEmail.charAt(0);
    }
    return initials.toUpperCase();
  }

}
