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

import { NotificationRequest, NotificationRequestPreview } from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepTrim, isDefined } from '@core/utils';
import { Observable, Subject } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatStepper } from '@angular/material/stepper';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { map } from 'rxjs/operators';

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
  notificationRequestForm: FormGroup;

  selectedIndex = 0;
  preview: NotificationRequestPreview = null;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RequestNotificationDialogComponent, NotificationRequest>,
              @Inject(MAT_DIALOG_DATA) public data: RequestNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    if (isDefined(data.isAdd)) {
      this.isAdd = data.isAdd;
    }

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.notificationRequestForm = this.fb.group({
      templateId: [null, Validators.required],
      targets: [null, Validators.required]
    });
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
      this.notificationService.getNotificationRequestPreview(this.notificationFormValue).subscribe(
        (preview) => this.preview = preview
      );
    }
  }

  private get notificationFormValue(): NotificationRequest {
    const formValue: NotificationRequest = deepTrim(this.notificationRequestForm.value);
    formValue.additionalConfig = {sendingDelayInSec: 0};
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
}
