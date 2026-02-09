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

import {
  NotificationDeliveryMethod,
  NotificationRequest,
  NotificationRequestPreview,
  NotificationTarget,
  NotificationType
} from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepTrim, guid, isDefinedAndNotNull } from '@core/utils';
import { Observable } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatStepper } from '@angular/material/stepper';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { map, takeUntil } from 'rxjs/operators';
import { getCurrentTime } from '@shared/models/time/time.models';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { MatButton } from '@angular/material/button';
import { TemplateConfiguration } from '@home/pages/notification/template/template-configuration';
import { Authority } from '@shared/models/authority.enum';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';

export interface RequestNotificationDialogData {
  request?: NotificationRequest;
  isAdd?: boolean;
}

@Component({
    selector: 'tb-sent-notification-dialog',
    templateUrl: './sent-notification-dialog.component.html',
    styleUrls: ['./sent-notification-dialog.component.scss'],
    standalone: false
})
export class SentNotificationDialogComponent extends
  TemplateConfiguration<SentNotificationDialogComponent, NotificationRequest> implements OnDestroy {

  @ViewChild('createNotification', {static: true}) createNotification: MatStepper;
  stepperOrientation: Observable<StepperOrientation>;

  isAdd = true;
  entityType = EntityType;
  notificationType = NotificationType;

  notificationRequestForm: FormGroup;

  selectedIndex = 0;
  preview: NotificationRequestPreview = null;

  dialogTitle = 'notification.notify-again';

  showRefresh = false;

  tinyMceOptions: Record<string, any> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['autoresize'],
    menubar: false,
    toolbar: false,
    statusbar: false,
    resize: false,
    readonly: true,
    height: 400,
    autofocus: false,
    branding: false,
    promotion: false,
    setup: (ed) => {
      ed.on('PreInit', () => {
        const document = $(ed.iframeElement.contentDocument);
        const body = $('#tinymce', document);
        body.attr({contenteditable: false});
        body.css('pointerEvents', 'none');
        body.css('userSelect', 'none');
      })
    }
  };

  private authUser: AuthUser = getCurrentAuthUser(this.store);

  private allowNotificationDeliveryMethods: Array<NotificationDeliveryMethod>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<SentNotificationDialogComponent, NotificationRequest>,
              @Inject(MAT_DIALOG_DATA) public data: RequestNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              protected fb: FormBuilder,
              private notificationService: NotificationService,
              private dialog: MatDialog,
              private translate: TranslateService) {
    super(store, router, dialogRef, fb);

    this.notificationDeliveryMethods.forEach(method => {
      if (method !== NotificationDeliveryMethod.WEB) {
        this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').get(method).disable({emitEvent: false});
      }
    });

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.notificationRequestForm = this.fb.group({
      useTemplate: [false],
      templateId: [{value: null, disabled: true}, Validators.required],
      targets: [null, Validators.required],
      template: this.templateNotificationForm,
      additionalConfig: this.fb.group({
        enabled: [false],
        timezone: [{value: '', disabled: true}, Validators.required],
        time: [{value: 0, disabled: true}, Validators.required]
      })
    });

    this.notificationRequestForm.get('template.name').setValue(guid());

    this.notificationRequestForm.get('useTemplate').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.notificationRequestForm.get('templateId').enable({emitEvent: false});
        this.notificationRequestForm.get('template').disable({emitEvent: false});
      } else {
        this.notificationRequestForm.get('templateId').disable({emitEvent: false});
        this.notificationRequestForm.get('template').enable({emitEvent: false});
        this.updateDeliveryMethodsDisableState();
      }
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
      this.notificationRequestForm.reset({}, {emitEvent: false});
      this.notificationRequestForm.patchValue(this.data.request, {emitEvent: false});
      this.notificationRequestForm.get('template.name').setValue(guid());
      this.notificationRequestForm.get('template.notificationType').setValue(NotificationType.GENERAL);
      let useTemplate = true;
      if (isDefinedAndNotNull(this.data.request.template)) {
        useTemplate = false;
        this.notificationTemplateConfigurationForm.patchValue({
          deliveryMethodsTemplates: this.data.request.template.configuration.deliveryMethodsTemplates
        }, {emitEvent: false});
      }
      this.notificationRequestForm.get('useTemplate').setValue(useTemplate, {onlySelf : true});
      this.deliveryConfiguration = this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').value;
    }
    this.refreshAllowDeliveryMethod();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
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
    if ($event.previouslySelectedIndex > $event.selectedIndex) {
      $event.previouslySelectedStep.interacted = false;
    }
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
          if (data.processedTemplates.WEB?.enabled) {
            (data.processedTemplates.WEB as any).text = data.processedTemplates.WEB.body;
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
    if (!formValue.useTemplate) {
      formValue.template = super.getNotificationTemplateValue();
    }
    delete formValue.useTemplate;
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

  private isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  private isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  minDate(): Date {
    return new Date(getCurrentTime(this.notificationRequestForm.get('additionalConfig.timezone').value).format('lll'));
  }

  maxDate(): Date {
    const date = this.minDate();
    date.setDate(date.getDate() + 7);
    return date;
  }

  createTarget($event: Event, button: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    button._elementRef.nativeElement.blur();
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          let formValue: string[] = this.notificationRequestForm.get('targets').value;
          if (!formValue) {
            formValue = [];
          }
          formValue.push(res.id.id);
          this.notificationRequestForm.get('targets').patchValue(formValue);
        }
      });
  }

  getDeliveryMethodsTemplatesControl(deliveryMethod: NotificationDeliveryMethod): AbstractControl {
    return this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').get(deliveryMethod).get('enabled');
  }

  getDeliveryMethodsTooltip(deliveryMethod: NotificationDeliveryMethod): string {
    if (this.allowConfigureDeliveryMethod(deliveryMethod)) {
      return this.translate.instant('notification.delivery-method-not-configure-click');
    }
    return this.translate.instant('notification.delivery-method-not-configure-contact');
  }

  allowConfigureDeliveryMethod(deliveryMethod: NotificationDeliveryMethod): boolean {
    const tenantAllowConfigureDeliveryMethod = new Set([
      NotificationDeliveryMethod.SLACK
    ]);
    if (deliveryMethod === NotificationDeliveryMethod.WEB) {
      return false;
    }
    if(this.isSysAdmin()) {
      return true;
    } else if (this.isTenantAdmin()) {
      return tenantAllowConfigureDeliveryMethod.has(deliveryMethod);
    }
    return false;
  }

  isInteractDeliveryMethod(deliveryMethod: NotificationDeliveryMethod) {
    return this.getDeliveryMethodsTemplatesControl(deliveryMethod).disabled && this.allowConfigureDeliveryMethod(deliveryMethod);
  }

  configurationPage(deliveryMethod: NotificationDeliveryMethod) {
    switch (deliveryMethod) {
      case NotificationDeliveryMethod.EMAIL:
        return '/settings/outgoing-mail';
      case NotificationDeliveryMethod.SMS:
      case NotificationDeliveryMethod.SLACK:
      case NotificationDeliveryMethod.MOBILE_APP:
        return '/settings/notifications';
    }
  }

  refreshAllowDeliveryMethod() {
    this.notificationService.getAvailableDeliveryMethods({ignoreLoading: true}).subscribe(allowMethods => {
      this.allowNotificationDeliveryMethods = allowMethods;
      if (!this.notificationRequestForm.get('useTemplate').value) {
        this.updateDeliveryMethodsDisableState();
        this.showRefresh = (this.notificationDeliveryMethods.length !== allowMethods.length);
      }
    });
  }

  private updateDeliveryMethodsDisableState() {
    if (this.allowNotificationDeliveryMethods) {
      this.notificationDeliveryMethods.forEach(method => {
        if (this.allowNotificationDeliveryMethods.includes(method)) {
          this.getDeliveryMethodsTemplatesControl(method).enable({emitEvent: true});
        } else {
          this.getDeliveryMethodsTemplatesControl(method).disable({emitEvent: true});
          this.getDeliveryMethodsTemplatesControl(method).setValue(false, {emitEvent: true}); //used for notify again
        }
      });
    }
  }
}
