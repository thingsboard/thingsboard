///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, ElementRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { Observable, of } from 'rxjs';
import { IAliasController } from '@core/api/widget-api.models';
import { catchError, map, mergeMap, publishReplay, refCount, tap } from 'rxjs/operators';
import { DataKey } from '@shared/models/widget.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-device-key-autocomplete',
  templateUrl: './device-key-autocomplete.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceKeyAutocompleteComponent),
      multi: true
    }
  ]
})
export class DeviceKeyAutocompleteComponent extends PageComponent implements OnInit, ControlValueAccessor, OnChanges {

  @ViewChild('keyInput') keyInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDeviceAliasId: string;

  @Input()
  keyType: DataKeyType;

  @Input()
  attributeLabel = 'widgets.rpc.attribute-value-key';

  @Input()
  timeseriesLabel = 'widgets.rpc.timeseries-value-key';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  dataKeyType = DataKeyType;

  private modelValue: string;

  private propagateChange = null;

  public deviceKeyFormGroup: FormGroup;

  filteredKeys: Observable<Array<string>>;
  keySearchText = '';

  private latestKeySearchResult: Array<string> = null;
  private keysFetchObservable$: Observable<Array<string>> = null;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private entityService: EntityService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.deviceKeyFormGroup = this.fb.group({
      key: [null, this.required ? [Validators.required] : []]
    });
    this.deviceKeyFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });

    this.filteredKeys = this.deviceKeyFormGroup.get('key').valueChanges
      .pipe(
        map(value => value ? value : ''),
        mergeMap(name => this.fetchKeys(name) )
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'targetDeviceAliasId' || propName === 'keyType') {
          this.clearKeysCache();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.deviceKeyFormGroup.disable({emitEvent: false});
    } else {
      this.deviceKeyFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    this.deviceKeyFormGroup.patchValue(
      {key: value}, {emitEvent: false}
    );
  }

  clearKey() {
    this.deviceKeyFormGroup.get('key').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.keyInput.nativeElement.blur();
      this.keyInput.nativeElement.focus();
    }, 0);
  }

  onFocus() {
    this.deviceKeyFormGroup.get('key').updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  private updateModel() {
    const value: string = this.deviceKeyFormGroup.get('key').value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private clearKeysCache(): void {
    this.latestKeySearchResult = null;
    this.keysFetchObservable$ = null;
  }

  private fetchKeys(searchText?: string): Observable<Array<string>> {
    if (this.keySearchText !== searchText || this.latestKeySearchResult === null) {
      this.keySearchText = searchText;
      const dataKeyFilter = this.createKeyFilter(this.keySearchText);
      return this.getKeys().pipe(
        map(name => name.filter(dataKeyFilter)),
        tap(res => this.latestKeySearchResult = res)
      );
    }
    return of(this.latestKeySearchResult);
  }

  private getKeys() {
    if (this.keysFetchObservable$ === null) {
      let fetchObservable: Observable<Array<DataKey>>;
      if (this.targetDeviceAliasId) {
        const dataKeyTypes = [this.keyType];
        fetchObservable = this.fetchEntityKeys(this.targetDeviceAliasId, dataKeyTypes);
      } else {
        fetchObservable = of([]);
      }
      this.keysFetchObservable$ = fetchObservable.pipe(
        map((dataKeys) => dataKeys.map((dataKey) => dataKey.name)),
        publishReplay(1),
        refCount()
      );
    }
    return this.keysFetchObservable$;
  }

  private fetchEntityKeys(entityAliasId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    return this.aliasController.getAliasInfo(entityAliasId).pipe(
      mergeMap((aliasInfo) => {
        return this.entityService.getEntityKeysByEntityFilter(
          aliasInfo.entityFilter,
          dataKeyTypes,
          {ignoreLoading: true, ignoreErrors: true}
        ).pipe(
          catchError(() => of([]))
        );
      }),
      catchError(() => of([] as Array<DataKey>))
    );
  }

  private createKeyFilter(query: string): (key: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.toLowerCase().startsWith(lowercaseQuery);
  }
}
