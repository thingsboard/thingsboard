///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { merge, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { isEqual } from '@core/utils';

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
      this.refresh$.next([]);
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
          this.refresh$.next([]);
          this.dirty = true;
        }
      }
      this.selectEntityFormGroup.get('entity').updateValueAndValidity();
    }
  }

  @Input()
  excludeEntityIds: Array<string>;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

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

  @Output()
  entityChanged = new EventEmitter<BaseData<EntityId>>();

  @ViewChild('entityInput', {static: true}) entityInput: ElementRef;

  entityText: string;
  noEntitiesMatchingText: string;
  entityRequiredText: string;

  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  private dirty = false;

  private refresh$ = new Subject<Array<BaseData<EntityId>>>();

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
    this.filteredEntities = merge(
      this.refresh$.asObservable(),
      this.selectEntityFormGroup.get('entity').valueChanges
        .pipe(
          debounceTime(150),
          tap(value => {
            let modelValue;
            if (typeof value === 'string' || !value) {
              modelValue = null;
            } else {
              modelValue = value.id.id;
            }
            this.updateView(modelValue, value);
            if (value === null) {
              this.clear();
            }
          }),
          // startWith<string | BaseData<EntityId>>(''),
          map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
          switchMap(name => this.fetchEntities(name)),
          share()
        )
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
        case EntityType.EDGE:
          this.entityText = 'edge.edge';
          this.noEntitiesMatchingText = 'edge.no-edges-matching';
          this.entityRequiredText = 'edge.edge-required';
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
        case AliasEntityType.CURRENT_USER:
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
        case AliasEntityType.CURRENT_USER_OWNER:
          const authUser =  getCurrentAuthUser(this.store);
          if (authUser.authority === Authority.TENANT_ADMIN) {
            this.entityText = 'tenant.tenant';
            this.noEntitiesMatchingText = 'tenant.no-tenants-matching';
            this.entityRequiredText = 'tenant.tenant-required';
          } else {
            this.entityText = 'customer.customer';
            this.noEntitiesMatchingText = 'customer.no-customers-matching';
            this.entityRequiredText = 'customer.customer-required';
          }
          break;
      }
    }
    if (this.labelText && this.labelText.length) {
      this.entityText = this.labelText;
    }
    if (this.requiredText && this.requiredText.length) {
      this.entityRequiredText = this.requiredText;
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

  async writeValue(value: string | EntityId | null): Promise<void> {
    this.searchText = '';
    if (value !== null && (typeof value === 'string' ||  (value.entityType && value.id))) {
      let targetEntityType: EntityType;
      let id: string;
      if (typeof value === 'string') {
        targetEntityType = this.checkEntityType(this.entityTypeValue);
        id = value;
      } else {
        targetEntityType = this.checkEntityType(value.entityType);
        id = value.id;
      }
      let entity: BaseData<EntityId> = null;
      try {
        entity = await this.entityService.getEntity(targetEntityType, id, {ignoreLoading: true, ignoreErrors: true}).toPromise();
      } catch (e) {
        this.propagateChange(null);
      }
      this.modelValue = entity !== null ? entity.id.id : null;
      this.selectEntityFormGroup.get('entity').patchValue(entity !== null ? entity : '', {emitEvent: false});
      this.entityChanged.emit(entity);
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

  updateView(value: string | null, entity: BaseData<EntityId> | null) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
      this.entityChanged.emit(entity);
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
      catchError(() => of(null)),
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
    } else if (entityType === AliasEntityType.CURRENT_USER) {
      return EntityType.USER;
    } else if (entityType === AliasEntityType.CURRENT_USER_OWNER) {
      const authUser =  getCurrentAuthUser(this.store);
      if (authUser.authority === Authority.TENANT_ADMIN) {
        return EntityType.TENANT;
      } else {
        return EntityType.CUSTOMER;
      }
    }
    return entityType;
  }
}
