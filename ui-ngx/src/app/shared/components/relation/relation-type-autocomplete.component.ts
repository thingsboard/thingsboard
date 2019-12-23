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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
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
import { RelationTypes } from '@app/shared/models/relation.models';

@Component({
  selector: 'tb-relation-type-autocomplete',
  templateUrl: './relation-type-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RelationTypeAutocompleteComponent),
    multi: true
  }]
})
export class RelationTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  relationTypeFormGroup: FormGroup;

  modelValue: string | null;

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

  @ViewChild('relationTypeInput', {static: true}) relationTypeInput: ElementRef;

  filteredRelationTypes: Observable<Array<string>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              public translate: TranslateService,
              private fb: FormBuilder) {
    this.relationTypeFormGroup = this.fb.group({
      relationType: [null, this.required ? [Validators.required] : []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    this.filteredRelationTypes = this.relationTypeFormGroup.get('relationType').valueChanges
      .pipe(
        tap(value => {
          this.updateView(value);
        }),
        // startWith<string | EntitySubtype>(''),
        map(value => value ? value : ''),
        mergeMap(type => this.fetchRelationTypes(type) )
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.relationTypeFormGroup.disable({emitEvent: false});
    } else {
      this.relationTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    this.relationTypeFormGroup.get('relationType').patchValue(value, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.relationTypeFormGroup.get('relationType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayRelationTypeFn(relationType?: string): string | undefined {
    return relationType ? relationType : undefined;
  }

  fetchRelationTypes(searchText?: string, strictMatch: boolean = false): Observable<Array<string>> {
    this.searchText = searchText;
    return of(RelationTypes).pipe(
      map(relationTypes => relationTypes.filter( relationType => {
        if (strictMatch) {
          return searchText ? relationType === searchText : false;
        } else {
          return searchText ? relationType.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        }
      }))
    );
  }

  clear() {
    this.relationTypeFormGroup.get('relationType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.relationTypeInput.nativeElement.blur();
      this.relationTypeInput.nativeElement.focus();
    }, 0);
  }

}
