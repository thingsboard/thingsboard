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

import { NotificationRule, TriggerType, TriggerTypeTranslationMap } from '@shared/models/notification.models';
import { Component, ElementRef, Inject, OnDestroy, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { deepTrim, isDefined } from '@core/utils';
import { Observable, of, Subject } from 'rxjs';
import { map, mergeMap, share, startWith } from 'rxjs/operators';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MatStepper } from '@angular/material/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatChipInputEvent, MatChipList } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import {
  AlarmSearchStatus,
  alarmSearchStatusTranslations,
  AlarmSeverity,
  alarmSeverityTranslations
} from '@shared/models/alarm.models';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { TranslateService } from '@ngx-translate/core';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';

export interface RuleNotificationDialogData {
  rule?: NotificationRule;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-rule-notification-dialog',
  templateUrl: './rule-notification-dialog.component.html',
  styleUrls: ['rule-notification-dialog.component.scss']
})
export class RuleNotificationDialogComponent extends
  DialogComponent<RuleNotificationDialogComponent, NotificationRule> implements OnDestroy {

  @ViewChild('addNotificationRule', {static: true}) addNotificationRule: MatStepper;

  @ViewChild('severitiesChipList') severitiesChipList: MatChipList;
  @ViewChild('severityAutocomplete') severityAutocomplete: MatAutocomplete;
  @ViewChild('severityInput') severityInput: ElementRef<HTMLInputElement>;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  stepperOrientation: Observable<StepperOrientation>;

  ruleNotificationForm: FormGroup;
  alarmTemplateForm: FormGroup;
  deviceInactivityTemplateForm: FormGroup;
  entityActionTemplateForm: FormGroup;

  triggerType = TriggerType;
  triggerTypes: TriggerType[] = Object.values(TriggerType);
  triggerTypeTranslationMap = TriggerTypeTranslationMap;

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;
  alarmSeverityTranslationMap = alarmSeverityTranslations;

  alarmSearchStatuses = [AlarmSearchStatus.ACTIVE,
    AlarmSearchStatus.CLEARED,
    AlarmSearchStatus.ACK,
    AlarmSearchStatus.UNACK];
  alarmSearchStatusTranslationMap = alarmSearchStatusTranslations;

  entityType = EntityType;
  entityTypes = Object.values(EntityType);
  isAdd = true;

  selectedIndex = 0;

  filteredDisplaySeverities: Observable<Array<string>>;
  severitySearchText = '';

  severityInputChange = new Subject<string>();

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RuleNotificationDialogComponent, NotificationRule>,
              @Inject(MAT_DIALOG_DATA) public data: RuleNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    if (isDefined(data.isAdd)) {
      this.isAdd = data.isAdd;
    }

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.ruleNotificationForm = this.fb.group({
      name: [null, Validators.required],
      templateId: [null, Validators.required],
      trigger: [this.triggerType.ALARM, Validators.required],
      configuration: this.fb.group({
        escalationConfig: this.fb.group({
          escalations: [[]]
        }),
        description: [null]
      })
    });

    this.alarmTemplateForm = this.fb.group({
      alarmTypeList: [[], Validators.required],
      alarmSeverityList: [[], Validators.required],
      alarmStatusList: [[], Validators.required],
      escalationConfig: [],
      description: ['']
    });

    this.deviceInactivityTemplateForm = this.fb.group({
      filterByDevice: [true],
      deviceId: [],
      deviceProfileId: [],
      notificationTargetId: [],
      description: ['']
    });

    this.entityActionTemplateForm = this.fb.group({
      entityType: [],
      status: this.fb.group({
        created: [false],
        updated: [false],
        deleted: [false]
      }),
      notificationTargetId: [],
      description: ['']
    });

    this.filteredDisplaySeverities = this.severityInputChange
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchSeverities(name) ),
        share()
      );
  }

  onSeverityRemoved(severity: string): void {
    const severities: string[] = this.alarmTemplateForm.get('alarmSeverityList').value;
    const index = severities.indexOf(severity);
    if (index > -1) {
      severities.splice(index, 1);
      this.alarmTemplateForm.get('alarmSeverityList').setValue(severities);
      this.alarmTemplateForm.get('alarmSeverityList').markAsDirty();
      this.severitiesChipList.errorState = !severities.length;
    }
  }

  onSeverityInputFocus() {
    this.severityInputChange.next(this.severityInput.nativeElement.value);
  }

  addSeverityFromChipInput(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim()) {
      const severityName = value.trim().toUpperCase();
      const existingSeverity = this.alarmSeverities.find(severity => severity.toUpperCase() === severityName);
      if (this.addSeverity(existingSeverity)) {
        this.clearSeverityInput('');
      }
    }
  }

  private addSeverity(existingSeverity: string): boolean {
    if (existingSeverity) {
      const displaySeverities: string[] = this.alarmTemplateForm.get('alarmSeverityList').value;
      const index = displaySeverities.indexOf(existingSeverity);
      if (index === -1) {
        displaySeverities.push(existingSeverity);
        this.alarmTemplateForm.get('alarmSeverityList').setValue(displaySeverities);
        this.alarmTemplateForm.get('alarmSeverityList').markAsDirty();
        this.severitiesChipList.errorState = false;
        return true;
      }
    }
    return false;
  }

  clearSeverityInput(value: string = '') {
    this.severityInput.nativeElement.value = value;
    this.severityInputChange.next(null);
    setTimeout(() => {
      this.severityInput.nativeElement.blur();
      this.severityInput.nativeElement.focus();
    }, 0);
  }

  severitySelected(event: MatAutocompleteSelectedEvent): void {
    this.addSeverity(event.option.value);
    this.clearSeverityInput('');
  }

  displaySeverityFn(severity?: string): string | undefined {
    return severity ? this.translate.instant(this.alarmSeverityTranslationMap.get(this.alarmSeverityEnum[severity])) : undefined;
  }

  private fetchSeverities(searchText?: string): Observable<Array<string>> {
    this.severitySearchText = searchText;
    if (this.severitySearchText && this.severitySearchText.length) {
      const search = this.severitySearchText.toUpperCase();
      return of(this.alarmSeverities.filter(severity => severity.toUpperCase().includes(search)));
    } else {
      return of(this.alarmSeverities);
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  public alarmTypeList(): string[] {
    return this.alarmTemplateForm.get('alarmTypeList').value;
  }

  public removeAlarmType(type: string): void {
    const types: string[] = this.alarmTemplateForm.get('alarmTypeList').value;
    const index = types.indexOf(type);
    if (index >= 0) {
      types.splice(index, 1);
      this.alarmTemplateForm.get('alarmTypeList').setValue(types);
      this.alarmTemplateForm.get('alarmTypeList').markAsDirty();
    }
  }

  public addAlarmType(event: MatChipInputEvent): void {
    const input = event.input;
    const value = event.value;

    const types: string[] = this.alarmTemplateForm.get('alarmTypeList').value;

    if ((value || '').trim()) {
      types.push(value.trim());
      this.alarmTemplateForm.get('alarmTypeList').setValue(types);
      this.alarmTemplateForm.get('alarmTypeList').markAsDirty();
    }

    if (input) {
      input.value = '';
    }
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
    if (this.selectedIndex === 1 && this.selectedIndex < this.maxStepperIndex && this.alarmTemplateForm.pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex === 2 && this.selectedIndex < this.maxStepperIndex && this.deviceInactivityTemplateForm.pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex === 3 && this.selectedIndex < this.maxStepperIndex && this.entityActionTemplateForm.pristine) {
      return 'action.skip';
    }
    if (this.selectedIndex !== 0 && this.selectedIndex >= this.maxStepperIndex) {
      return 'action.add';
    }
    return 'action.next';
  }

  private get maxStepperIndex(): number {
    return this.addNotificationRule?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      const formValue: NotificationRule = this.ruleNotificationForm.value;
      // if (formValue.configuration.deliveryMethodsTemplates.PUSH.enabled) {
      //   Object.assign(formValue.configuration.deliveryMethodsTemplates.PUSH, this.pushTemplateForm.value);
      // } else {
      //   delete formValue.configuration.deliveryMethodsTemplates.PUSH;
      // }
      // if (formValue.configuration.deliveryMethodsTemplates.EMAIL.enabled) {
      //   Object.assign(formValue.configuration.deliveryMethodsTemplates.EMAIL, this.emailTemplateForm.value);
      // } else {
      //   delete formValue.configuration.deliveryMethodsTemplates.EMAIL;
      // }
      // if (formValue.configuration.deliveryMethodsTemplates.SMS.enabled) {
      //   Object.assign(formValue.configuration.deliveryMethodsTemplates.SMS, this.smsTemplateForm.value);
      // } else {
      //   delete formValue.configuration.deliveryMethodsTemplates.SMS;
      // }
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

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    let formValue = deepTrim(this.ruleNotificationForm.value);
    if (isDefined(this.data.rule)) {
      formValue = Object.assign({}, this.data.rule, formValue);
    }
    this.notificationService.saveNotificationRule(formValue).subscribe(
      (rule) => this.dialogRef.close(rule)
    );
  }
}
