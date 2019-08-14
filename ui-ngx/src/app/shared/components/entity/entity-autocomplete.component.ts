///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Observable} from 'rxjs';
import {map, mergeMap, startWith, tap} from 'rxjs/operators';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {AliasEntityType, EntityType} from '@shared/models/entity-type.models';
import {BaseData} from '@shared/models/base-data';
import {EntityId} from '@shared/models/id/entity-id';
import {EntityService} from '@core/http/entity.service';

@Component({
  selector: 'tb-entity-autocomplete',
  templateUrl: './entity-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityAutocompleteComponent),
    multi: true
  }]
})
export class EntityAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  selectEntityFormGroup: FormGroup;

  modelValue: string | null;

  entityTypeValue: EntityType | AliasEntityType;

  entitySubtypeValue: string;

  @Input()
  set entityType(entityType: EntityType) {
    if (this.entityTypeValue !== entityType) {
      this.entityTypeValue = entityType;
      this.load();
    }
  }

  @Input()
  set entitySubtype(entitySubtype: string) {
    if (this.entitySubtypeValue !== entitySubtype) {
      this.entitySubtypeValue = entitySubtype;
      const currentEntity = this.getCurrentEntity();
      if (currentEntity) {
        if ((currentEntity as any).type !== this.entitySubtypeValue) {
          this.reset();
        }
      }
    }
  }

  @Input()
  excludeEntityIds: Array<string>;

  @Input()
  required: boolean;

  @Input()
  disabled: boolean;

  @ViewChild('entityInput', {static: true}) entityInput: ElementRef;

  entityText: string;
  noEntitiesMatchingText: string;
  entityRequiredText: string;

  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  private searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private fb: FormBuilder) {
    this.selectEntityFormGroup = this.fb.group({
      entity: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntities = this.selectEntityFormGroup.get('dashboard').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id.id;
          }
          this.updateView(modelValue);
        }),
        startWith<string | BaseData<EntityId>>(''),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchEntities(name) )
      );
  }

  ngAfterViewInit(): void {}

  load(): void {
    if (this.entityTypeValue) {
      switch (this.entityTypeValue) {
        case EntityType.ASSET:
          this.entityText = 'asset.asset';
          this.noEntitiesMatchingText = 'asset.no-assets-matching';
          this.entityRequiredText = 'asset.asset-required';
          break;
        case EntityType.DEVICE:
          this.entityText = 'device.device';
          this.noEntitiesMatchingText = 'device.no-devices-matching';
          this.entityRequiredText = 'device.device-required';
          break;
        case EntityType.ENTITY_VIEW:
          this.entityText = 'entity-view.entity-view';
          this.noEntitiesMatchingText = 'entity-view.no-entity-views-matching';
          this.entityRequiredText = 'entity-view.entity-view-required';
          break;
        case EntityType.RULE_CHAIN:
          this.entityText = 'rulechain.rulechain';
          this.noEntitiesMatchingText = 'rulechain.no-rulechains-matching';
          this.entityRequiredText = 'rulechain.rulechain-required';
          break;
        case EntityType.TENANT:
          this.entityText = 'tenant.tenant';
          this.noEntitiesMatchingText = 'tenant.no-tenants-matching';
          this.entityRequiredText = 'tenant.tenant-required';
          break;
        case EntityType.CUSTOMER:
          this.entityText = 'customer.customer';
          this.noEntitiesMatchingText = 'customer.no-customers-matching';
          this.entityRequiredText = 'customer.customer-required';
          break;
        case EntityType.USER:
          this.entityText = 'user.user';
          this.noEntitiesMatchingText = 'user.no-users-matching';
          this.entityRequiredText = 'user.user-required';
          break;
        case EntityType.DASHBOARD:
          this.entityText = 'dashboard.dashboard';
          this.noEntitiesMatchingText = 'dashboard.no-dashboards-matching';
          this.entityRequiredText = 'dashboard.dashboard-required';
          break;
        case EntityType.ALARM:
          this.entityText = 'alarm.alarm';
          this.noEntitiesMatchingText = 'alarm.no-alarms-matching';
          this.entityRequiredText = 'alarm.alarm-required';
          break;
        case AliasEntityType.CURRENT_CUSTOMER:
          this.entityText = 'customer.default-customer';
          this.noEntitiesMatchingText = 'customer.no-customers-matching';
          this.entityRequiredText = 'customer.default-customer-required';
          break;
      }
    }
    const currentEntity = this.getCurrentEntity();
    if (currentEntity) {
      const currentEntityType = currentEntity.id.entityType;
      if (this.entityTypeValue && currentEntityType !== this.entityTypeValue) {
        this.reset();
      }
    }
  }

  getCurrentEntity(): BaseData<EntityId> | null {
    const currentEntity = this.selectEntityFormGroup.get('entity').value;
    if (currentEntity && typeof currentEntity !== 'string') {
      return currentEntity as BaseData<EntityId>;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (value != null) {
      let targetEntityType = this.entityTypeValue;
      if (targetEntityType === AliasEntityType.CURRENT_CUSTOMER) {
        targetEntityType = EntityType.CUSTOMER;
      }
      this.entityService.getEntity(targetEntityType, value).subscribe(
        (entity) => {
          this.modelValue = entity.id.id;
          this.selectEntityFormGroup.get('entity').patchValue(entity, {emitEvent: true});
        }
      );
    } else {
      this.modelValue = null;
      this.selectEntityFormGroup.get('entity').patchValue(null, {emitEvent: true});
    }
  }

  reset() {
    this.selectEntityFormGroup.get('entity').patchValue(null, {emitEvent: true});
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityFn(entity?: BaseData<EntityId>): string | undefined {
    return entity ? entity.name : undefined;
  }

  fetchEntities(searchText?: string): Observable<Array<BaseData<EntityId>>> {
    this.searchText = searchText;
    let targetEntityType = this.entityTypeValue;
    if (targetEntityType === AliasEntityType.CURRENT_CUSTOMER) {
      targetEntityType = EntityType.CUSTOMER;
    }
    return this.entityService.getEntitiesByNameFilter(targetEntityType, searchText,
      50, this.entitySubtypeValue, false, true).pipe(
      map((data) => {
        if (data) {
          if (this.excludeEntityIds && this.excludeEntityIds.length) {
            const entities: Array<BaseData<EntityId>> = [];
            data.forEach((entity) => {
              if (this.excludeEntityIds.indexOf(entity.id.id) === -1) {
                entities.push(entity);
              }
            });
            return entities;
          } else {
            return data;
          }
        } else {
          return [];
        }
      }
    ));
  }

  clear() {
    this.selectEntityFormGroup.get('entity').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.entityInput.nativeElement.blur();
      this.entityInput.nativeElement.focus();
    }, 0);
  }

}
