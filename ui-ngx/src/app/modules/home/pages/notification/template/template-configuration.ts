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
  ActionButtonLinkType,
  ActionButtonLinkTypeTranslateMap,
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
import { deepClone, deepTrim } from '@core/utils';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class TemplateConfiguration<T, R = any> extends DialogComponent<T, R> implements OnDestroy{

  templateNotificationForm: FormGroup;
  webTemplateForm: FormGroup;
  emailTemplateForm: FormGroup;
  smsTemplateForm: FormGroup;
  slackTemplateForm: FormGroup;
  microsoftTeamsTemplateForm: FormGroup;

  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationDeliveryMethodTranslateMap = NotificationDeliveryMethodTranslateMap;
  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

  actionButtonLinkType = ActionButtonLinkType;
  actionButtonLinkTypes = Object.keys(ActionButtonLinkType) as ActionButtonLinkType[];
  actionButtonLinkTypeTranslateMap = ActionButtonLinkTypeTranslateMap;

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
        deliveryMethodsTemplates: this.fb.group({}, {validators: this.atLeastOne()})
      })
    });

    this.notificationDeliveryMethods.forEach(method => {
      (this.templateNotificationForm.get('configuration.deliveryMethodsTemplates') as FormGroup)
        .addControl(method, this.fb.group({enabled: method === NotificationDeliveryMethod.WEB}), {emitEvent: false});
    });

    this.webTemplateForm = this.fb.group({
      subject: ['', Validators.required],
      body: ['', Validators.required],
      additionalConfig: this.fb.group({
        icon: this.fb.group({
          enabled: [false],
          icon: [{value: 'notifications', disabled: true}, Validators.required],
          color: [{value: '#757575', disabled: true}]
        }),
        actionButtonConfig: this.createButtonConfigForm()
      })
    });

    this.webTemplateForm.get('additionalConfig.icon.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value) {
        this.webTemplateForm.get('additionalConfig.icon.icon').enable({emitEvent: false});
        this.webTemplateForm.get('additionalConfig.icon.color').enable({emitEvent: false});
      } else {
        this.webTemplateForm.get('additionalConfig.icon.icon').disable({emitEvent: false});
        this.webTemplateForm.get('additionalConfig.icon.color').disable({emitEvent: false});
      }
    });

    this.emailTemplateForm = this.fb.group({
      subject: ['', Validators.required],
      body: ['', Validators.required]
    });

    this.smsTemplateForm = this.fb.group({
      body: ['', [Validators.required, Validators.maxLength(320)]]
    });

    this.slackTemplateForm = this.fb.group({
      body: ['', Validators.required]
    });

    this.microsoftTeamsTemplateForm = this.fb.group({
      subject: [''],
      body: ['', Validators.required],
      themeColor: [''],
      button: this.createButtonConfigForm()
    });

    this.deliveryMethodFormsMap = new Map<NotificationDeliveryMethod, FormGroup>([
      [NotificationDeliveryMethod.WEB, this.webTemplateForm],
      [NotificationDeliveryMethod.EMAIL, this.emailTemplateForm],
      [NotificationDeliveryMethod.SMS, this.smsTemplateForm],
      [NotificationDeliveryMethod.SLACK, this.slackTemplateForm],
      [NotificationDeliveryMethod.MICROSOFT_TEAMS, this.microsoftTeamsTemplateForm]
    ]);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  atLeastOne() {
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
    const template: NotificationTemplate = deepClone(this.templateNotificationForm.value);
    this.notificationDeliveryMethods.forEach(method => {
      if (template.configuration.deliveryMethodsTemplates[method]?.enabled) {
        Object.assign(template.configuration.deliveryMethodsTemplates[method], this.deliveryMethodFormsMap.get(method).value, {method});
      } else {
        delete template.configuration.deliveryMethodsTemplates[method];
      }
    });
    return deepTrim(template);
  }

  private createButtonConfigForm(): FormGroup {
    const form = this.fb.group({
      enabled: [false],
      text: [{value: '', disabled: true}, [Validators.required, Validators.maxLength(50)]],
      linkType: [ActionButtonLinkType.LINK],
      link: [{value: '', disabled: true}, Validators.required],
      dashboardId: [{value: null, disabled: true}, Validators.required],
      dashboardState: [{value: null, disabled: true}],
      setEntityIdInState: [{value: true, disabled: true}],
    });

    form.get('enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value) {
        form.enable({emitEvent: false});
        form.get('linkType').updateValueAndValidity({onlySelf: true});
      } else {
        form.disable({emitEvent: false});
        form.get('enabled').enable({emitEvent: false});
      }
    });

    form.get('linkType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const isEnabled = form.get('enabled').value;
      if (isEnabled) {
        if (value === ActionButtonLinkType.LINK) {
          form.get('link').enable({emitEvent: false});
          form.get('dashboardId').disable({emitEvent: false});
          form.get('dashboardState').disable({emitEvent: false});
          form.get('setEntityIdInState').disable({emitEvent: false});
        } else {
          form.get('link').disable({emitEvent: false});
          form.get('dashboardId').enable({emitEvent: false});
          form.get('dashboardState').enable({emitEvent: false});
          form.get('setEntityIdInState').enable({emitEvent: false});
        }
      }
    });
    return form;
  }
}
