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
import { MAT_LEGACY_DIALOG_DATA as MAT_DIALOG_DATA, MatLegacyDialogRef as MatDialogRef } from '@angular/material/legacy-dialog';
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
  styleUrls: []
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

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private datePipe: DatePipe,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AlarmDetailsDialogData,
              private alarmService: AlarmService,
              public dialogRef: MatDialogRef<AlarmDetailsDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.allowAcknowledgment = data.allowAcknowledgment;
    this.allowClear = data.allowClear;
    this.displayDetails = data.displayDetails;

    this.alarmFormGroup = this.fb.group(
      {
        createdTime: [''],
        originatorName: [''],
        startTime: [''],
        endTime: [''],
        ackTime: [''],
        clearTime: [''],
        type: [''],
        alarmSeverity: [''],
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
    this.alarmFormGroup.get('type').patchValue(alarm.type);
    this.alarmFormGroup.get('alarmSeverity')
      .patchValue(this.translate.instant(alarmSeverityTranslations.get(alarm.severity)));
    this.alarmFormGroup.get('alarmStatus')
      .patchValue(this.translate.instant(alarmStatusTranslations.get(alarm.status)));
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

}
