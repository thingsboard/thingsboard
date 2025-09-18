///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { AfterViewInit, ChangeDetectorRef, Component, Input, OnInit, output, ViewChild } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { charsWithNumRegex, oneSpaceInsideRegex } from '@shared/models/regex.constants';
import {
  ArgumentEntityType,
  ArgumentEntityTypeParamsMap,
  ArgumentEntityTypeTranslations,
  CalculatedFieldGeofencing,
  CalculatedFieldGeofencingValue,
  CalculatedFieldType,
  GeofencingDirectionTranslations,
  GeofencingReportStrategy,
  GeofencingReportStrategyTranslations,
  getCalculatedFieldCurrentEntityFilter
} from '@shared/models/calculated-field.models';
import { debounceTime, delay, distinctUntilChanged, filter, map } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { BehaviorSubject, merge, Observable, of } from 'rxjs';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { EntityAutocompleteComponent } from '@shared/components/entity/entity-autocomplete.component';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntitySearchDirection } from '@shared/models/relation.models';

@Component({
  selector: 'tb-calculated-field-geofencing-zone-groups-panel',
  templateUrl: './calculated-field-geofencing-zone-groups-panel.component.html',
  styleUrls: ['./calculated-field-geofencing-zone-groups-panel.component.scss']
})
export class CalculatedFieldGeofencingZoneGroupsPanelComponent implements OnInit, AfterViewInit {

  @Input() buttonTitle: string;
  @Input() zone: CalculatedFieldGeofencing;
  @Input() entityId: EntityId;
  @Input() tenantId: string;
  @Input() entityName: string;
  @Input() calculatedFieldType: CalculatedFieldType;
  @Input() usedNames: string[];

  @ViewChild('entityAutocomplete') entityAutocomplete: EntityAutocompleteComponent;

  geofencingDataApplied = output<CalculatedFieldGeofencingValue>();

  readonly maxRelationLevelPerCfArgument = getCurrentAuthState(this.store).maxRelationLevelPerCfArgument;

  geofencingFormGroup = this.fb.group({
    name: ['', [Validators.required, this.uniqNameRequired(), this.forbiddenNameValidator(), Validators.pattern(charsWithNumRegex), Validators.maxLength(255)]],
    refEntityId: this.fb.group({
      entityType: [ArgumentEntityType.Current],
      id: ['']
    }),
    refDynamicSourceConfiguration: this.fb.group({
      direction: [EntitySearchDirection.TO],
      relationType: ['', [Validators.required]],
      maxLevel: [1, [Validators.required, Validators.min(1), Validators.max(this.maxRelationLevelPerCfArgument)]],
      fetchLastLevelOnly: [false],
    }),
    perimeterKeyName: ['', [Validators.pattern(oneSpaceInsideRegex)]],
    reportStrategy: [GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS],
    createRelationsWithMatchedZones: [false],
    direction: [EntitySearchDirection.TO],
    relationType: ['', [Validators.required]]
  });

  entityFilter: EntityFilter;
  entityNameSubject = new BehaviorSubject<string>(null);

  readonly ArgumentEntityType = ArgumentEntityType;
  readonly argumentEntityTypes = Object.values(ArgumentEntityType) as ArgumentEntityType[];
  readonly ArgumentEntityTypeTranslations = ArgumentEntityTypeTranslations;
  readonly DataKeyType = DataKeyType;
  readonly ArgumentEntityTypeParamsMap = ArgumentEntityTypeParamsMap;
  readonly GeofencingReportStrategyList = Object.values(GeofencingReportStrategy) as Array<GeofencingReportStrategy>;
  readonly GeofencingReportStrategyTranslations = GeofencingReportStrategyTranslations;
  readonly GeofencingDirectionList = Object.values(EntitySearchDirection) as Array<EntitySearchDirection>;
  readonly GeofencingDirectionTranslations = GeofencingDirectionTranslations;

  private currentEntityFilter: EntityFilter;

  constructor(
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private popover: TbPopoverComponent<CalculatedFieldGeofencingZoneGroupsPanelComponent>,
    private store: Store<AppState>
  ) {

    this.observeMaxLevelChanges();
    this.observeEntityFilterChanges();
    this.observeEntityTypeChanges();
    this.observeUpdatePosition();
    this.observeCreateRelationZonesChanges();
  }

  get entityType(): ArgumentEntityType {
    return this.geofencingFormGroup.get('refEntityId').get('entityType').value;
  }

  get refEntityIdFormGroup(): FormGroup {
    return this.geofencingFormGroup.get('refEntityId') as FormGroup;
  }

  get refDynamicSourceFormGroup(): FormGroup {
    return this.geofencingFormGroup.get('refDynamicSourceConfiguration') as FormGroup;
  }

  ngOnInit(): void {
    this.geofencingFormGroup.patchValue(this.zone, {emitEvent: false});
    if (this.zone.refDynamicSourceConfiguration?.type) {
      this.refEntityIdFormGroup.get('entityType').setValue(this.zone.refDynamicSourceConfiguration.type, {emitEvent: false});
    }
    this.validateFetchLastLevelOnly(this.zone?.refDynamicSourceConfiguration?.maxLevel);
    this.validateDirectionAndRelationType(this.zone?.createRelationsWithMatchedZones);
    this.validateRefDynamicSourceConfiguration(this.zone?.refEntityId?.entityType || this.zone?.refDynamicSourceConfiguration?.type);

    this.currentEntityFilter = getCalculatedFieldCurrentEntityFilter(this.entityName, this.entityId);
    this.updateEntityFilter(this.zone.refEntityId?.entityType);
  }

  fetchOptions(searchText: string): Observable<Array<string>> {
    const search = searchText ? searchText?.toLowerCase() : '';
    return of(['Contains', 'Manages']).pipe(map(name => name?.filter(option => option.toLowerCase().includes(search))));
  }

  private observeMaxLevelChanges(): void {
    this.refDynamicSourceFormGroup.get('maxLevel').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.validateFetchLastLevelOnly(value));
  }

  private observeCreateRelationZonesChanges(): void {
    this.geofencingFormGroup.get('createRelationsWithMatchedZones').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.validateDirectionAndRelationType(value));
  }

  private validateFetchLastLevelOnly(maxLevel = 1): void {
    if (maxLevel > 1) {
      this.refDynamicSourceFormGroup.get('fetchLastLevelOnly').enable({emitEvent: false});
    } else {
      this.refDynamicSourceFormGroup.get('fetchLastLevelOnly').disable({emitEvent: false});
    }
  }

  private validateDirectionAndRelationType(createRelation = false): void {
    if (createRelation) {
      this.geofencingFormGroup.get('direction').enable({emitEvent: false});
      this.geofencingFormGroup.get('relationType').enable({emitEvent: false});
    } else {
      this.geofencingFormGroup.get('direction').disable({emitEvent: false});
      this.geofencingFormGroup.get('relationType').disable({emitEvent: false});
    }
  }

  private validateRefDynamicSourceConfiguration(type: ArgumentEntityType = ArgumentEntityType.Current): void {
    if (type === ArgumentEntityType.RelationQuery) {
      this.refDynamicSourceFormGroup.enable({emitEvent: false});
    } else {
      this.refDynamicSourceFormGroup.disable({emitEvent: false});
    }
  }

  ngAfterViewInit(): void {
    if (this.zone.refEntityId?.id === NULL_UUID) {
      this.entityAutocomplete.selectEntityFormGroup.get('entity').markAsTouched();
    }
  }

  saveZone(): void {
    const value = this.geofencingFormGroup.value as CalculatedFieldGeofencingValue;
    const argumentType = value.refEntityId.entityType;
    switch (argumentType) {
      case ArgumentEntityType.Current:
        delete value.refEntityId;
        break;
      case ArgumentEntityType.RelationQuery:
        delete value.refEntityId;
        value.refDynamicSourceConfiguration.type = ArgumentEntityType.RelationQuery;
        break;
      case ArgumentEntityType.Tenant:
        value.refEntityId.id = this.tenantId;
        break
      default:
        value.entityName = this.entityNameSubject.value;
    }
    this.geofencingDataApplied.emit(value);
  }

  cancel(): void {
    this.popover.hide();
  }

  private updateEntityFilter(entityType: ArgumentEntityType = ArgumentEntityType.Current): void {
    let entityFilter: EntityFilter;
    switch (entityType) {
      case ArgumentEntityType.Current:
      case ArgumentEntityType.RelationQuery:
        entityFilter = this.currentEntityFilter;
        break;
      case ArgumentEntityType.Tenant:
        entityFilter = {
          type: AliasFilterType.singleEntity,
          singleEntity: {
            id: this.tenantId,
            entityType: EntityType.TENANT
          },
        };
        break;
      default:
        entityFilter = {
          type: AliasFilterType.singleEntity,
          singleEntity: this.geofencingFormGroup.get('refEntityId').value as unknown as EntityId,
        };
    }
    this.entityFilter = entityFilter;
    this.cd.markForCheck();
  }

  private observeEntityFilterChanges(): void {
    merge(
      this.refEntityIdFormGroup.get('entityType').valueChanges,
      this.refEntityIdFormGroup.get('id').valueChanges.pipe(filter(Boolean)),
    )
      .pipe(debounceTime(50), takeUntilDestroyed())
      .subscribe(() => this.updateEntityFilter(this.entityType));
  }

  private observeEntityTypeChanges(): void {
    this.refEntityIdFormGroup.get('entityType').valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(type => {
        this.geofencingFormGroup.get('refEntityId').get('id').setValue('');
        const isEntityWithId = type !== ArgumentEntityType.Tenant && type !== ArgumentEntityType.Current && type !== ArgumentEntityType.RelationQuery;
        this.geofencingFormGroup.get('refEntityId')
          .get('id')[isEntityWithId ? 'enable' : 'disable']();
        if (!isEntityWithId) {
          this.entityNameSubject.next(null);
        }
        this.validateRefDynamicSourceConfiguration(type);
      });
  }

  private uniqNameRequired(): ValidatorFn {
    return (control: FormControl) => {
      const newName = control.value.trim().toLowerCase();
      const isDuplicate = this.usedNames?.some(name => name.toLowerCase() === newName);

      return isDuplicate ? { duplicateName: true } : null;
    };
  }

  private forbiddenNameValidator(): ValidatorFn {
    return (control: FormControl) => {
      const trimmedValue = control.value.trim().toLowerCase();
      const forbiddenNames = ['ctx', 'e', 'pi'];
      return forbiddenNames.includes(trimmedValue) ? { forbiddenName: true } : null;
    };
  }

  private observeUpdatePosition(): void {
    merge(
      this.refEntityIdFormGroup.get('entityType').valueChanges,
      this.geofencingFormGroup.get('createRelationsWithMatchedZones').valueChanges
    )
      .pipe(delay(50), takeUntilDestroyed())
      .subscribe(() => this.popover.updatePosition());
  }

}
