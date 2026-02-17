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
  AlarmAction,
  AlarmActionTranslationMap,
  AlarmAssignmentAction,
  AlarmAssignmentActionTranslationMap,
  ComponentLifecycleEvent,
  ComponentLifecycleEventTranslationMap,
  DeviceEvent,
  DeviceEventTranslationMap,
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
import {
  AlarmSearchStatus,
  alarmSearchStatusTranslations,
  AlarmSeverity,
  alarmSeverityTranslations
} from '@shared/models/alarm.models';
import { TranslateService } from '@ngx-translate/core';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { MatButton } from '@angular/material/button';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import {
  ApiFeature,
  ApiFeatureTranslationMap,
  ApiUsageStateValue,
  ApiUsageStateValueTranslationMap
} from '@shared/models/api-usage.models';
import { LimitedApi, LimitedApiTranslationMap } from '@shared/models/limited-api.models';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { EdgeConnectionEvent, EdgeConnectionEventTranslationMap } from '@shared/models/edge.models';

export interface RuleNotificationDialogData {
  rule?: NotificationRule;
  isAdd?: boolean;
  isCopy?: boolean;
}

@Component({
    selector: 'tb-rule-notification-dialog',
    templateUrl: './rule-notification-dialog.component.html',
    styleUrls: ['rule-notification-dialog.component.scss'],
    standalone: false
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
  alarmAssignmentTemplateForm: FormGroup;
  ruleEngineEventsTemplateForm: FormGroup;
  entitiesLimitTemplateForm: FormGroup;
  apiUsageLimitTemplateForm: FormGroup;
  newPlatformVersionTemplateForm: FormGroup;
  rateLimitsTemplateForm: FormGroup;
  edgeCommunicationFailureTemplateForm: FormGroup;
  edgeConnectionTemplateForm: FormGroup;
  taskProcessingFailureTemplateForm: FormGroup;
  resourceUsageShortageTemplateForm: FormGroup;

  triggerType = TriggerType;
  triggerTypes: TriggerType[];
  triggerTypeTranslationMap = TriggerTypeTranslationMap;

  alarmSearchStatuses = [
    AlarmSearchStatus.ACTIVE,
    AlarmSearchStatus.CLEARED,
    AlarmSearchStatus.ACK,
    AlarmSearchStatus.UNACK
  ];
  alarmSearchStatusTranslationMap = alarmSearchStatusTranslations;

  alarmSeverityTranslationMap = alarmSeverityTranslations;
  alarmSeverities = Object.keys(AlarmSeverity) as Array<AlarmSeverity>;

  alarmActions: AlarmAction[] = Object.values(AlarmAction);
  alarmActionTranslationMap = AlarmActionTranslationMap;

  alarmAssignmentActions: AlarmAssignmentAction[] = Object.values(AlarmAssignmentAction);
  alarmAssignmentActionTranslationMap = AlarmAssignmentActionTranslationMap;

  componentLifecycleEvents: ComponentLifecycleEvent[] = Object.values(ComponentLifecycleEvent);
  componentLifecycleEventTranslationMap = ComponentLifecycleEventTranslationMap;

  deviceEvents: DeviceEvent[] = Object.values(DeviceEvent);
  deviceEventTranslationMap = DeviceEventTranslationMap;

  apiUsageStateValues: ApiUsageStateValue[] = Object.values(ApiUsageStateValue);
  apiUsageStateValueTranslationMap = ApiUsageStateValueTranslationMap;

  apiFeatures: ApiFeature[] = Object.values(ApiFeature);
  apiFeatureTranslationMap = ApiFeatureTranslationMap;

  edgeConnectionEvents: EdgeConnectionEvent[] = Object.values(EdgeConnectionEvent);
  edgeConnectionEventTranslationMap = EdgeConnectionEventTranslationMap;

  limitedApis: StringItemsOption[];

  entityType = EntityType;
  isAdd = true;

  allowEntityTypeForEntitiesLimit = [
    EntityType.DEVICE,
    EntityType.ASSET,
    EntityType.CUSTOMER,
    EntityType.USER,
    EntityType.DASHBOARD,
    EntityType.RULE_CHAIN
  ];

  selectedIndex = 0;

  dialogTitle = 'notification.edit-rule';

  private destroy$ = new Subject<void>();

  private readonly ruleNotification: NotificationRule;

  private triggerTypeFormsMap: Map<TriggerType, FormGroup>;
  private authState: AuthState = getCurrentAuthState(this.store);
  private authUser: AuthUser = this.authState.authUser;
  private _allowEntityTypeForEntityAction: EntityType[];

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

    this.triggerTypes = this.allowTriggerTypes();

    if (isDefined(data.isAdd)) {
      this.isAdd = data.isAdd;
    }

    this.limitedApis = Object.values(LimitedApi).map(value => ({
      name: this.translate.instant(LimitedApiTranslationMap.get(value)),
      value
    }));

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.ruleNotificationForm = this.fb.group({
      name: [null, Validators.required],
      enabled: [true, Validators.required],
      templateId: [null, Validators.required],
      triggerType: [this.isSysAdmin() ? TriggerType.ENTITIES_LIMIT : TriggerType.ALARM, Validators.required],
      recipientsConfig: this.fb.group({
        targets: [{value: null, disabled: !this.isSysAdmin()}, Validators.required],
        escalationTable: [{value: null, disabled: this.isSysAdmin()}, Validators.required]
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

    this.ruleNotificationForm.get('recipientsConfig.escalationTable').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this.countRecipientsChainConfig() > 1) {
        this.alarmTemplateForm.get('triggerConfig.clearRule').enable({emitEvent: false});
      } else {
        this.alarmTemplateForm.get('triggerConfig.clearRule').disable({emitEvent: false});
      }
    });

    this.edgeConnectionTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        edges: [null],
        notifyOn: [null]
      })
    });

    this.edgeCommunicationFailureTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        edges: [null]
      })
    });

    this.alarmTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        alarmTypes: [null],
        alarmSeverities: [[]],
        clearRule: this.fb.group({
          alarmStatuses: [[]]
        }),
        notifyOn: [[AlarmAction.CREATED], Validators.required]
      })
    });

    this.deviceInactivityTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        filterByDevice: [true],
        devices: [null],
        deviceProfiles: [{value: null, disabled: true}],
        notifyOn: [[DeviceEvent.INACTIVE], Validators.required]
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
        entityTypes: [[EntityType.DEVICE], Validators.required],
        created: [false],
        updated: [false],
        deleted: [false]
      })
    });

    this.alarmCommentTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        alarmTypes: [null],
        alarmSeverities: [[]],
        alarmStatuses: [[]],
        onlyUserComments: [false],
        notifyOnCommentUpdate: [false]
      })
    });

    this.alarmAssignmentTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        alarmTypes: [null],
        alarmSeverities: [[]],
        alarmStatuses: [[]],
        notifyOn: [[AlarmAssignmentAction.ASSIGNED], Validators.required]
      })
    });

    this.ruleEngineEventsTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        ruleChains: [null],
        ruleChainEvents: [null],
        onlyRuleChainLifecycleFailures: [false],
        trackRuleNodeEvents: [false],
        ruleNodeEvents: [null],
        onlyRuleNodeLifecycleFailures: [false]
      })
    });

    this.entitiesLimitTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        entityTypes: [],
        threshold: [80, [Validators.min(0), Validators.max(100)]]
      })
    });

    this.apiUsageLimitTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        apiFeatures: [[]],
        notifyOn: [[ApiUsageStateValue.WARNING], Validators.required]
      })
    });

    this.newPlatformVersionTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({

      })
    });

    this.rateLimitsTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        apis: []
      })
    });

    this.taskProcessingFailureTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        taskTypes: []
      })
    });

    this.resourceUsageShortageTemplateForm = this.fb.group({
      triggerConfig: this.fb.group({
        cpuThreshold: [80, [Validators.min(0), Validators.max(100)]],
        ramThreshold: [80, [Validators.min(0), Validators.max(100)]],
        storageThreshold: [80, [Validators.min(0), Validators.max(100)]]
      })
    });

    this.triggerTypeFormsMap = new Map<TriggerType, FormGroup>([
      [TriggerType.ALARM, this.alarmTemplateForm],
      [TriggerType.ALARM_COMMENT, this.alarmCommentTemplateForm],
      [TriggerType.DEVICE_ACTIVITY, this.deviceInactivityTemplateForm],
      [TriggerType.ENTITY_ACTION, this.entityActionTemplateForm],
      [TriggerType.ALARM_ASSIGNMENT, this.alarmAssignmentTemplateForm],
      [TriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT, this.ruleEngineEventsTemplateForm],
      [TriggerType.ENTITIES_LIMIT, this.entitiesLimitTemplateForm],
      [TriggerType.API_USAGE_LIMIT, this.apiUsageLimitTemplateForm],
      [TriggerType.NEW_PLATFORM_VERSION, this.newPlatformVersionTemplateForm],
      [TriggerType.RATE_LIMITS, this.rateLimitsTemplateForm],
      [TriggerType.EDGE_COMMUNICATION_FAILURE, this.edgeCommunicationFailureTemplateForm],
      [TriggerType.EDGE_CONNECTION, this.edgeConnectionTemplateForm],
      [TriggerType.TASK_PROCESSING_FAILURE, this.taskProcessingFailureTemplateForm],
      [TriggerType.RESOURCES_SHORTAGE, this.resourceUsageShortageTemplateForm]
    ]);

    if (data.isAdd || data.isCopy) {
      this.dialogTitle = 'notification.add-rule';
    }
    this.ruleNotification = deepClone(this.data.rule);

    if (this.ruleNotification) {
      if (this.data.isCopy) {
        this.ruleNotification.name += ` (${this.translate.instant('action.copy')})`;
      } else {
        this.ruleNotificationForm.get('triggerType').disable({emitEvent: false});
      }
      this.ruleNotificationForm.reset({}, {emitEvent: false});
      this.ruleNotificationForm.patchValue(this.ruleNotification, {emitEvent: false});
      this.ruleNotificationForm.get('triggerType').updateValueAndValidity({onlySelf: true});
      const currentForm = this.triggerTypeFormsMap.get(this.ruleNotification.triggerType);
      currentForm.patchValue(this.ruleNotification, {emitEvent: false});
      if (this.ruleNotification.triggerType === TriggerType.DEVICE_ACTIVITY) {
        this.deviceInactivityTemplateForm.get('triggerConfig.filterByDevice')
          .patchValue(!!this.ruleNotification.triggerConfig.devices, {onlySelf: true});
      }
      if (this.ruleNotification.triggerType === TriggerType.ENTITIES_LIMIT) {
        this.entitiesLimitTemplateForm.get('triggerConfig.threshold').patchValue(this.ruleNotification.triggerConfig.threshold * 100, {emitEvent: false});
      }
      if (this.ruleNotification.triggerType === TriggerType.RESOURCES_SHORTAGE) {
        this.resourceUsageShortageTemplateForm.get('triggerConfig.cpuThreshold').patchValue(this.ruleNotification.triggerConfig.cpuThreshold * 100, {emitEvent: false});
        this.resourceUsageShortageTemplateForm.get('triggerConfig.ramThreshold').patchValue(this.ruleNotification.triggerConfig.ramThreshold * 100, {emitEvent: false});
        this.resourceUsageShortageTemplateForm.get('triggerConfig.storageThreshold').patchValue(this.ruleNotification.triggerConfig.storageThreshold * 100, {emitEvent: false});
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
    if ($event.previouslySelectedIndex > $event.selectedIndex) {
      $event.previouslySelectedStep.interacted = false;
    }
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
      if (triggerType === TriggerType.DEVICE_ACTIVITY) {
        delete formValue.triggerConfig.filterByDevice;
      }
      if (triggerType === TriggerType.ENTITIES_LIMIT) {
        formValue.triggerConfig.threshold = formValue.triggerConfig.threshold / 100;
      }
      if (triggerType === TriggerType.RESOURCES_SHORTAGE) {
        formValue.triggerConfig.cpuThreshold = formValue.triggerConfig.cpuThreshold / 100;
        formValue.triggerConfig.ramThreshold = formValue.triggerConfig.ramThreshold / 100;
        formValue.triggerConfig.storageThreshold = formValue.triggerConfig.storageThreshold / 100;
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
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
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

  countRecipientsChainConfig(): number {
    return Object.keys(this.ruleNotificationForm.get('recipientsConfig.escalationTable').value ?? {}).length;
  }

  formatLabel(value: number): string {
    return `${value}%`;
  }

  private isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  private allowTriggerTypes(): TriggerType[] {
    const sysAdminAllowTriggerTypes = new Set([
      TriggerType.ENTITIES_LIMIT,
      TriggerType.API_USAGE_LIMIT,
      TriggerType.NEW_PLATFORM_VERSION,
      TriggerType.RATE_LIMITS,
      TriggerType.TASK_PROCESSING_FAILURE,
      TriggerType.RESOURCES_SHORTAGE
    ]);

    if (this.isSysAdmin()) {
      return Array.from(sysAdminAllowTriggerTypes);
    }
    return Object.values(TriggerType).filter(type => !sysAdminAllowTriggerTypes.has(type));
  }

  get allowEntityTypeForEntityAction(): EntityType[] {
    if (!this._allowEntityTypeForEntityAction) {
      const excludeEntityType: Set<EntityType> = new Set([
        EntityType.API_USAGE_STATE,
        EntityType.TENANT_PROFILE,
        EntityType.RPC,
        EntityType.QUEUE,
        EntityType.NOTIFICATION,
        EntityType.NOTIFICATION_REQUEST,
        EntityType.WIDGET_TYPE
      ]);
      this._allowEntityTypeForEntityAction = Object.values(EntityType).filter(type => !excludeEntityType.has(type));
    }
    return this._allowEntityTypeForEntityAction;
  }
}
