// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import {
  DeliveryMethodsTemplates,
  NotificationDeliveryMethod,
  NotificationDeliveryMethodInfoMap,
  NotificationTemplate,
  NotificationTemplateTypeTranslateMap,
  NotificationType
} from '@shared/models/notification.models';
import { takeUntil } from 'rxjs/operators';
import { merge, Subject } from 'rxjs';
import { Directive, OnDestroy } from '@angular/core';
import { deepClone, deepTrim } from '@core/utils';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { EntityType } from '@shared/models/entity-type.models';
import { ReportTemplateType } from '@app/shared/models/report.models';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class TemplateConfiguration<T, R = any> extends DialogComponent<T, R> implements OnDestroy{

  notificationType = NotificationType;
  ReportTemplateType = ReportTemplateType;
  entityType = EntityType;

  templateNotificationForm: FormGroup;
  notificationTemplateConfigurationForm: FormGroup;

  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationDeliveryMethodInfoMap = NotificationDeliveryMethodInfoMap;
  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

  deliveryConfiguration: Partial<DeliveryMethodsTemplates>;

  protected readonly destroy$ = new Subject<void>();

  private authUser: AuthUser = getCurrentAuthUser(this.store);

  protected constructor(protected store: Store<AppState>,
                        protected router: Router,
                        protected dialogRef: MatDialogRef<T, R>,
                        protected fb: FormBuilder) {
    super(store, router, dialogRef);

    this.templateNotificationForm = this.fb.group({
      name: ['', Validators.required],
      notificationType: [NotificationType.GENERAL],
      configuration: this.fb.group({
        deliveryMethodsTemplates: this.fb.group({}, {validators: this.atLeastOne()}),
        attachReport: [],
        reportTemplateId: [null, [Validators.required]],
        userId: [null, [Validators.required]],
        timezone: [null, [Validators.required]]
      })
    });

    this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.deliveryConfiguration = value;
    });

    merge(this.templateNotificationForm.get('notificationType').valueChanges,
          this.templateNotificationForm.get('configuration.attachReport').valueChanges).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateValidators();
    });

    this.notificationTemplateConfigurationForm = this.fb.group({
      deliveryMethodsTemplates: null
    });

    this.notificationDeliveryMethods.forEach(method => {
      (this.templateNotificationForm.get('configuration.deliveryMethodsTemplates') as FormGroup)
        .addControl(method, this.fb.group({enabled: method === NotificationDeliveryMethod.WEB}), {emitEvent: false});
    });

    merge(this.templateNotificationForm.get('configuration.deliveryMethodsTemplates.SLACK').valueChanges,
      this.templateNotificationForm.get('configuration.deliveryMethodsTemplates.EMAIL').valueChanges).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateAttachReportValidators();
    });

    this.deliveryConfiguration = this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').value;
    this.updateAttachReportValidators();
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

  isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  protected getNotificationTemplateValue(): NotificationTemplate {
    const template = deepClone(this.templateNotificationForm.value);
    template.configuration.deliveryMethodsTemplates = deepClone(this.notificationTemplateConfigurationForm.get('deliveryMethodsTemplates').value);
    return deepTrim(template);
  }

  protected updateValidators() {
    const notificationType: NotificationType = this.templateNotificationForm.get('notificationType').value;
    if (notificationType === NotificationType.REPORT_GENERATED) {
      this.templateNotificationForm.get('configuration.attachReport').patchValue(false, {emitEvent: false});
    }
    const attachReport = this.templateNotificationForm.get('configuration.attachReport');
    if (attachReport.value && !attachReport.disabled) {
      this.templateNotificationForm.get('configuration.reportTemplateId').enable({emitEvent: false});
      this.templateNotificationForm.get('configuration.userId').enable({emitEvent: false});
      this.templateNotificationForm.get('configuration.timezone').enable({emitEvent: false});
    } else {
      this.templateNotificationForm.get('configuration.reportTemplateId').disable({emitEvent: false});
      this.templateNotificationForm.get('configuration.userId').disable({emitEvent: false});
      this.templateNotificationForm.get('configuration.timezone').disable({emitEvent: false});
    }
  }

  protected updateAttachReportValidators() {
    const slack = this.templateNotificationForm.get('configuration.deliveryMethodsTemplates.SLACK').value;
    const email = this.templateNotificationForm.get('configuration.deliveryMethodsTemplates.EMAIL').value;
    if (!email.enabled && !slack.enabled) {
      this.templateNotificationForm.get('configuration.attachReport').patchValue(false, {emitEvent: false});
      this.templateNotificationForm.get('configuration.attachReport').disable();
    } else {
      this.templateNotificationForm.get('configuration.attachReport').enable();
    }
  }
}
