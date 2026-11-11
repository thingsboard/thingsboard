// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { SchedulerEvent } from '@shared/models/scheduler-event.models';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { SchedulerEventConfigType } from '@home/components/scheduler/scheduler-event-config.models';
import { deepClone, isDefinedAndNotNull, isObject, isString } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface SchedulerEventDialogData {
  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};
  isAdd: boolean;
  readonly: boolean;
  schedulerEvent: SchedulerEvent;
  defaultEventType: string;
}

@Component({
    selector: 'tb-scheduler-event-dialog',
    templateUrl: './scheduler-event-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: SchedulerEventDialogComponent }],
    styleUrls: ['./scheduler-event-dialog.component.scss'],
    standalone: false
})
export class SchedulerEventDialogComponent extends DialogComponent<SchedulerEventDialogComponent, boolean>
  implements OnInit, ErrorStateMatcher {

  schedulerEventFormGroup: UntypedFormGroup;

  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};
  isAdd: boolean;
  readonly: boolean;
  schedulerEvent: SchedulerEvent;
  defaultEventType: string;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SchedulerEventDialogData,
              private schedulerEventService: SchedulerEventService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<SchedulerEventDialogComponent, boolean>,
              public fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
    this.schedulerEventConfigTypes = data.schedulerEventConfigTypes;
    this.isAdd = data.isAdd;
    this.readonly = data.readonly;
    this.schedulerEvent = data.schedulerEvent;
    this.defaultEventType = data.defaultEventType;
  }

  ngOnInit(): void {
    const configuration = deepClone(this.schedulerEvent.configuration);
    if (configuration && this.schedulerEvent.originatorId) {
      configuration.originatorId = this.schedulerEvent.originatorId;
    }
    this.schedulerEventFormGroup = this.fb.group({
      name: [this.schedulerEvent.name, [Validators.required, Validators.maxLength(255)]],
      type: [this.isAdd ? this.defaultEventType : this.schedulerEvent.type, [Validators.required]],
      configuration: [configuration, [Validators.required]],
      enabled: [isDefinedAndNotNull(this.schedulerEvent.enabled) ? this.schedulerEvent.enabled: true, []],
      schedule: [this.schedulerEvent.schedule, [Validators.required]]
    });
    if (this.readonly) {
      this.schedulerEventFormGroup.disable();
    } else if (this.defaultEventType) {
      this.schedulerEventFormGroup.get('type').disable();
    } else if (!this.readonly) {
      this.schedulerEventFormGroup.get('type').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((newVal) => {
        const prevVal = this.schedulerEventFormGroup.value.type;
        if (newVal !== prevVal && newVal) {
          this.schedulerEventFormGroup.get('configuration').patchValue({
            originatorId: null,
            msgType: null,
            msgBody: {},
            metadata: {}
          }, {emitEvent: false});
        }
      });
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    this.submitted = true;
    if (!this.schedulerEventFormGroup.invalid) {
      const schedulerEventValue = this.schedulerEventFormGroup.getRawValue();
      this.schedulerEvent.originatorId = schedulerEventValue.configuration?.originatorId;
      if (schedulerEventValue.configuration?.originatorId) {
        delete schedulerEventValue.configuration?.originatorId;
      }
      this.schedulerEvent = {...this.schedulerEvent, ...schedulerEventValue};
      this.schedulerEventService.saveSchedulerEvent(this.deepTrim(this.schedulerEvent)).subscribe(
        () => {
            this.dialogRef.close(true);
        }
      );
    }
  }

  private deepTrim<T>(obj: T): T {
    return Object.keys(obj).reduce((acc, curr) => {
      if (isString(obj[curr])) {
        acc[curr] = obj[curr].trim();
      } else if (isObject(obj[curr])) {
        acc[curr] = this.deepTrim(obj[curr]);
      } else {
        acc[curr] = obj[curr];
      }
      return acc;
    }, Array.isArray(obj) ? [] : {}) as T;
  }
}
