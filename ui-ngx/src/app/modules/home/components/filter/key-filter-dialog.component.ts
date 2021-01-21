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

import { Component, ElementRef, Inject, OnDestroy, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  EntityKeyType,
  entityKeyTypeTranslationMap,
  EntityKeyValueType,
  entityKeyValueTypesMap,
  KeyFilterInfo,
  KeyFilterPredicate
} from '@shared/models/query/query.models';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { entityFields } from '@shared/models/entity.models';
import { Observable, of, Subject } from 'rxjs';
import { filter, map, mergeMap, publishReplay, refCount, startWith, takeUntil } from 'rxjs/operators';
import { isDefined } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceProfileService } from '@core/http/device-profile.service';

export interface KeyFilterDialogData {
  keyFilter: KeyFilterInfo;
  isAdd: boolean;
  displayUserParameters: boolean;
  allowUserDynamicSource: boolean;
  readonly: boolean;
  telemetryKeysOnly: boolean;
  entityId?: EntityId;
}

@Component({
  selector: 'tb-key-filter-dialog',
  templateUrl: './key-filter-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: KeyFilterDialogComponent}],
  styleUrls: ['./key-filter-dialog.component.scss']
})
export class KeyFilterDialogComponent extends
  DialogComponent<KeyFilterDialogComponent, KeyFilterInfo>
  implements OnInit, OnDestroy, ErrorStateMatcher {

  @ViewChild('keyNameInput', {static: true}) private keyNameInput: ElementRef;

  private dirty = false;
  private entityKeysName: Observable<Array<string>>;
  private destroy$ = new Subject();

  keyFilterFormGroup: FormGroup;

  entityKeyTypes =
    this.data.telemetryKeysOnly ?
      [EntityKeyType.ATTRIBUTE, EntityKeyType.TIME_SERIES] :
      [EntityKeyType.ENTITY_FIELD, EntityKeyType.ATTRIBUTE, EntityKeyType.TIME_SERIES];

  entityKeyTypeTranslations = entityKeyTypeTranslationMap;

  entityKeyValueTypesKeys = Object.keys(EntityKeyValueType);

  entityKeyValueTypeEnum = EntityKeyValueType;

  entityKeyValueTypes = entityKeyValueTypesMap;

  submitted = false;

  showAutocomplete = false;

  filteredKeysName: Observable<Array<string>>;

  searchText = '';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: KeyFilterDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<KeyFilterDialogComponent, KeyFilterInfo>,
              private deviceProfileService: DeviceProfileService,
              private dialogs: DialogService,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.keyFilterFormGroup = this.fb.group(
      {
        key: this.fb.group(
          {
            type: [this.data.keyFilter.key.type, [Validators.required]],
            key: [this.data.keyFilter.key.key, [Validators.required]]
          }
        ),
        valueType: [this.data.keyFilter.valueType, [Validators.required]],
        predicates: [this.data.keyFilter.predicates, [Validators.required]]
      }
    );

    if (!this.data.readonly) {
      this.keyFilterFormGroup.get('valueType').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((valueType: EntityKeyValueType) => {
        const prevValue: EntityKeyValueType = this.keyFilterFormGroup.value.valueType;
        const predicates: KeyFilterPredicate[] = this.keyFilterFormGroup.get('predicates').value;
        if (prevValue && prevValue !== valueType && predicates && predicates.length) {
          this.dialogs.confirm(this.translate.instant('filter.key-value-type-change-title'),
            this.translate.instant('filter.key-value-type-change-message')).subscribe(
            (result) => {
              if (result) {
                this.keyFilterFormGroup.get('predicates').setValue([]);
              } else {
                this.keyFilterFormGroup.get('valueType').setValue(prevValue, {emitEvent: false});
              }
            }
          );
        }
      });

      this.keyFilterFormGroup.get('key.type').valueChanges.pipe(
        startWith(this.data.keyFilter.key.type),
        takeUntil(this.destroy$)
      ).subscribe((type: EntityKeyType) => {
        if (type === EntityKeyType.ENTITY_FIELD || isDefined(this.data.entityId)) {
          this.entityKeysName = null;
          this.dirty = false;
          this.showAutocomplete = true;
        } else {
          this.showAutocomplete = false;
        }
      });

      this.keyFilterFormGroup.get('key.key').valueChanges.pipe(
        filter((keyName) =>
          this.keyFilterFormGroup.get('key.type').value === EntityKeyType.ENTITY_FIELD && entityFields.hasOwnProperty(keyName)),
        takeUntil(this.destroy$)
      ).subscribe((keyName: string) => {
        const prevValueType: EntityKeyValueType = this.keyFilterFormGroup.value.valueType;
        const newValueType = entityFields[keyName]?.time ? EntityKeyValueType.DATE_TIME : EntityKeyValueType.STRING;
        if (prevValueType !== newValueType) {
          this.keyFilterFormGroup.get('valueType').patchValue(newValueType, {emitEvent: false});
        }
      });
    } else {
      this.keyFilterFormGroup.disable({emitEvent: false});
    }
  }

  ngOnInit() {
    this.filteredKeysName = this.keyFilterFormGroup.get('key.key').valueChanges
      .pipe(
        map(value => value ? value : ''),
        mergeMap(name => this.fetchEntityName(name))
      );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  clear() {
    this.keyFilterFormGroup.get('key.key').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.keyNameInput.nativeElement.blur();
      this.keyNameInput.nativeElement.focus();
    }, 0);
  }

  onFocus() {
    if (!this.dirty && this.showAutocomplete) {
      this.keyFilterFormGroup.get('key.key').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = true;
    }
  }

  save(): void {
    this.submitted = true;
    if (this.keyFilterFormGroup.valid) {
      const keyFilter: KeyFilterInfo = this.keyFilterFormGroup.getRawValue();
      this.dialogRef.close(keyFilter);
    }
  }

  private fetchEntityName(searchText?: string): Observable<Array<string>> {
    this.searchText = searchText;
    return this.getEntityKeys().pipe(
      map(keys => searchText ? keys.filter(key => key.toUpperCase().startsWith(searchText.toUpperCase())) : keys)
    );
  }

  private getEntityKeys(): Observable<Array<string>> {
    if (!this.entityKeysName) {
      let keyNameObservable: Observable<Array<string>>;
      switch (this.keyFilterFormGroup.get('key.type').value) {
        case EntityKeyType.ENTITY_FIELD:
          keyNameObservable = of(Object.keys(entityFields).map(itm => entityFields[itm]).map(entityField => entityField.keyName).sort());
          break;
        case EntityKeyType.ATTRIBUTE:
          keyNameObservable = this.deviceProfileService.getDeviceProfileDevicesAttributesKeys(
            this.data.entityId?.id,
            {ignoreLoading: true}
          );
          break;
        case EntityKeyType.TIME_SERIES:
          keyNameObservable = this.deviceProfileService.getDeviceProfileDevicesTimeseriesKeys(
            this.data.entityId?.id,
            {ignoreLoading: true}
          );
          break;
        default:
          keyNameObservable = of([]);
      }
      this.entityKeysName = keyNameObservable.pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.entityKeysName;
  }
}
