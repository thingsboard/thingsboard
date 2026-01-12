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

import { Component, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { AliasFilterType, aliasFilterTypeTranslationMap, EntityAliasFilter } from '@shared/models/alias.models';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { EntitySearchDirection, entitySearchDirectionTranslations } from '@shared/models/relation.models';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-entity-filter',
  templateUrl: './entity-filter.component.html',
  styleUrls: ['./entity-filter.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityFilterComponent),
      multi: true
    }
  ]
})
export class EntityFilterComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input() disabled: boolean;

  @Input() allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input() resolveMultiple: boolean;

  @Output() resolveMultipleChanged: EventEmitter<boolean> = new EventEmitter<boolean>();

  entityFilterFormGroup: FormGroup;
  filterFormGroup: FormGroup;

  aliasFilterTypes: Array<AliasFilterType>;

  listEntityTypes: Array<EntityType | AliasEntityType>;

  aliasFilterType = AliasFilterType;
  aliasFilterTypeTranslations = aliasFilterTypeTranslationMap;
  entityType = EntityType;

  directionTypes = Object.keys(EntitySearchDirection);
  directionTypeTranslations = entitySearchDirectionTranslations;
  directionTypeEnum = EntitySearchDirection;

  private propagateChange = null;

  private destroy$ = new Subject<void>();
  private subscriptions = new Subscription();

  constructor(private entityService: EntityService,
              private fb: FormBuilder) {
  }

  ngOnInit(): void {

    this.aliasFilterTypes = this.entityService.getAliasFilterTypesByEntityTypes(this.allowedEntityTypes);

    this.listEntityTypes = this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes, false);
    if (!this.allowedEntityTypes?.length || this.allowedEntityTypes.includes(EntityType.QUEUE_STATS)) {
      this.listEntityTypes.push(EntityType.QUEUE_STATS);
    }

    this.entityFilterFormGroup = this.fb.group({
      type: [null, [Validators.required]]
    });
    this.entityFilterFormGroup.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type: AliasFilterType) => {
      this.filterTypeChanged(type);
    });
    this.entityFilterFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
    this.filterFormGroup = this.fb.group({});
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscriptions.unsubscribe();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(filter: EntityAliasFilter): void {
    if (!filter) {
      filter = {
        type: null,
        resolveMultiple: this.resolveMultiple
      };
    }
    this.entityFilterFormGroup.get('type').patchValue(filter.type, {emitEvent: false});
    if (filter && filter.type) {
      this.updateFilterFormGroup(filter.type, filter);
    }
  }

  private updateFilterFormGroup(type: AliasFilterType, filter?: EntityAliasFilter) {
    this.subscriptions.unsubscribe();
    this.subscriptions = new Subscription();
    switch (type) {
      case AliasFilterType.singleEntity:
        this.filterFormGroup = this.fb.group({
          singleEntity: [filter ? filter.singleEntity : null, [Validators.required]]
        });
        break;
      case AliasFilterType.entityList:
        this.filterFormGroup = this.fb.group({
          entityType: [filter ? filter.entityType : null, [Validators.required]],
          entityList: [{
            value: filter ? filter.entityList : [],
            disabled: !filter?.entityType
          }, [Validators.required]],
        });
        const entityTypeSubscription = this.filterFormGroup.get('entityType').valueChanges.subscribe((entityType) => {
          if (entityType && this.filterFormGroup.get('entityList').disabled) {
            this.filterFormGroup.get('entityList').enable({emitEvent: false});
          }
        });
        this.subscriptions.add(entityTypeSubscription);
        break;
      case AliasFilterType.entityName:
        this.filterFormGroup = this.fb.group({
          entityType: [filter ? filter.entityType : null, [Validators.required]],
          entityNameFilter: [filter ? filter.entityNameFilter : '', [Validators.required]],
        });
        break;
      case AliasFilterType.entityType:
        this.filterFormGroup = this.fb.group({
          entityType: [filter ? filter.entityType : null, [Validators.required]]
        });
        break;
      case AliasFilterType.stateEntity:
        this.filterFormGroup = this.fb.group({
          stateEntityParamName: [filter ? filter.stateEntityParamName : null, []],
          defaultStateEntity: [filter ? filter.defaultStateEntity : null, []],
        });
        break;
      case AliasFilterType.assetType:
        this.filterFormGroup = this.fb.group({
          assetTypes: [filter ? filter.assetTypes : null, [Validators.required]],
          assetNameFilter: [filter ? filter.assetNameFilter : '', []],
        });
        break;
      case AliasFilterType.deviceType:
        this.filterFormGroup = this.fb.group({
          deviceTypes: [filter ? filter.deviceTypes : null, [Validators.required]],
          deviceNameFilter: [filter ? filter.deviceNameFilter : '', []],
        });
        break;
      case AliasFilterType.edgeType:
        this.filterFormGroup = this.fb.group({
          edgeTypes: [filter ? filter.edgeTypes : null, [Validators.required]],
          edgeNameFilter: [filter ? filter.edgeNameFilter : '', []],
        });
        break;
      case AliasFilterType.entityViewType:
        this.filterFormGroup = this.fb.group({
          entityViewTypes: [filter ? filter.entityViewTypes : null, [Validators.required]],
          entityViewNameFilter: [filter ? filter.entityViewNameFilter : '', []],
        });
        break;
      case AliasFilterType.apiUsageState:
        this.filterFormGroup = this.fb.group({});
        break;
      case AliasFilterType.relationsQuery:
      case AliasFilterType.assetSearchQuery:
      case AliasFilterType.deviceSearchQuery:
      case AliasFilterType.edgeSearchQuery:
      case AliasFilterType.entityViewSearchQuery:
        this.filterFormGroup = this.fb.group({
          rootStateEntity: [filter ? filter.rootStateEntity : false, []],
          stateEntityParamName: [filter ? filter.stateEntityParamName : null, []],
          defaultStateEntity: [filter ? filter.defaultStateEntity : null, []],
          rootEntity: [filter ? filter.rootEntity : null, (filter && filter.rootStateEntity) ? [] : [Validators.required]],
          direction: [filter ? filter.direction : EntitySearchDirection.FROM, [Validators.required]],
          maxLevel: [filter ? filter.maxLevel : 1, []],
          fetchLastLevelOnly: [filter ? filter.fetchLastLevelOnly : false, []]
        });
        const rootStateSubscription = this.filterFormGroup.get('rootStateEntity').valueChanges.subscribe((rootStateEntity: boolean) => {
          this.filterFormGroup.get('rootEntity').setValidators(rootStateEntity ? [] : [Validators.required]);
          this.filterFormGroup.get('rootEntity').updateValueAndValidity();
        });
        this.subscriptions.add(rootStateSubscription);
        if (type === AliasFilterType.relationsQuery) {
          this.filterFormGroup.addControl('filters',
            this.fb.control(filter ? filter.filters : [], []));
        } else {
          this.filterFormGroup.addControl('relationType',
            this.fb.control(filter ? filter.relationType : null, []));
          if (type === AliasFilterType.assetSearchQuery) {
            this.filterFormGroup.addControl('assetTypes',
              this.fb.control(filter ? filter.assetTypes : [], [Validators.required]));
          } else if (type === AliasFilterType.deviceSearchQuery) {
            this.filterFormGroup.addControl('deviceTypes',
              this.fb.control(filter ? filter.deviceTypes : [], [Validators.required]));
          } else if (type === AliasFilterType.edgeSearchQuery) {
            this.filterFormGroup.addControl('edgeTypes',
              this.fb.control(filter ? filter.edgeTypes : [], [Validators.required]));
          } else if (type === AliasFilterType.entityViewSearchQuery) {
            this.filterFormGroup.addControl('entityViewTypes',
              this.fb.control(filter ? filter.entityViewTypes : [], [Validators.required]));
          }
        }
        break;
    }
    const filterFormSubscription = this.filterFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.subscriptions.add(filterFormSubscription);
  }

  private filterTypeChanged(type: AliasFilterType) {
    let resolveMultiple = true;
    if (type === AliasFilterType.singleEntity || type === AliasFilterType.stateEntity || type === AliasFilterType.apiUsageState) {
      resolveMultiple = false;
    }
    if (this.resolveMultiple !== resolveMultiple) {
      this.resolveMultipleChanged.emit(resolveMultiple);
    }
    this.updateFilterFormGroup(type);
  }

  private updateModel() {
    let filter = null;
    if (this.entityFilterFormGroup.valid && this.filterFormGroup.valid) {
      filter = {
        type: this.entityFilterFormGroup.get('type').value,
        resolveMultiple: this.resolveMultiple
      };
      filter = {...filter, ...this.filterFormGroup.value};
    }
    this.propagateChange(filter);
  }
}
