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

import {
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { Observable, of } from 'rxjs';
import { IAliasController } from '@core/api/widget-api.models';
import { catchError, map, mergeMap, publishReplay, refCount, tap } from 'rxjs/operators';
import { DataKey, TargetDevice, TargetDeviceType, targetDeviceValid } from '@shared/models/widget.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityFilter, singleEntityFilterFromDeviceId } from '@shared/models/query/query.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
  targetDevice: TargetDevice;

  @Input()
  keyType: DataKeyType;

  @Input()
  attributeScope: AttributeScope;

  @Input()
  attributeLabel = 'widgets.rpc.attribute-value-key';

  @Input()
  timeseriesLabel = 'widgets.rpc.timeseries-value-key';

  @Input()
  requiredText: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  dataKeyType = DataKeyType;

  private modelValue: string;

  private propagateChange = null;

  public deviceKeyFormGroup: UntypedFormGroup;

  filteredKeys: Observable<Array<string>>;
  keySearchText = '';

  private latestKeySearchResult: Array<string> = null;
  private keysFetchObservable$: Observable<Array<string>> = null;

  constructor(protected store: Store<AppState>,
              private entityService: EntityService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.deviceKeyFormGroup = this.fb.group({
      key: [null, this.required ? [Validators.required] : []]
    });
    this.deviceKeyFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
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
        if (propName === 'targetDevice' || propName === 'keyType' || propName === 'attributeScope') {
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
      if (targetDeviceValid(this.targetDevice)) {
        const dataKeyTypes = [this.keyType];
        fetchObservable = this.fetchEntityKeys(this.targetDevice, dataKeyTypes);
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

  private fetchEntityKeys(targetDevice: TargetDevice, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    let entityFilter$: Observable<EntityFilter>;
    if (targetDevice.type === TargetDeviceType.device) {
      entityFilter$ = of(singleEntityFilterFromDeviceId(targetDevice.deviceId));
    } else {
      entityFilter$ = this.aliasController.getAliasInfo(targetDevice.entityAliasId).pipe(
        map(aliasInfo => aliasInfo.entityFilter)
      );
    }
    return entityFilter$.pipe(
      mergeMap((entityFilter) =>
        this.entityService.getEntityKeysByEntityFilterAndScope(
          entityFilter,
          dataKeyTypes, [EntityType.DEVICE],
          this.attributeScope,
          {ignoreLoading: true, ignoreErrors: true}
        ).pipe(
          catchError(() => of([]))
        )),
      catchError(() => of([] as Array<DataKey>))
    );
  }

  private createKeyFilter(query: string): (key: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.toLowerCase().startsWith(lowercaseQuery);
  }
}
