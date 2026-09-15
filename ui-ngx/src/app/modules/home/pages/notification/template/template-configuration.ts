// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
import { Subject } from 'rxjs';
import { Directive, OnDestroy } from '@angular/core';
import { deepClone, deepTrim } from '@core/utils';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class TemplateConfiguration<T, R = any> extends DialogComponent<T, R> implements OnDestroy{

  templateNotificationForm: FormGroup;
  notificationTemplateConfigurationForm: FormGroup;

  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationDeliveryMethodInfoMap = NotificationDeliveryMethodInfoMap;
  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

  deliveryConfiguration: Partial<DeliveryMethodsTemplates>;

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

    this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.deliveryConfiguration = value;
    });

    this.notificationTemplateConfigurationForm = this.fb.group({
      deliveryMethodsTemplates: null
    });

    this.notificationDeliveryMethods.forEach(method => {
      (this.templateNotificationForm.get('configuration.deliveryMethodsTemplates') as FormGroup)
        .addControl(method, this.fb.group({enabled: method === NotificationDeliveryMethod.WEB}), {emitEvent: false});
    });

    this.deliveryConfiguration = this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').value;
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
    const template = deepClone(this.templateNotificationForm.value);
    template.configuration = deepClone(this.notificationTemplateConfigurationForm.value);
    return deepTrim(template);
  }
}
