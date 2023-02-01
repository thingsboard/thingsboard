///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { NotificationRequest, NotificationRequestPreview, NotificationType } from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepTrim } from '@core/utils';
import { Observable, Subject } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatStepper } from '@angular/material/stepper';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { map, takeUntil } from 'rxjs/operators';
import { getCurrentTime } from '@shared/models/time/time.models';

export interface RequestNotificationDialogData {
  request?: NotificationRequest;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-request-notification-dialog',
  templateUrl: './request-notification-dialog.component.html',
  styleUrls: ['./request-notification-dialog.component.scss']
})
export class RequestNotificationDialogComponent extends
  DialogComponent<RequestNotificationDialogComponent, NotificationRequest> implements OnDestroy {

  @ViewChild('createNotification', {static: true}) createNotification: MatStepper;
  stepperOrientation: Observable<StepperOrientation>;
  isAdd = true;
  entityType = EntityType;
  notificationType = NotificationType;
  notificationRequestForm: FormGroup;

  selectedIndex = 0;
  preview: NotificationRequestPreview = null;

  dialogTitle = 'notification.notify-again';

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RequestNotificationDialogComponent, NotificationRequest>,
              @Inject(MAT_DIALOG_DATA) public data: RequestNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.notificationRequestForm = this.fb.group({
      templateId: [null, Validators.required],
      targets: [null, Validators.required],
      additionalConfig: this.fb.group({
        enabled: [false],
        timezone: [{value: '', disabled: true}, Validators.required],
        time: [{value: 0, disabled: true}, Validators.required]
      })
    });

    this.notificationRequestForm.get('additionalConfig.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.notificationRequestForm.get('additionalConfig.timezone').enable({emitEvent: false});
        this.notificationRequestForm.get('additionalConfig.time').enable({emitEvent: false});
      } else {
        this.notificationRequestForm.get('additionalConfig.timezone').disable({emitEvent: false});
        this.notificationRequestForm.get('additionalConfig.time').disable({emitEvent: false});
      }
    });

    if (data.isAdd) {
      this.dialogTitle = 'notification.new-notification';
    }
    if (data.request) {
      this.notificationRequestForm.patchValue(this.data.request, {emitEvent: false});
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  nextStepLabel(): string {
    if (this.selectedIndex >= this.maxStepperIndex) {
      return 'action.send';
    }
    return 'action.next';
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
    if (this.selectedIndex === this.maxStepperIndex) {
      this.getPreview();
    }
  }

  backStep() {
    this.createNotification.previous();
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStepperIndex) {
      this.add();
    } else {
      this.createNotification.next();
    }
  }

  private get maxStepperIndex(): number {
    return this.createNotification?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      this.notificationService.createNotificationRequest(this.notificationFormValue).subscribe(
        (notification) => this.dialogRef.close(notification)
      );
    }
  }

  private getPreview() {
    if (this.allValid()) {
      this.preview = null;
      this.notificationService.getNotificationRequestPreview(this.notificationFormValue).pipe(
        map(data => {
          if (data.processedTemplates.PUSH?.enabled) {
            (data.processedTemplates.PUSH as any).text = data.processedTemplates.PUSH.body;
          }
          return data;
        })
      ).subscribe(
        (preview) => this.preview = preview
      );
    }
  }

  private get notificationFormValue(): NotificationRequest {
    const formValue = deepTrim(this.notificationRequestForm.value);
    let delay = 0;
    if (formValue.additionalConfig.enabled) {
      delay = (this.notificationRequestForm.value.additionalConfig.time.valueOf() - this.minDate().valueOf()) / 1000;
    }
    formValue.additionalConfig = {
      sendingDelayInSec: delay > 0 ? delay : 0
    };
    return formValue;
  }

  private allValid(): boolean {
    return !this.createNotification.steps.find((item, index) => {
      if (item.stepControl?.invalid) {
        item.interacted = true;
        this.createNotification.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  minDate(): Date {
    return new Date(getCurrentTime(this.notificationRequestForm.get('additionalConfig.timezone').value).format('lll'));
  }

  maxDate(): Date {
    const date = this.minDate();
    date.setDate(date.getDate() + 7);
    return date;
  }
}
