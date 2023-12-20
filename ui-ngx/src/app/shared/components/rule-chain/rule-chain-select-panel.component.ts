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

import {
  AfterViewInit,
  Component,
  ElementRef,
  Inject,
  InjectionToken,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, takeUntil, startWith, share } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { OverlayRef } from '@angular/cdk/overlay';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { RuleChain, RuleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { TranslateService } from '@ngx-translate/core';

export const RULE_CHAIN_SELECT_PANEL_DATA = new InjectionToken<any>('RuleChainSelectPanelData');

export interface RuleChainSelectPanelData {
  ruleChainId: string | null;
  ruleChainType: RuleChainType;
}

@Component({
  selector: 'tb-rule-chain-select-panel',
  templateUrl: './rule-chain-select-panel.component.html',
  styleUrls: ['./rule-chain-select-panel.component.scss']
})
export class RuleChainSelectPanelComponent implements AfterViewInit, OnDestroy {

  ruleChainId: string;
  ruleChainType: RuleChainType;

  selectRuleChainGroup: FormGroup;

  @ViewChild('ruleChainInput', {static: true}) userInput: ElementRef;

  filteredRuleChains: Observable<Array<RuleChain>>;

  searchText = '';

  ruleChainSelected = false;

  result?: RuleChain;

  private dirty = false;
  private destroy$ = new Subject<void>();

  constructor(@Inject(RULE_CHAIN_SELECT_PANEL_DATA) public data: RuleChainSelectPanelData,
              public overlayRef: OverlayRef,
              public translate: TranslateService,
              private fb: FormBuilder,
              private ruleChainService: RuleChainService) {
    this.ruleChainId = data.ruleChainId;
    this.ruleChainType = data.ruleChainType;
    this.selectRuleChainGroup = this.fb.group({
      ruleChainInput: ['', {nonNullable: true}]
    });
    this.filteredRuleChains = this.selectRuleChainGroup.get('ruleChainInput').valueChanges
      .pipe(
        debounceTime(150),
        startWith(''),
        distinctUntilChanged((a: string, b: string) => a.trim() === b.trim()),
        switchMap(name => this.fetchRuleChains(name)),
        share(),
        takeUntil(this.destroy$)
      );
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.userInput.nativeElement.focus();
    }, 0);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.clear();
    this.ruleChainSelected = true;
    if (event.option.value?.id) {
      this.result = event.option.value;
    }
    this.overlayRef.dispose();
  }

  fetchRuleChains(searchText?: string): Observable<Array<RuleChain>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getRuleChains(pageLink)
      .pipe(
        catchError(() => of(emptyPageData<RuleChain>())),
        map(pageData => pageData.data)
      );
  }

  onFocus(): void {
    if (!this.dirty) {
      this.selectRuleChainGroup.get('ruleChainInput').updateValueAndValidity({onlySelf: true});
      this.dirty = true;
    }
  }

  clear() {
    this.selectRuleChainGroup.get('ruleChainInput').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.userInput.nativeElement.blur();
      this.userInput.nativeElement.focus();
    }, 0);
  }

  private getRuleChains(pageLink: PageLink): Observable<PageData<RuleChain>> {
    return this.ruleChainService.getRuleChains(pageLink, this.ruleChainType, {ignoreLoading: true});
  }

}
