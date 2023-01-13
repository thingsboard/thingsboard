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

import {
  DeliveryMethodNotificationTemplate,
  NotificationDeliveryMethod,
  NotificationTemplate,
  NotificationTemplateType,
  NotificationTemplateTypeTranslateMap, SlackChanelType
} from '@shared/models/notification.models';
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
import { map } from 'rxjs/operators';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MatStepper } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { NotificationType } from '@core/notification/notification.models';

export interface TemplateNotificationDialogData {
  template?: NotificationTemplate;
  isAdd?: boolean;
  isCopy?: boolean;
}

@Component({
  selector: 'tb-template-notification-dialog',
  templateUrl: './template-notification-dialog.component.html',
  styleUrls: ['./template-notification-dialog.component.scss']
})
export class TemplateNotificationDialogComponent
  extends DialogComponent<TemplateNotificationDialogComponent, NotificationTemplate> implements OnDestroy {

  @ViewChild('addNotificationTemplate', {static: true}) addNotificationTemplate: MatStepper;

  stepperOrientation: Observable<StepperOrientation>;

  templateNotificationForm: FormGroup;
  pushTemplateForm: FormGroup;
  emailTemplateForm: FormGroup;
  smsTemplateForm: FormGroup;
  slackTemplateForm: FormGroup;
  dialogTitle = 'notification.edit-notification-template';
  saveButtonLabel = 'action.save';

  notificationTypes = Object.keys(NotificationTemplateType) as NotificationType[];
  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

  selectedIndex = 0;

  tinyMceOptions: Record<string, any>;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<TemplateNotificationDialogComponent, NotificationTemplate>,
              @Inject(MAT_DIALOG_DATA) public data: TemplateNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    if (data.isAdd) {
      this.dialogTitle = 'notification.add-notification-template';
      this.saveButtonLabel = 'action.add';
    }
    if (data.isCopy) {
      this.dialogTitle = 'notification.copy-notification-template';
    }

    this.tinyMceOptions = {
      base_url: '/assets/tinymce',
      suffix: '.min',
      plugins: ['link table image imagetools code fullscreen'],
      menubar: 'edit insert tools view format table',
      toolbar: 'fontselect fontsizeselect | formatselect | bold italic  strikethrough  forecolor backcolor ' +
        '| link | table | image | alignleft aligncenter alignright alignjustify  ' +
        '| numlist bullist outdent indent  | removeformat | code | fullscreen',
      height: 400,
      autofocus: false,
      branding: false
    };

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.templateNotificationForm = this.fb.group({
      name: ['', Validators.required],
      notificationType: [NotificationTemplateType.GENERAL],
      configuration: this.fb.group({
        notificationSubject: ['', Validators.required],
        defaultTextTemplate: ['', Validators.required],
        deliveryMethodsTemplates: this.fb.group({})
      })
    });
    this.notificationDeliveryMethods.forEach(method => {
      (this.templateNotificationForm.get('configuration.deliveryMethodsTemplates') as FormGroup)
        .addControl(method, this.fb.group({method, enabled: method === NotificationDeliveryMethod.PUSH}), {emitEvent: false});
    });

    this.pushTemplateForm = this.fb.group({
      subject: [''],
      body: [''],
      icon: [''],
      actionButtonConfig: ['']
    });

    this.emailTemplateForm = this.fb.group({
      subject: [''],
      body: ['']
    });

    this.smsTemplateForm = this.fb.group({
      body: ['']
    });

    this.slackTemplateForm = this.fb.group({
      body: [''],
      conversationId: ['', Validators.required],
      conversationType: [SlackChanelType.DIRECT]
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

  save() {
    let formValue = deepTrim(this.templateNotificationForm.value);
    if (isDefined(this.data.template)) {
      formValue = Object.assign({}, this.data.template, formValue);
    }
    this.notificationService.saveNotificationTemplate(formValue).subscribe(
      (target) => this.dialogRef.close(target)
    );
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
  }

  backStep() {
    this.addNotificationTemplate.previous();
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStepperIndex) {
      this.add();
    } else {
      this.addNotificationTemplate.next();
    }
  }

  nextStepLabel(): string {
    if (this.selectedIndex === 1 && this.selectedIndex < this.maxStepperIndex && this.pushTemplateForm.pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex === 2 && this.selectedIndex < this.maxStepperIndex && this.emailTemplateForm.pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex === 3 && this.selectedIndex < this.maxStepperIndex && this.smsTemplateForm.pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex >= this.maxStepperIndex) {
      return 'action.add';
    }
    return 'action.next';
  }

  private get maxStepperIndex(): number {
    return this.addNotificationTemplate?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      const formValue: NotificationTemplate = this.templateNotificationForm.value;
      if (formValue.configuration.deliveryMethodsTemplates.PUSH.enabled) {
        Object.assign(formValue.configuration.deliveryMethodsTemplates.PUSH, this.pushTemplateForm.value);
      } else {
        delete formValue.configuration.deliveryMethodsTemplates.PUSH;
      }
      if (formValue.configuration.deliveryMethodsTemplates.EMAIL.enabled) {
        Object.assign(formValue.configuration.deliveryMethodsTemplates.EMAIL, this.emailTemplateForm.value);
      } else {
        delete formValue.configuration.deliveryMethodsTemplates.EMAIL;
      }
      if (formValue.configuration.deliveryMethodsTemplates.SMS.enabled) {
        Object.assign(formValue.configuration.deliveryMethodsTemplates.SMS, this.smsTemplateForm.value);
      } else {
        delete formValue.configuration.deliveryMethodsTemplates.SMS;
      }
      if (formValue.configuration.deliveryMethodsTemplates.SLACK.enabled) {
        Object.assign(formValue.configuration.deliveryMethodsTemplates.SLACK, this.slackTemplateForm.value);
      } else {
        delete formValue.configuration.deliveryMethodsTemplates.SLACK;
      }
      this.notificationService.saveNotificationTemplate(deepTrim(formValue)).subscribe(
        (target) => this.dialogRef.close(target)
      );
    }
  }

  allValid(): boolean {
    return !this.addNotificationTemplate.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addNotificationTemplate.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }
}
