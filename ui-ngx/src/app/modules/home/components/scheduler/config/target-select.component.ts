// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, forwardRef, Input, OnDestroy, Output } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { DeviceProfileInfo } from '@shared/models/device.models';
import { DeviceId } from '@shared/models/id/device-id';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatFormFieldAppearance } from '@angular/material/form-field';

type TargetType = 'entity' | 'deviceProfile' | 'groupTenant' | 'ownerGroup';

interface FormValue {
  target: TargetType;
  deviceTargetId: DeviceId;
  deviceProfileTargetId: DeviceProfileId;
  groupOriginatorId: EntityGroupId;
  groupOwnerId: EntityGroupId;
}

@Component({
    selector: 'tb-target-select',
    templateUrl: './target-select.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TargetSelectComponent),
            multi: true
        }],
    standalone: false
})
export class TargetSelectComponent implements ControlValueAccessor, OnDestroy {

  targetFormGroup: UntypedFormGroup;
  entityType = EntityType.DEVICE;
  currentUser = getCurrentAuthUser(this.store);

  headerOptions = [
    {
      name: this.translate.instant('device.device'),
      value: 'entity'
    },
    {
      name: this.translate.instant('device-profile.device-profile'),
      value: 'deviceProfile'
    },
    {
      name: this.translate.instant('entity-group.device-group'),
      value: 'groupTenant'
    },
    {
      name: this.translate.instant('scheduler.devices-group-owner'),
      value: 'ownerGroup'
    }
  ];

  private modelValue: EntityId | null;
  private destroy$ = new Subject<void>();
  private loadData = false;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill'

  @Output()
  currentEntity = new EventEmitter<EntityId|EntityGroupInfo>();

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {
    this.targetFormGroup = this.fb.group({
      target: ['entity'],
      deviceTargetId: [null],
      deviceProfileTargetId: [null],
      groupOriginatorId: [null],
      groupOwnerId: [null]
    });
    this.targetFormGroup.get('target').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      (target: TargetType) => {
        if (target === 'groupTenant' || target === 'ownerGroup') {
          this.targetFormGroup.patchValue({
            groupOriginatorId: null,
            groupOwnerId: null
          });
        } else if (target === 'entity') {
          this.targetFormGroup.get('deviceTargetId').patchValue(null);
        } else if (target === 'deviceProfile') {
          this.targetFormGroup.get('deviceProfileTargetId').patchValue(null);
        }
      }
    );
    this.targetFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.targetFormGroup.disable({emitEvent: false});
    } else {
      this.targetFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityId | null): void {
    this.modelValue = value;
    let target = 'entity';
    if (this.modelValue && this.modelValue.entityType === EntityType.ENTITY_GROUP) {
      target = 'groupTenant';
    }
    if (this.modelValue && this.modelValue.entityType === EntityType.DEVICE_PROFILE) {
      target = 'deviceProfile';
    }
    this.targetFormGroup.patchValue({
      target,
      deviceTargetId: target === 'entity' ? value : null,
      deviceProfileTargetId: target === 'deviceProfile' ? value : null,
      groupOriginatorId: target === 'groupTenant' ? value : null
    }, {emitEvent: false});
    this.loadData = true;
  }

  entityGroupLoaded(entityGroup: EntityGroupInfo) {
    if (this.loadData && isDefinedAndNotNull(entityGroup)) {
      this.loadData = false;
      if (this.currentUser.authority === Authority.TENANT_ADMIN && entityGroup?.ownerId?.id !== this.currentUser.tenantId) {
        this.targetFormGroup.patchValue({
          target: 'ownerGroup',
          groupOwnerId: entityGroup.ownerId,
          groupOriginatorId: entityGroup.id
        }, {emitEvent: false});
      } else {
        this.targetFormGroup.patchValue({
          target: 'groupTenant',
          groupOriginatorId: entityGroup.id
        }, {emitEvent: false});
      }
    }
    if (isDefinedAndNotNull(entityGroup)) {
      this.currentEntity.emit(entityGroup);
    }
  }

  deviceProfileLoaded(deviceProfileInfo: DeviceProfileInfo) {
    if (isDefinedAndNotNull(deviceProfileInfo)) {
      this.currentEntity.emit(deviceProfileInfo.id);
    }
  }

  deviceLoaded(device: any) {
    if (isDefinedAndNotNull(device)) {
      this.currentEntity.emit(device.deviceProfileId);
    }
  }

  updateView(value: FormValue | null) {
    let entityId = null;
    switch (value?.target) {
      case 'entity':
        entityId = value.deviceTargetId;
        break;
      case 'deviceProfile':
        entityId = value.deviceProfileTargetId;
        break;
      case 'groupTenant':
      case 'ownerGroup':
        entityId = value.groupOriginatorId;
        break;
    }
    if (!isEqual(this.modelValue, entityId)) {
      this.modelValue = entityId;
      if (this.modelValue && this.modelValue.id) {
        this.propagateChange(this.modelValue);
      } else {
        this.propagateChange(null);
      }
    }
  }
}
