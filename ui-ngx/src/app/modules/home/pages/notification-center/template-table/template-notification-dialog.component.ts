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
  NotificationDeliveryMethod,
  NotificationDeliveryMethodTranslateMap,
  NotificationTemplate,
  NotificationTemplateTypeTranslateMap,
  NotificationType,
  SlackChanelType,
  SlackChanelTypesTranslateMap
} from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepClone, deepTrim } from '@core/utils';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MatStepper } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { TranslateService } from '@ngx-translate/core';

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

  @ViewChild('notificationTemplateStepper', {static: true}) notificationTemplateStepper: MatStepper;

  stepperOrientation: Observable<StepperOrientation>;

  templateNotificationForm: FormGroup;
  pushTemplateForm: FormGroup;
  emailTemplateForm: FormGroup;
  smsTemplateForm: FormGroup;
  slackTemplateForm: FormGroup;
  dialogTitle = 'notification.edit-notification-template';

  notificationTypes = Object.keys(NotificationType) as NotificationType[];
  slackChanelTypes = Object.keys(SlackChanelType) as SlackChanelType[];
  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;
  notificationDeliveryMethodTranslateMap = NotificationDeliveryMethodTranslateMap;
  slackChanelTypesTranslateMap = SlackChanelTypesTranslateMap;

  selectedIndex = 0;

  tinyMceOptions: Record<string, any> = {
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

  private readonly destroy$ = new Subject<void>();
  private readonly templateNotification: NotificationTemplate;

  private deliveryMethodFormsMap: Map<NotificationDeliveryMethod, FormGroup>;
  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<TemplateNotificationDialogComponent, NotificationTemplate>,
              @Inject(MAT_DIALOG_DATA) public data: TemplateNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder,
              private notificationService: NotificationService,
              private translate: TranslateService) {
    super(store, router, dialogRef);

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.templateNotificationForm = this.fb.group({
      name: ['', Validators.required],
      notificationType: [NotificationType.GENERAL],
      configuration: this.fb.group({
        notificationSubject: ['', Validators.required],
        defaultTextTemplate: ['', Validators.required],
        deliveryMethodsTemplates: this.fb.group({}, {validators: this.atLeastOne()})
      })
    });
    this.notificationDeliveryMethods.forEach(method => {
      (this.templateNotificationForm.get('configuration.deliveryMethodsTemplates') as FormGroup)
        .addControl(method, this.fb.group({enabled: method === NotificationDeliveryMethod.PUSH}), {emitEvent: false});
    });

    this.pushTemplateForm = this.fb.group({
      subject: [''],
      body: [''],
      additionalConfig: this.fb.group({
        icon: this.fb.group({
          enabled: [false],
          icon: [{value: '', disabled: true}, Validators.required],
          color: ['#757575']
        }),
        actionButtonConfig: this.fb.group({
          enabled: [false],
          text: [{value: '', disabled: true}, Validators.required],
          color: ['#305680'],
          link: [{value: '', disabled: true}, Validators.required]
        }),
      })
    });

    this.pushTemplateForm.get('additionalConfig.icon.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value) {
        this.pushTemplateForm.get('additionalConfig.icon.icon').enable({emitEvent: false});
      } else {
        this.pushTemplateForm.get('additionalConfig.icon.icon').disable({emitEvent: false});
      }
    });

    this.pushTemplateForm.get('additionalConfig.actionButtonConfig.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value) {
        this.pushTemplateForm.get('additionalConfig.actionButtonConfig.text').enable({emitEvent: false});
        this.pushTemplateForm.get('additionalConfig.actionButtonConfig.link').enable({emitEvent: false});
      } else {
        this.pushTemplateForm.get('additionalConfig.actionButtonConfig.text').disable({emitEvent: false});
        this.pushTemplateForm.get('additionalConfig.actionButtonConfig.link').disable({emitEvent: false});
      }
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

    this.deliveryMethodFormsMap = new Map<NotificationDeliveryMethod, FormGroup>([
      [NotificationDeliveryMethod.PUSH, this.pushTemplateForm],
      [NotificationDeliveryMethod.EMAIL, this.emailTemplateForm],
      [NotificationDeliveryMethod.SMS, this.smsTemplateForm],
      [NotificationDeliveryMethod.SLACK, this.slackTemplateForm]
    ]);

    if (data.isAdd || data.isCopy) {
      this.dialogTitle = 'notification.add-notification-template';
    }
    this.templateNotification = deepClone(this.data.template);

    if (this.templateNotification) {
      if (this.data.isCopy) {
        this.templateNotification.name += ` (${this.translate.instant('action.copy')})`;
      }
      this.templateNotificationForm.reset({}, {emitEvent: false});
      this.templateNotificationForm.patchValue(this.templateNotification, {emitEvent: false});
      for (const method in this.templateNotification.configuration.deliveryMethodsTemplates) {
        this.deliveryMethodFormsMap.get(NotificationDeliveryMethod[method])
          .patchValue(this.templateNotification.configuration.deliveryMethodsTemplates[method]);
      }
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
    if (this.selectedIndex !== 0) {
      if (this.selectedIndex >= this.maxStepperIndex) {
        return (this.data.isAdd || this.data.isCopy) ? 'action.add' : 'action.save';
      } else if (this.notificationTemplateStepper.selected.stepControl.pristine) {
        return 'action.skip';
      }
    }
    return 'action.next';
  }

  private get maxStepperIndex(): number {
    return this.notificationTemplateStepper?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      let template: NotificationTemplate = this.templateNotificationForm.value;
      this.notificationDeliveryMethods.forEach(method => {
        if (template.configuration.deliveryMethodsTemplates[method].enabled) {
          Object.assign(template.configuration.deliveryMethodsTemplates[method], this.deliveryMethodFormsMap.get(method).value, {method});
        } else {
          delete template.configuration.deliveryMethodsTemplates[method];
        }
      });
      if (this.templateNotification) {
        template = {...this.templateNotification, ...template};
      }
      this.notificationService.saveNotificationTemplate(deepTrim(template)).subscribe(
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

  private atLeastOne() {
    return (group: FormGroup): ValidationErrors | null => {
      let hasAtLeastOne = true;
      if (group?.controls) {
        const controlsFormValue: FormGroup[] = Object.entries(group.controls).map(method => method[1]) as any;
        hasAtLeastOne = controlsFormValue.some(value => value.controls.enabled.value);
      }
      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }
}
