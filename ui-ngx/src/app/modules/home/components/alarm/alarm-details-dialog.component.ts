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

import { Component, Inject, ViewChild } from '@angular/core';
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
import { AlarmCommentComponent } from '@home/components/alarm/alarm-comment.component';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { UtilsService } from '@core/services/utils.service';

export interface AlarmDetailsDialogData {
  alarmId?: string;
  alarm?: AlarmInfo;
  allowAcknowledgment: boolean;
  allowClear: boolean;
  displayDetails: boolean;
  allowAssign: boolean;
}

@Component({
    selector: 'tb-alarm-details-dialog',
    templateUrl: './alarm-details-dialog.component.html',
    styleUrls: ['./alarm-details-dialog.component.scss'],
    standalone: false
})
export class AlarmDetailsDialogComponent extends DialogComponent<AlarmDetailsDialogComponent, boolean> {

  alarmId: string;
  alarmFormGroup: UntypedFormGroup;

  allowAcknowledgment: boolean;
  allowClear: boolean;
  displayDetails: boolean;
  allowAssign: boolean;

  loadAlarmSubject = new ReplaySubject<AlarmInfo>();
  alarm$: Observable<AlarmInfo> = this.loadAlarmSubject.asObservable().pipe(
    tap(alarm => this.loadAlarmFields(alarm))
  );

  alarmSeverityColorsMap = alarmSeverityColors;
  alarmStatuses = AlarmStatus;

  alarmUpdated = false;

  @ViewChild('alarmCommentComponent', { static: true }) alarmCommentComponent: AlarmCommentComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private datePipe: DatePipe,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AlarmDetailsDialogData,
              private alarmService: AlarmService,
              private utils: UtilsService,
              public dialogRef: MatDialogRef<AlarmDetailsDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.allowAcknowledgment = data.allowAcknowledgment;
    this.allowClear = data.allowClear;
    this.displayDetails = data.displayDetails;
    this.allowAssign = data.allowAssign;

    this.alarmFormGroup = this.fb.group(
      {
        originatorName: [''],
        alarmSeverity: [''],
        startTime: [''],
        duration: [''],
        type: [''],
        alarmStatus: [''],
        alarmDetails: [null]
      }
    );

    if (!this.data.alarm) {
      this.alarmId = this.data.alarmId;
      this.loadAlarm();
    } else {
      this.alarmId = this.data.alarm?.id?.id;
      setTimeout(() => {
        this.loadAlarmSubject.next(this.data.alarm);
      }, 0);
    }
  }

  loadAlarm() {
    this.alarmService.getAlarmInfo(this.alarmId, {ignoreLoading: true}).subscribe(
      alarm => this.loadAlarmSubject.next(alarm)
    );
  }

  loadAlarmFields(alarm: AlarmInfo) {
    this.alarmFormGroup.get('originatorName')
      .patchValue(alarm.originatorLabel ? alarm.originatorLabel : alarm.originatorName);
    this.alarmFormGroup.get('alarmSeverity')
      .patchValue(this.translate.instant(alarmSeverityTranslations.get(alarm.severity)));
    if (alarm.startTs) {
      this.alarmFormGroup.get('startTime')
        .patchValue(this.datePipe.transform(alarm.startTs, 'yyyy-MM-dd HH:mm:ss'));
    }
    if (alarm.startTs || alarm.clearTs) {
      let duration = '';
      if (alarm.startTs && !alarm.cleared) {
        duration = this.millisecondsToTimeStringPipe.transform(Date.now() - alarm.startTs);
      } else if (alarm.clearTs && alarm.cleared) {
        duration = this.millisecondsToTimeStringPipe.transform(alarm.clearTs - alarm.startTs);
      }
      this.alarmFormGroup.get('duration').patchValue(duration);
    }
    this.alarmFormGroup.get('type').patchValue(this.utils.customTranslation(alarm.type, alarm.type));
    this.alarmFormGroup.get('alarmStatus')
      .patchValue(this.translate.instant(alarmStatusTranslations.get(alarm.status)));
    this.alarmFormGroup.get('alarmDetails').patchValue(alarm.details);
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
          this.alarmCommentComponent.loadAlarmComments();
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
          this.alarmCommentComponent.loadAlarmComments();
        }
      );
    }
  }

  onReassign(): void {
    this.alarmUpdated = true;
    this.loadAlarm();
    this.alarmCommentComponent.loadAlarmComments();
  }
}
