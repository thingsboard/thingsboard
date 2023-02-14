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

import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import {
  NotificationDeliveryMethod,
  NotificationDeliveryMethodTranslateMap,
  NotificationTemplate,
  NotificationTemplateTypeTranslateMap,
  NotificationType
} from '@shared/models/notification.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { Directive, OnDestroy } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class TemplateConfiguration<T, R = any> extends DialogComponent<T, R> implements OnDestroy{

  templateNotificationForm: FormGroup;
  pushTemplateForm: FormGroup;
  emailTemplateForm: FormGroup;
  smsTemplateForm: FormGroup;
  slackTemplateForm: FormGroup;

  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationDeliveryMethodTranslateMap = NotificationDeliveryMethodTranslateMap;
  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

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

  protected readonly destroy$ = new Subject<void>();

  protected deliveryMethodFormsMap: Map<NotificationDeliveryMethod, FormGroup>;

  protected constructor(protected store: Store<AppState>,
                        protected router: Router,
                        protected dialogRef: MatDialogRef<T, R>,
                        protected fb: FormBuilder) {
    super(store, router, dialogRef);

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
      body: ['']
    });

    this.deliveryMethodFormsMap = new Map<NotificationDeliveryMethod, FormGroup>([
      [NotificationDeliveryMethod.PUSH, this.pushTemplateForm],
      [NotificationDeliveryMethod.EMAIL, this.emailTemplateForm],
      [NotificationDeliveryMethod.SMS, this.smsTemplateForm],
      [NotificationDeliveryMethod.SLACK, this.slackTemplateForm]
    ]);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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

  protected getNotificationTemplateValue(): NotificationTemplate {
    const template: NotificationTemplate = this.templateNotificationForm.value;
    this.notificationDeliveryMethods.forEach(method => {
      if (template.configuration.deliveryMethodsTemplates[method].enabled) {
        Object.assign(template.configuration.deliveryMethodsTemplates[method], this.deliveryMethodFormsMap.get(method).value, {method});
      } else {
        delete template.configuration.deliveryMethodsTemplates[method];
      }
    });
    return template;
  }
}
