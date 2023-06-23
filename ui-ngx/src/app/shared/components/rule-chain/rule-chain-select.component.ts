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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { map, share } from 'rxjs/operators';
import { PageData } from '@shared/models/page/page-data';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TooltipPosition } from '@angular/material/tooltip';
import { RuleChain, RuleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Direction } from '@shared/models/page/sort-order';

@Component({
  selector: 'tb-rule-chain-select',
  templateUrl: './rule-chain-select.component.html',
  styleUrls: ['./rule-chain-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RuleChainSelectComponent),
    multi: true
  }]
})
export class RuleChainSelectComponent implements ControlValueAccessor, OnInit {

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  ruleChainType: RuleChainType = RuleChainType.CORE;

  ruleChains$: Observable<Array<RuleChain>>;

  ruleChain: RuleChain;

  selected: any;

  private propagateChange = (v: any) => { };

  constructor(private ruleChainService: RuleChainService) {
  }

  ngOnInit() {
    const pageLink = new PageLink(100, 0, null, {
      property: 'name',
      direction: Direction.ASC
    });

    this.ruleChains$ = this.getRuleChains(pageLink).pipe(
      map((pageData) => pageData.data),
      share()
    );
  }

  public compareWith(object1: any, object2: any) {
    return object1 && object2 && object1.id.id === object2.id.id;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }


  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string | null): void {
    if (isDefinedAndNotNull(value)) {
      this.ruleChainService.getRuleChain(value)
        .subscribe(ruleChain => this.ruleChain = ruleChain);
    }
  }

  getname() {
    return this.ruleChain?.name;
  }

  ruleChainIdChanged() {
    this.updateView();
  }

  private updateView() {
    this.propagateChange(this.ruleChain.id.id);
  }

  private getRuleChains(pageLink: PageLink): Observable<PageData<RuleChain>> {
    return this.ruleChainService.getRuleChains(pageLink, this.ruleChainType, {ignoreLoading: true});
  }

}
