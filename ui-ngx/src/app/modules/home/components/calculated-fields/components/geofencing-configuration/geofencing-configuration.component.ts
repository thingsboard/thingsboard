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

import { booleanAttribute, Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  ArgumentEntityType,
  CalculatedFieldGeofencing,
  CalculatedFieldGeofencingConfiguration,
  CalculatedFieldOutput,
  CalculatedFieldType,
  defaultCalculatedFieldOutput,
  getCalculatedFieldCurrentEntityFilter,
  notEmptyObjectValidator
} from '@shared/models/calculated-field.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { EntityId } from '@shared/models/id/entity-id';

@Component({
  selector: 'tb-geofencing-configuration',
  templateUrl: './geofencing-configuration.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GeofencingConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => GeofencingConfigurationComponent),
      multi: true
    }
  ],
})
export class GeofencingConfigurationComponent implements ControlValueAccessor, Validator, OnChanges {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input({required: true})
  ownerId: EntityId;

  @Input({transform: booleanAttribute}) isEditValue = true;

  readonly minAllowedScheduledUpdateIntervalInSecForCF = getCurrentAuthState(this.store).minAllowedScheduledUpdateIntervalInSecForCF;
  readonly DataKeyType = DataKeyType;

  geofencingConfiguration = this.fb.group({
    entityCoordinates: this.fb.group({
      latitudeKeyName: [null, [Validators.required]],
      longitudeKeyName: [null, [Validators.required]],
    }),
    zoneGroups: this.fb.control<Record<string, CalculatedFieldGeofencing>>({}, notEmptyObjectValidator()),
    scheduledUpdateEnabled: [true],
    scheduledUpdateInterval: [this.minAllowedScheduledUpdateIntervalInSecForCF],
    output: this.fb.control<CalculatedFieldOutput>(defaultCalculatedFieldOutput)
  });

  currentEntityFilter: EntityFilter;
  isRelatedEntity: boolean;

  private propagateChange: (config: CalculatedFieldGeofencingConfiguration) => void = () => { };

  constructor(private fb: FormBuilder,
              private store: Store<AppState>) {

    this.geofencingConfiguration.get('zoneGroups').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((zoneGroups: Record<string, CalculatedFieldGeofencing>) =>
        this.checkRelatedEntity(zoneGroups)
      );

    this.geofencingConfiguration.get('scheduledUpdateEnabled').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value: boolean) =>
        this.checkScheduledUpdateEnabled(value)
      );

    this.geofencingConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updatedModel(this.geofencingConfiguration.getRawValue() as any);
    })
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.entityName || changes.entityId) {
      const entityNameChanges = changes.entityName;
      const entityIdChanges = changes.entityId;
      if ((entityNameChanges?.currentValue !== entityNameChanges?.previousValue) || (entityIdChanges?.currentValue !== entityIdChanges?.previousValue)) {
        this.currentEntityFilter = getCalculatedFieldCurrentEntityFilter(this.entityName, this.entityId);
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.geofencingConfiguration.valid || this.geofencingConfiguration.disabled ? null : { geofencingConfigError: false };
  }

  writeValue(config: CalculatedFieldGeofencingConfiguration): void {
    this.geofencingConfiguration.patchValue(config, {emitEvent: false});
    this.checkRelatedEntity(this.geofencingConfiguration.get('zoneGroups').value);
    if (this.geofencingConfiguration.enabled) {
      this.checkScheduledUpdateEnabled(this.geofencingConfiguration.get('scheduledUpdateEnabled').value);
    }
  }

  registerOnChange(fn: (config: CalculatedFieldGeofencingConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.geofencingConfiguration.disable({emitEvent: false});
    } else {
      this.geofencingConfiguration.enable({emitEvent: false});
      this.checkScheduledUpdateEnabled(this.geofencingConfiguration.get('scheduledUpdateEnabled').value);
    }
  }

  private updatedModel(value: CalculatedFieldGeofencingConfiguration) {
    value.type = CalculatedFieldType.GEOFENCING;
    this.propagateChange(value)
  }

  private checkScheduledUpdateEnabled(value: boolean) {
    if (value) {
      this.geofencingConfiguration.get('scheduledUpdateInterval').enable({emitEvent: false});
    } else {
      this.geofencingConfiguration.get('scheduledUpdateInterval').disable({emitEvent: false});
    }
  }

  private checkRelatedEntity(zoneGroups: Record<string, CalculatedFieldGeofencing>) {
    this.isRelatedEntity = Object.values(zoneGroups).some(zone => zone.refDynamicSourceConfiguration?.type === ArgumentEntityType.RelationQuery);
  }
}
