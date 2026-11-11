// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators,
  ValidationErrors,
  NG_VALIDATORS,
  Validator
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { MessageType } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';
import { deepClone, isDefinedAndNotNull, isEqual, isObject, isString } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { OtaUpdateType } from '@shared/models/ota-package.models';

@Component({
    selector: 'tb-ota-update-event-config',
    templateUrl: './ota-update-event-config.component.html',
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OtaUpdateEventConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => OtaUpdateEventConfigComponent),
            multi: true
        }],
    standalone: false
})
export class OtaUpdateEventConfigComponent implements ControlValueAccessor, OnDestroy, OnInit, AfterViewInit, Validator {

  private destroy$ = new Subject<void>();

  modelValue: SchedulerEventConfiguration | null;
  updatePackageForm: UntypedFormGroup;
  currentGroupType: EntityType;
  packageType = OtaUpdateType.FIRMWARE;
  profileId: string;
  groupId: string;
  groupAll = false;

  @Input()
  schedulerEventType: string;

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    this.updatePackageForm = this.fb.group({
      originatorId: [null, Validators.required],
      packageId: [{value: null, disabled: true}, Validators.required]
    });

    this.updatePackageForm.get('originatorId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((entityId) => {
      if (isDefinedAndNotNull(entityId)) {
        this.updatePackageForm.get('packageId').enable({emitEvent: false});
      } else {
        this.updatePackageForm.get('packageId').disable({emitEvent: false});
        this.updatePackageForm.get('packageId').patchValue(null, {emitEvent: false});
      }
    });

    this.updatePackageForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    if (isDefinedAndNotNull(this.updatePackageForm) && this.schedulerEventType === 'updateSoftware') {
      this.packageType = OtaUpdateType.SOFTWARE;
    }
  }

  ngAfterViewInit(): void {
    if (!this.updatePackageForm.valid) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.updatePackageForm.disable({emitEvent: false});
    } else if (isDefinedAndNotNull(this.updatePackageForm.get('originatorId').value)){
      this.updatePackageForm.enable({emitEvent: false});
    } else {
      this.updatePackageForm.get('originatorId').enable({emitEvent: false});
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = value;
    if (value) {
      if (!this.modelValue.msgType) {
        this.modelValue.msgType =
          this.schedulerEventType === 'updateSoftware' ? MessageType.SOFTWARE_UPDATED : MessageType.FIRMWARE_UPDATED;
      }
      const formValue = this.prepareInputConfig(value);
      this.updatePackageForm.patchValue(formValue, {emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    if (!this.updatePackageForm.valid) {
      return {
        updatePackageForm: {
          valid: false
        }
      };
    }

    return null;
  }

  private prepareInputConfig(value: SchedulerEventConfiguration): SchedulerEventConfiguration | null {
    let formValue = deepClone(value);
    if (!isEqual(this.modelValue.msgBody, {})) {
      formValue = Object.assign(formValue, {packageId: this.modelValue.msgBody});
      this.updatePackageForm.get('packageId').enable({emitEvent: false});
    } else {
      this.updatePackageForm.get('packageId').disable({emitEvent: false});
    }
    delete formValue.msgBody;

    return formValue;
  }

  private updateModel() {
    if (this.updatePackageForm.valid) {
      const value = this.updatePackageForm.getRawValue();
      const msgValue = {
        originatorId: value.originatorId,
        msgBody: value.packageId !== null ? value.packageId : {}
      };
      this.modelValue = {...this.modelValue, ...msgValue};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  currentEntity(entity: EntityId | EntityGroupInfo | null) {
    if (isDefinedAndNotNull(entity)) {
      if (isString(entity.id) && 'entityType' in entity && entity.entityType === EntityType.DEVICE_PROFILE) {
        this.profileId = entity.id;
        this.groupId = null;
        this.groupAll = false;
      } else if (isObject(entity.id) && entity.id.hasOwnProperty('id')) {
        this.groupId = (entity.id as EntityId).id;
        this.groupAll = (entity as EntityGroupInfo).groupAll;
      }
    }
  }
}
