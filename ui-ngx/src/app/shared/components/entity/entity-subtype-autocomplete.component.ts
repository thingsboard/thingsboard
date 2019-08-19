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

import {AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild, OnDestroy} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Observable, of, throwError, Subscription} from 'rxjs';
import {PageLink} from '@shared/models/page/page-link';
import {Direction} from '@shared/models/page/sort-order';
import {filter, map, mergeMap, publishReplay, refCount, startWith, tap, publish} from 'rxjs/operators';
import {PageData, emptyPageData} from '@shared/models/page/page-data';
import {DashboardInfo} from '@app/shared/models/dashboard.models';
import {DashboardId} from '@app/shared/models/id/dashboard-id';
import {DashboardService} from '@core/http/dashboard.service';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {getCurrentAuthUser} from '@app/core/auth/auth.selectors';
import {Authority} from '@shared/models/authority.enum';
import {TranslateService} from '@ngx-translate/core';
import {DeviceService} from '@core/http/device.service';
import {EntitySubtype, EntityType} from '@app/shared/models/entity-type.models';
import {BroadcastService} from '@app/core/services/broadcast.service';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {AssetService} from '@core/http/asset.service';
import {EntityViewService} from '@core/http/entity-view.service';

@Component({
  selector: 'tb-entity-subtype-autocomplete',
  templateUrl: './entity-subtype-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntitySubTypeAutocompleteComponent),
    multi: true
  }]
})
export class EntitySubTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  subTypeFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  entityType: EntityType;

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

  @ViewChild('subTypeInput', {static: true}) subTypeInput: ElementRef;

  selectEntitySubtypeText: string;
  entitySubtypeText: string;
  entitySubtypeRequiredText: string;

  filteredSubTypes: Observable<Array<EntitySubtype>>;

  subTypes: Observable<Array<EntitySubtype>>;

  private broadcastSubscription: Subscription;

  private searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              public translate: TranslateService,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private entityViewService: EntityViewService,
              private fb: FormBuilder) {
    this.subTypeFormGroup = this.fb.group({
      subType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    switch (this.entityType) {
      case EntityType.ASSET:
        this.selectEntitySubtypeText = 'asset.select-asset-type';
        this.entitySubtypeText = 'asset.asset-type';
        this.entitySubtypeRequiredText = 'asset.asset-type-required';
        this.broadcastSubscription = this.broadcast.on('assetSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.DEVICE:
        this.selectEntitySubtypeText = 'device.select-device-type';
        this.entitySubtypeText = 'device.device-type';
        this.entitySubtypeRequiredText = 'device.device-type-required';
        this.broadcastSubscription = this.broadcast.on('deviceSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.ENTITY_VIEW:
        this.selectEntitySubtypeText = 'entity-view.select-entity-view-type';
        this.entitySubtypeText = 'entity-view.entity-view-type';
        this.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
        this.broadcastSubscription = this.broadcast.on('entityViewSaved', () => {
          this.subTypes = null;
        });
        break;
    }

    this.filteredSubTypes = this.subTypeFormGroup.get('subType').valueChanges
      .pipe(
        tap(value => {
            let modelValue;
            if (!value) {
              modelValue = null;
            } else if (typeof value === 'string') {
              modelValue = value;
            } else {
              modelValue = value.type;
            }
            this.updateView(modelValue);
        }),
        startWith<string | EntitySubtype>(''),
        map(value => value ? (typeof value === 'string' ? value : value.type) : ''),
        mergeMap(type => this.fetchSubTypes(type) )
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    if (this.broadcastSubscription) {
      this.broadcastSubscription.unsubscribe();
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.subTypeFormGroup.disable();
    } else {
      this.subTypeFormGroup.enable();
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (value != null) {
      this.modelValue = value;
      this.fetchSubTypes(value, true).subscribe(
        (subTypes) => {
          const subType = subTypes && subTypes.length === 1 ? subTypes[0] : null;
          this.subTypeFormGroup.get('subType').patchValue(subType, {emitEvent: true});
        }
      );
    } else {
      this.modelValue = null;
      this.subTypeFormGroup.get('subType').patchValue(null, {emitEvent: true});
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displaySubTypeFn(subType?: EntitySubtype): string | undefined {
    return subType ? subType.type : undefined;
  }

  fetchSubTypes(searchText?: string, strictMatch: boolean = false): Observable<Array<EntitySubtype>> {
    this.searchText = searchText;
    return this.getSubTypes().pipe(
      map(subTypes => subTypes.filter( subType => {
        if (strictMatch) {
          return searchText ? subType.type === searchText : false;
        } else {
          return searchText ? subType.type.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        }
      }))
    );
  }

  getSubTypes(): Observable<Array<EntitySubtype>> {
    if (!this.subTypes) {
      switch (this.entityType) {
        case EntityType.ASSET:
          this.subTypes = this.assetService.getAssetTypes(false, true);
          break;
        case EntityType.DEVICE:
          this.subTypes = this.deviceService.getDeviceTypes(false,  true);
          break;
        case EntityType.ENTITY_VIEW:
          this.subTypes = this.entityViewService.getEntityViewTypes(false, true);
          break;
      }
      if (this.subTypes) {
        this.subTypes = this.subTypes.pipe(
          publishReplay(1),
          refCount()
        );
      } else {
        return throwError(null);
      }
    }
    return this.subTypes;
  }

  clear() {
    this.subTypeFormGroup.get('subType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.subTypeInput.nativeElement.blur();
      this.subTypeInput.nativeElement.focus();
    }, 0);
  }

}
