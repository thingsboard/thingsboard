///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-entity-autocomplete',
  templateUrl: './entity-autocomplete.component.html',
  styleUrls: ['./entity-autocomplete.component.scss'],
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
      this.reset();
      this.dirty = true;
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
          this.dirty = true;
        }
      }
      this.selectEntityFormGroup.get('entity').updateValueAndValidity();
    }
  }

  @Input()
  excludeEntityIds: Array<string>;

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

  @ViewChild('entityInput', {static: true}) entityInput: ElementRef;

  entityText: string;
  noEntitiesMatchingText: string;
  entityRequiredText: string;

  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  private dirty = false;

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
    this.filteredEntities = this.selectEntityFormGroup.get('entity').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        // startWith<string | BaseData<EntityId>>(''),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchEntities(name) ),
        share()
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
        case AliasEntityType.CURRENT_TENANT:
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
    if (this.disabled) {
      this.selectEntityFormGroup.disable({emitEvent: false});
    } else {
      this.selectEntityFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | EntityId | null): void {
    this.searchText = '';
    if (value != null) {
      if (typeof value === 'string') {
        const targetEntityType = this.checkEntityType(this.entityTypeValue);
        this.entityService.getEntity(targetEntityType, value, {ignoreLoading: true, ignoreErrors: true}).subscribe(
          (entity) => {
            this.modelValue = entity.id.id;
            this.selectEntityFormGroup.get('entity').patchValue(entity, {emitEvent: false});
          },
          () => {
            this.modelValue = null;
            this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: false});
            if (value !== null) {
              this.propagateChange(this.modelValue);
            }
          }
        );
      } else {
        const targetEntityType = this.checkEntityType(value.entityType);
        this.entityService.getEntity(targetEntityType, value.id, {ignoreLoading: true, ignoreErrors: true}).subscribe(
          (entity) => {
            this.modelValue = entity.id.id;
            this.selectEntityFormGroup.get('entity').patchValue(entity, {emitEvent: false});
          },
          () => {
            this.modelValue = null;
            this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: false});
            if (value !== null) {
              this.propagateChange(this.modelValue);
            }
          }
        );
      }
    } else {
      this.modelValue = null;
      this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectEntityFormGroup.get('entity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  reset() {
    this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: false});
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
    const targetEntityType = this.checkEntityType(this.entityTypeValue);
    return this.entityService.getEntitiesByNameFilter(targetEntityType, searchText,
      50, this.entitySubtypeValue, {ignoreLoading: true}).pipe(
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
    this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.entityInput.nativeElement.blur();
      this.entityInput.nativeElement.focus();
    }, 0);
  }

  checkEntityType(entityType: EntityType | AliasEntityType): EntityType {
    if (entityType === AliasEntityType.CURRENT_CUSTOMER) {
      return EntityType.CUSTOMER;
    } else if (entityType === AliasEntityType.CURRENT_TENANT) {
      return EntityType.TENANT;
    }
    return entityType;
  }
}
