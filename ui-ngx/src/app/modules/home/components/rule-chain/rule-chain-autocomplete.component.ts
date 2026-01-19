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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { RuleChainService } from '@core/http/rule-chain.service';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { RuleChainType } from '@app/shared/models/rule-chain.models';
import { getEntityDetailsPageURL } from '@core/utils';

@Component({
  selector: 'tb-rule-chain-autocomplete',
  templateUrl: './rule-chain-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RuleChainAutocompleteComponent),
    multi: true
  }]
})
export class RuleChainAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectRuleChainFormGroup: UntypedFormGroup;

  modelValue: string | null;

  @Input()
  labelText: string = 'rulechain.rulechain';

  @Input()
  requiredText: string;

  @Input()
  ruleChainType: RuleChainType = RuleChainType.CORE;

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

  @ViewChild('ruleChainInput', {static: true}) ruleChainInput: ElementRef;
  @ViewChild('ruleChainInput', {read: MatAutocompleteTrigger}) ruleChainAutocomplete: MatAutocompleteTrigger;

  filteredRuleChains: Observable<Array<BaseData<EntityId>>>;

  searchText = '';
  ruleChainURL: string;

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private ruleChainService: RuleChainService,
              private fb: UntypedFormBuilder) {
    this.selectRuleChainFormGroup = this.fb.group({
      ruleChainId: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredRuleChains = this.selectRuleChainFormGroup.get('ruleChainId').valueChanges
      .pipe(
        debounceTime(150),
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
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchRuleChain(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {}

  getCurrentEntity(): BaseData<EntityId> | null {
    const currentRuleChain = this.selectRuleChainFormGroup.get('ruleChainId').value;
    if (currentRuleChain && typeof currentRuleChain !== 'string') {
      return currentRuleChain as BaseData<EntityId>;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectRuleChainFormGroup.disable({emitEvent: false});
    } else {
      this.selectRuleChainFormGroup.enable({emitEvent: false});
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (value != null) {
      const targetEntityType = EntityType.RULE_CHAIN;
      this.entityService.getEntity(targetEntityType, value, {ignoreLoading: true, ignoreErrors: true}).subscribe(
        (entity) => {
          this.modelValue = entity.id.id;
          this.ruleChainURL = getEntityDetailsPageURL(this.modelValue,EntityType.RULE_CHAIN);
          if (this.ruleChainType === RuleChainType.EDGE) {
            this.ruleChainURL = '/edgeManagement' + this.ruleChainURL;
          }
          this.selectRuleChainFormGroup.get('ruleChainId').patchValue(entity, {emitEvent: false});
        },
        () => {
          this.modelValue = null;
          this.selectRuleChainFormGroup.get('ruleChainId').patchValue('', {emitEvent: false});
          if (value !== null) {
            this.propagateChange(this.modelValue);
          }
        }
      );
    } else {
      this.modelValue = null;
      this.selectRuleChainFormGroup.get('ruleChainId').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectRuleChainFormGroup.get('ruleChainId').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  reset() {
    this.selectRuleChainFormGroup.get('ruleChainId').patchValue('', {emitEvent: false});
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayRuleChainFn(ruleChain?: BaseData<EntityId>): string | undefined {
    return ruleChain ? ruleChain.name : undefined;
  }

  fetchRuleChain(searchText?: string): Observable<Array<BaseData<EntityId>>> {
    this.searchText = searchText;
    return this.entityService.getEntitiesByNameFilter(EntityType.RULE_CHAIN, searchText,
      50, this.ruleChainType, {ignoreLoading: true}).pipe(
        catchError(() => of([]))
    );
  }

  clear() {
    this.selectRuleChainFormGroup.get('ruleChainId').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.ruleChainInput.nativeElement.blur();
      this.ruleChainInput.nativeElement.focus();
    }, 0);
  }

  createDefaultRuleChain($event: Event, ruleChainName: string) {
    $event.preventDefault();
    this.ruleChainAutocomplete.closePanel();
    this.ruleChainService.createDefaultRuleChain(ruleChainName.trim()).subscribe((ruleChain) => {
      this.selectRuleChainFormGroup.get('ruleChainId').patchValue(ruleChain);
    });
  }
}
