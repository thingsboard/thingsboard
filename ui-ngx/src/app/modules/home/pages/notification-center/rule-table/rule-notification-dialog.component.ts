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

import {
  NotificationRule,
  NotificationTarget,
  TriggerType,
  TriggerTypeTranslationMap
} from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { EntityType } from '@shared/models/entity-type.models';
import { deepClone, deepTrim, isDefined } from '@core/utils';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MatStepper } from '@angular/material/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { AlarmStatus, alarmStatusTranslations } from '@shared/models/alarm.models';
import { TranslateService } from '@ngx-translate/core';
import {
  TargetNotificationDialogComponent,
  TargetsNotificationDialogData
} from '@home/pages/notification-center/targets-table/target-notification-dialog.componet';
import { MatButton } from '@angular/material/button';

export interface RuleNotificationDialogData {
  rule?: NotificationRule;
  isAdd?: boolean;
  isCopy?: boolean;
}

@Component({
  selector: 'tb-rule-notification-dialog',
  templateUrl: './rule-notification-dialog.component.html',
  styleUrls: ['rule-notification-dialog.component.scss']
})
export class RuleNotificationDialogComponent extends
  DialogComponent<RuleNotificationDialogComponent, NotificationRule> implements OnDestroy {

  @ViewChild('addNotificationRule', {static: true}) addNotificationRule: MatStepper;

  stepperOrientation: Observable<StepperOrientation>;

  ruleNotificationForm: FormGroup;
  alarmTemplateForm: FormGroup;
  deviceInactivityTemplateForm: FormGroup;
  entityActionTemplateForm: FormGroup;
  alarmCommentTemplateForm: FormGroup;

  triggerType = TriggerType;
  triggerTypes: TriggerType[] = Object.values(TriggerType);
  triggerTypeTranslationMap = TriggerTypeTranslationMap;

  alarmSearchStatuses: AlarmStatus[] = Object.values(AlarmStatus);
  alarmSearchStatusTranslationMap = alarmStatusTranslations;

  entityType = EntityType;
  entityTypes: EntityType[] = Object.values(EntityType);
  isAdd = true;

  selectedIndex = 0;

  dialogTitle = 'notification.edit-rule';

  private destroy$ = new Subject<void>();

  private readonly ruleNotification: NotificationRule;

  private triggerTypeFormsMap: Map<TriggerType, FormGroup>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RuleNotificationDialogComponent, NotificationRule>,
              @Inject(MAT_DIALOG_DATA) public data: RuleNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder,
              public translate: TranslateService,
              private notificationService: NotificationService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    if (isDefined(data.isAdd)) {
      this.isAdd = data.isAdd;
    }

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.ruleNotificationForm = this.fb.group({
      name: [null, Validators.required],
      templateId: [null, Validators.required],
      triggerType: [TriggerType.ALARM, Validators.required],
      recipientsConfig: this.fb.group({
        targets: [{value: null, disabled: true}, Validators.required],
        escalationTable: [null, Validators.required]
      }),
      triggerConfig: [null],
      additionalConfig: this.fb.group({
        description: ['']
      })
    });

    this.ruleNotificationForm.get('triggerType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value === TriggerType.ALARM) {
        this.ruleNotificationForm.get('recipientsConfig.escalationTable').enable({emitEvent: false});
        this.ruleNotificationForm.get('recipientsConfig.targets').disable({emitEvent: false});
      } else {
        this.ruleNotificationForm.get('recipientsConfig.escalationTable').disable({emitEvent: false});
        this.ruleNotificationForm.get('recipientsConfig.targets').enable({emitEvent: false});
      }
    });

    this.alarmTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        alarmTypes: [null],
        alarmSeverities: [null, Validators.required],
        clearRule: this.fb.group({
          alarmStatus: [null]
        })
      })
    });

    this.deviceInactivityTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        filterByDevice: [true],
        devices: [null],
        deviceProfiles: [{value: null, disabled: true}]
      })
    });

    this.deviceInactivityTemplateForm.get('triggerConfig.filterByDevice').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
        if (value) {
          this.deviceInactivityTemplateForm.get('triggerConfig.devices').enable({emitEvent: false});
          this.deviceInactivityTemplateForm.get('triggerConfig.deviceProfiles').disable({emitEvent: false});
        } else {
          this.deviceInactivityTemplateForm.get('triggerConfig.deviceProfiles').enable({emitEvent: false});
          this.deviceInactivityTemplateForm.get('triggerConfig.devices').disable({emitEvent: false});
        }
    });

    this.entityActionTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        entityType: [EntityType.DEVICE],
        created: [false],
        updated: [false],
        deleted: [false]
      })
    });

    this.alarmCommentTemplateForm = this.fb.group({ });

    this.triggerTypeFormsMap = new Map<TriggerType, FormGroup>([
      [TriggerType.ALARM, this.alarmTemplateForm],
      [TriggerType.ALARM_COMMENT, this.alarmCommentTemplateForm],
      [TriggerType.DEVICE_INACTIVITY, this.deviceInactivityTemplateForm],
      [TriggerType.ENTITY_ACTION, this.entityActionTemplateForm]
    ]);

    if (data.isAdd || data.isCopy) {
      this.dialogTitle = 'notification.add-rule';
    }
    this.ruleNotification = deepClone(this.data.rule);

    if (this.ruleNotification) {
      if (this.data.isCopy) {
        this.ruleNotification.name += ` (${this.translate.instant('action.copy')})`;
      }
      this.ruleNotificationForm.reset({}, {emitEvent: false});
      this.ruleNotificationForm.patchValue(this.ruleNotification, {emitEvent: false});
      this.ruleNotificationForm.get('triggerType').updateValueAndValidity({onlySelf: true});
      const currentForm = this.triggerTypeFormsMap.get(this.ruleNotification.triggerType);
      currentForm.patchValue(this.ruleNotification, {emitEvent: false});
      if (this.ruleNotification.triggerType === TriggerType.DEVICE_INACTIVITY) {
        this.deviceInactivityTemplateForm.get('triggerConfig.filterByDevice')
          .patchValue(!!this.ruleNotification.triggerConfig.devices, {onlySelf: true});
      }
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
  }

  backStep() {
    this.addNotificationRule.previous();
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStepperIndex) {
      this.add();
    } else {
      this.addNotificationRule.next();
    }
  }

  nextStepLabel(): string {
    if (this.selectedIndex !== 0) {
      return (this.data.isAdd || this.data.isCopy) ? 'action.add' : 'action.save';
    }
    return 'action.next';
  }

  private get maxStepperIndex(): number {
    return this.addNotificationRule?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      let formValue = this.ruleNotificationForm.value;
      const triggerType: TriggerType = this.ruleNotificationForm.get('triggerType').value;
      const currentForm = this.triggerTypeFormsMap.get(triggerType);
      Object.assign(formValue, currentForm.value);
      if (triggerType === TriggerType.DEVICE_INACTIVITY) {
        delete formValue.triggerConfig.filterByDevice;
      }
      formValue.recipientsConfig.triggerType = triggerType;
      formValue.triggerConfig.triggerType = triggerType;
      if (this.ruleNotification && !this.data.isCopy) {
        formValue = {...this.ruleNotification, ...formValue};
      }
      this.notificationService.saveNotificationRule(deepTrim(formValue)).subscribe(
        (target) => this.dialogRef.close(target)
      );
    }
  }

  private allValid(): boolean {
    return !this.addNotificationRule.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addNotificationRule.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  createTarget($event: Event, button: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    button._elementRef.nativeElement.blur();
    this.dialog.open<TargetNotificationDialogComponent, TargetsNotificationDialogData,
      NotificationTarget>(TargetNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          let formValue: string[] = this.ruleNotificationForm.get('recipientsConfig.targets').value;
          if (!formValue) {
            formValue = [];
          }
          formValue.push(res.id.id);
          this.ruleNotificationForm.get('recipientsConfig.targets').patchValue(formValue);
        }
      });
  }
}
