///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { NotificationTemplate, NotificationType } from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MatStepper } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { TranslateService } from '@ngx-translate/core';
import { TemplateConfiguration } from '@home/pages/notification/template/template-configuration';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';

export interface TemplateNotificationDialogData {
  template?: NotificationTemplate;
  predefinedType?: NotificationType;
  isAdd?: boolean;
  isCopy?: boolean;
  name?: string;
}

@Component({
  selector: 'tb-template-notification-dialog',
  templateUrl: './template-notification-dialog.component.html',
  styleUrls: ['./template-notification-dialog.component.scss']
})
export class TemplateNotificationDialogComponent
  extends TemplateConfiguration<TemplateNotificationDialogComponent, NotificationTemplate> implements OnDestroy {

  @ViewChild('notificationTemplateStepper', {static: true}) notificationTemplateStepper: MatStepper;

  stepperOrientation: Observable<StepperOrientation>;

  dialogTitle = 'notification.edit-notification-template';

  notificationTypes: NotificationType[];

  selectedIndex = 0;
  hideSelectType = false;

  notificationTemplateConfigurationForm: FormGroup;

  private readonly templateNotification: NotificationTemplate;
  private authUser: AuthUser = getCurrentAuthUser(this.store);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<TemplateNotificationDialogComponent, NotificationTemplate>,
              @Inject(MAT_DIALOG_DATA) public data: TemplateNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              protected fb: FormBuilder,
              private notificationService: NotificationService,
              private translate: TranslateService) {
    super(store, router, dialogRef, fb);

    this.notificationTypes = this.allowNotificationType();

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    if (isDefinedAndNotNull(this.data?.predefinedType)) {
      this.hideSelectType = true;
      this.templateNotificationForm.get('notificationType').setValue(this.data.predefinedType, {emitEvent: false});
    }
    if (isDefinedAndNotNull(this.data?.name)) {
      this.templateNotificationForm.get('name').setValue(this.data.name, {emitEvent: false});
    }

    if (data.isAdd || data.isCopy) {
      this.dialogTitle = 'notification.add-notification-template';
    }
    this.templateNotification = deepClone(this.data.template);

    if (this.templateNotification) {
      if (this.data.isCopy) {
        this.templateNotification.name += ` (${this.translate.instant('action.copy')})`;
      } else {
        this.templateNotificationForm.get('notificationType').disable({emitEvent: false});
      }
      this.templateNotificationForm.reset({}, {emitEvent: false});
      this.templateNotificationForm.patchValue(this.templateNotification, {emitEvent: false});
      this.notificationTemplateConfigurationForm.patchValue({
        deliveryMethodsTemplates: this.templateNotification.configuration.deliveryMethodsTemplates
      }, {emitEvent: false});
      this.deliveryConfiguration = this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').value;
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

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
    if ($event.previouslySelectedIndex > $event.selectedIndex) {
      $event.previouslySelectedStep.interacted = false;
    }
  }

  backStep() {
    this.notificationTemplateStepper.previous();
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStepperIndex) {
      this.add();
    } else {
      this.notificationTemplateStepper.next();
    }
  }

  nextStepLabel(): string {
    if (this.selectedIndex >= this.maxStepperIndex && this.selectedIndex !== 0) {
      return (this.data.isAdd || this.data.isCopy) ? 'action.add' : 'action.save';
    }
    return 'action.next';
  }

  private get maxStepperIndex(): number {
    return this.notificationTemplateStepper?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      let template = this.getNotificationTemplateValue();
      if (this.templateNotification && !this.data.isCopy) {
        template = {...this.templateNotification, ...template};
      }
      this.notificationService.saveNotificationTemplate(template).subscribe(
        (target) => this.dialogRef.close(target)
      );
    }
  }

  private allValid(): boolean {
    return !this.notificationTemplateStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.notificationTemplateStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  private isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  private allowNotificationType(): NotificationType[] {
    const sysAdminAllowNotificationTypes = new Set([
      NotificationType.ENTITIES_LIMIT,
      NotificationType.ENTITIES_LIMIT_INCREASE_REQUEST,
      NotificationType.API_USAGE_LIMIT,
      NotificationType.NEW_PLATFORM_VERSION,
      NotificationType.RATE_LIMITS,
      NotificationType.TASK_PROCESSING_FAILURE,
      NotificationType.RESOURCES_SHORTAGE
    ]);

    if (this.isSysAdmin()) {
      return [NotificationType.GENERAL, ...sysAdminAllowNotificationTypes];
    }
    return Object.values(NotificationType).filter(type => !sysAdminAllowNotificationTypes.has(type));
  }
}
