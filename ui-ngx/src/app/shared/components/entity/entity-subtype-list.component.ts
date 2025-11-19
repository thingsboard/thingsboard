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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, ReplaySubject, Subscription, throwError } from 'rxjs';
import { debounceTime, map, mergeMap, share } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntitySubtype, EntityType } from '@shared/models/entity-type.models';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipGrid, MatChipInputEvent } from '@angular/material/chips';
import { BroadcastService } from '@core/services/broadcast.service';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { AlarmService } from '@core/http/alarm.service';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceArray, coerceBoolean } from '@shared/decorators/coercion';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { UtilsService } from '@core/services/utils.service';
import { EntityService } from '@core/http/entity.service';
import { CalculatedFieldType } from "@shared/models/calculated-field.models";
import { CalculatedFieldsService } from "@core/http/calculated-fields.service";

@Component({
  selector: 'tb-entity-subtype-list',
  templateUrl: './entity-subtype-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntitySubTypeListComponent),
      multi: true
    }
  ]
})
export class EntitySubTypeListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  entitySubtypeListFormGroup: FormGroup;

  modelValue: Array<string> | null;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  @coerceBoolean()
  set required(value: boolean) {
    if (this.requiredValue !== value) {
      this.requiredValue = value;
      this.updateValidators();
    }
  }

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  label: string;

  @Input()
  disabled: boolean;

  @Input()
  entityType: EntityType;

  @Input()
  calculatedFieldType: CalculatedFieldType;

  @Input()
  emptyInputPlaceholder: string;

  @Input()
  filledInputPlaceholder: string;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceArray()
  additionalClasses: Array<string>;

  @ViewChild('entitySubtypeInput') entitySubtypeInput: ElementRef<HTMLInputElement>;
  @ViewChild('entitySubtypeAutocomplete') entitySubtypeAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  entitySubtypeList: Array<string> = [];
  filteredEntitySubtypeList: Observable<Array<string>>;
  private entitySubtypes: Observable<Array<string>>;

  private broadcastSubscription: Subscription;

  placeholder: string;
  secondaryPlaceholder: string;
  noSubtypesMathingText: string;
  subtypeListEmptyText: string;

  separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  private hasPageDataEntitySubTypes = new Set<EntityType>([
    EntityType.ALARM,
    EntityType.CALCULATED_FIELD
  ]);

  constructor(private broadcast: BroadcastService,
              public translate: TranslateService,
              private alarmService: AlarmService,
              private calculatedFieldsService: CalculatedFieldsService,
              private utils: UtilsService,
              private fb: FormBuilder,
              private entityService: EntityService) {
    this.entitySubtypeListFormGroup = this.fb.group({
      entitySubtypeList: [this.entitySubtypeList, this.required ? [Validators.required] : []],
      entitySubtype: [null]
    });
  }


  updateValidators() {
    this.entitySubtypeListFormGroup.get('entitySubtypeList').setValidators(this.required ? [Validators.required] : []);
    this.entitySubtypeListFormGroup.get('entitySubtypeList').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    switch (this.entityType) {
      case EntityType.ASSET:
        this.placeholder = this.required ? this.translate.instant('asset.enter-asset-type')
          : this.translate.instant('asset.any-asset');
        this.secondaryPlaceholder = '+' + this.translate.instant('asset.asset-type');
        this.noSubtypesMathingText = 'asset.no-asset-types-matching';
        this.subtypeListEmptyText = 'asset.asset-type-list-empty';
        this.broadcastSubscription = this.broadcast.on('assetSaved', () => {
          this.entitySubtypes = null;
        });
        break;
      case EntityType.DEVICE:
        this.placeholder = this.required ? this.translate.instant('device.enter-device-type')
          : this.translate.instant('device.any-device');
        this.secondaryPlaceholder = '+' + this.translate.instant('device.device-type');
        this.noSubtypesMathingText = 'device.no-device-types-matching';
        this.subtypeListEmptyText = 'device.device-profile-type-list-empty';
        this.broadcastSubscription = this.broadcast.on('deviceSaved', () => {
          this.entitySubtypes = null;
        });
        break;
      case EntityType.EDGE:
        this.placeholder = this.required ? this.translate.instant('edge.enter-edge-type')
          : this.translate.instant('edge.any-edge');
        this.secondaryPlaceholder = '+' + this.translate.instant('edge.edge-type');
        this.noSubtypesMathingText = 'edge.no-edge-types-matching';
        this.subtypeListEmptyText = 'edge.edge-type-list-empty';
        this.broadcastSubscription = this.broadcast.on('edgeSaved', () => {
          this.entitySubtypes = null;
        });
        break;
      case EntityType.ENTITY_VIEW:
        this.placeholder = this.required ? this.translate.instant('entity-view.enter-entity-view-type')
          : this.translate.instant('entity-view.any-entity-view');
        this.secondaryPlaceholder = '+' + this.translate.instant('entity-view.entity-view-type');
        this.noSubtypesMathingText = 'entity-view.no-entity-view-types-matching';
        this.subtypeListEmptyText = 'entity-view.entity-view-type-list-empty';
        this.broadcastSubscription = this.broadcast.on('entityViewSaved', () => {
          this.entitySubtypes = null;
        });
        break;
      case EntityType.ALARM:
        this.placeholder = this.required ? this.translate.instant('alarm.enter-alarm-type')
          : this.translate.instant('alarm.any-type');
        this.secondaryPlaceholder = '+' + this.translate.instant('alarm.alarm-type');
        this.noSubtypesMathingText = 'alarm.no-alarm-types-matching';
        this.subtypeListEmptyText = 'alarm.alarm-type-list-empty';
        break;
      case EntityType.CALCULATED_FIELD:
        this.placeholder = this.required ? this.translate.instant('alarm.enter-alarm-rule-type')
          : this.translate.instant('alarm-rule.any-type');
        this.secondaryPlaceholder = '+' + this.translate.instant('alarm-rule.alarm-rule');
        this.noSubtypesMathingText = 'alarm-rule.no-alarm-rule-types-matching';
        this.subtypeListEmptyText = 'alarm-rule.alarm-rule-type-list-empty';
        break;
    }

    if (this.emptyInputPlaceholder) {
      this.placeholder = this.emptyInputPlaceholder;
    }
    if (this.filledInputPlaceholder) {
      this.secondaryPlaceholder = this.filledInputPlaceholder;
    }

    this.filteredEntitySubtypeList = this.entitySubtypeListFormGroup.get('entitySubtype').valueChanges.pipe(
      debounceTime(150),
      map(value => value ? value : ''),
      mergeMap(name => this.fetchEntitySubtypes(name)),
      share()
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
    if (isDisabled) {
      this.entitySubtypeListFormGroup.disable({emitEvent: false});
    } else {
      this.entitySubtypeListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.entitySubtypeList = [...value];
      this.entitySubtypeListFormGroup.get('entitySubtypeList').setValue(this.entitySubtypeList);
    } else {
      this.entitySubtypeList = [];
      this.entitySubtypeListFormGroup.get('entitySubtypeList').setValue(this.entitySubtypeList);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  private add(entitySubtype: string): void {
    if (!this.modelValue || this.modelValue.indexOf(entitySubtype) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entitySubtype);
      this.entitySubtypeList.push(entitySubtype);
      this.entitySubtypeListFormGroup.get('entitySubtypeList').setValue(this.entitySubtypeList);
    }
    this.propagateChange(this.modelValue);
  }

  chipAdd(event: MatChipInputEvent): void {
    const value = (event.value || '').trim();
    if (value) {
      this.add(value);
      this.clear('');
    }
  }

  addOnBlur(event: FocusEvent) {
    if (!event.relatedTarget) {
      return;
    }
    const value = this.entitySubtypeInput.nativeElement.value;
    this.chipAdd({value} as MatChipInputEvent);
  }

  remove(entitySubtype: string) {
    const index = this.entitySubtypeList.indexOf(entitySubtype);
    if (index >= 0) {
      this.entitySubtypeList.splice(index, 1);
      this.entitySubtypeListFormGroup.get('entitySubtypeList').setValue(this.entitySubtypeList);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
    }
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.add(event.option.viewValue);
    this.clear('');
  }

  displayEntitySubtypeFn(entitySubtype?: string): string | undefined {
    return entitySubtype ? entitySubtype : undefined;
  }

  private fetchEntitySubtypes(searchText?: string): Observable<Array<string>> {
    this.searchText = searchText;
    return this.getEntitySubtypes(searchText).pipe(
      map(subTypes => {
        let result;
        if (this.hasPageDataEntitySubTypes.has(this.entityType)) {
          result = subTypes;
        } else {
          result = subTypes.filter(subType => searchText ? subType.toUpperCase().startsWith(searchText.toUpperCase()) : true);
        }
        if (!result.length && searchText.length) {
          result = [searchText];
        }
        return result;
      })
    );
  }

  private getEntitySubtypes(searchText?: string): Observable<Array<string>> {
    if (this.hasPageDataEntitySubTypes.has(this.entityType)) {
      const pageLink = new PageLink(25, 0, searchText);
      let subTypesPagesObservable: Observable<PageData<EntitySubtype>>;
      let subTypesCfPagesObservable: Observable<PageData<string>>;
      switch (this.entityType) {
        case EntityType.ALARM:
          subTypesPagesObservable = this.alarmService.getAlarmTypes(pageLink, {ignoreLoading: true});
          break;
        case EntityType.CALCULATED_FIELD:
          subTypesCfPagesObservable = this.calculatedFieldsService.getAlarmRuleNames(pageLink, CalculatedFieldType.ALARM, {ignoreLoading: true});
      }
      if (subTypesPagesObservable) {
        this.entitySubtypes = subTypesPagesObservable.pipe(
            map(subTypesPage => subTypesPage.data.map(subType => subType.type)),
        );
      } else if (subTypesCfPagesObservable) {
        this.entitySubtypes = subTypesCfPagesObservable.pipe(
          map(subTypesPage => subTypesPage.data)
        );
      } else {
        return throwError(null);
      }
    }
    if (!this.entitySubtypes) {
      const subTypesObservable = this.entityService.getEntitySubtypesObservable(this.entityType);
      if (subTypesObservable) {
        this.entitySubtypes = subTypesObservable.pipe(
          share({
            connector: () => new ReplaySubject(1),
            resetOnError: false,
            resetOnComplete: false,
            resetOnRefCountZero: true,
          }),
        );
      } else {
        return throwError(null);
      }
    }
    return this.entitySubtypes;
  }

  onFocus() {
    if (this.dirty) {
      this.entitySubtypeListFormGroup.get('entitySubtype').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear(value: string = '') {
    this.entitySubtypeInput.nativeElement.value = value;
    this.entitySubtypeListFormGroup.get('entitySubtype').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entitySubtypeInput.nativeElement.blur();
      this.entitySubtypeInput.nativeElement.focus();
    }, 0);
  }

  customTranslate(entity: string) {
    return this.utils.customTranslation(entity, entity);
  }

}
